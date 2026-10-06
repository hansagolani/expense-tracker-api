# Expense Tracker API

A Spring Boot REST API for tracking personal expenses, with JWT-based
authentication, role-based access control (USER/ADMIN), and an event-driven
piece using RabbitMQ. Second portfolio project, built to demonstrate breadth
beyond Project 2 (Library Management API) — specifically security and
messaging, which that project didn't cover.

## Status
In progress. The core implementation is complete and tested.
Remaining work is final documentation/design-decision writeups.

Auth, CRUD (categories/expenses), and RabbitMQ eventing are all done and tested 
- 20+ tests across two suites, all passing. 
- This project is a local portfolio demo, not a live deployment, there's no hosted instance.

## Stack
- Java 17, Spring Boot 3.3 (Web, Data JPA, Validation, Security, AMQP)
- JWT via JJWT
- RabbitMQ
- H2 (local only - this project doesn't run anywhere else)
- Maven

## Domain
- `User` - username, email, hashed password, roles (USER/ADMIN)
- `Category` - owned by a user, name unique per owner
- `Expense` - belongs to a category, owned by a user

A regular user only sees/touches their own data. Admin can see everything
and delete anything (moderation), but creating an expense still requires
owning the category - no exceptions, even for admin. Keeps `expense.owner`
and `expense.category.owner` always in sync instead of needing a special
case to explain.

## Auth
- `POST /api/auth/register` / `POST /api/auth/login` - both public and return a JWT
- Everything else needs `Authorization: Bearer <token>`
- Stateless - no server-side sessions
- No endpoint to grant ADMIN - a `CommandLineRunner` (`DataInitializer`)
  seeds one admin account on startup instead. Username/email/password are
  read from `application.properties` (`app.admin.*`) rather than hardcoded -
  even though this project isn't deployed anywhere, externalizing config
  instead of hardcoding it is the habit worth keeping, same reasoning as
  the JWT secret below.
- A custom `AuthenticationEntryPoint` returns a plain 401 for unauthenticated
  requests instead of Spring Security's default redirect-to-login behavior,
  which doesn't make sense for a stateless API with no login page.

## Authorization: two layers, two status codes
- **Ownership violations** (trying to access/delete someone else's category
  or expense) return 404, not 403 - a 403 would confirm the resource exists
  at all, just not to you; a 404 doesn't leak that.
- **Role violations** (a non-admin hitting `/api/admin/**`) return 403,
  handled by Spring Security's `hasRole("ADMIN")` at the filter level - the
  request never reaches application code in this case, unlike the ownership
  checks above.

## Endpoints
- `GET/POST /api/categories`, `GET/DELETE /api/categories/{id}`
- `GET/POST /api/expenses` (`?categoryId=`), `GET/DELETE /api/expenses/{id}`
- `GET /api/admin/users` - ADMIN only, returns username/email/roles, never
  the password hash

## Error handling
Centralized pattern — one `GlobalExceptionHandler` mapping:
- `ResourceNotFoundException` → 404
- `DuplicateResourceException` → 409 (e.g. username/email/category name already taken)
- `InvalidOperationException` → 400
- `AccessDeniedException` → 403 (role violations)
- `DataIntegrityViolationException` → 409 (DB constraint backstop)
- `MethodArgumentNotValidException` → 400, field-level messages
- `BadCredentialsException` → 401, generic message (doesn't confirm whether
  the username exists)

## Messaging
Creating an expense (`ExpenseService.createExpense`) publishes an
`ExpenseCreatedEvent` through Spring's `ApplicationEventPublisher`. A
`@TransactionalEventListener(phase = AFTER_COMMIT)` only forwards it to
RabbitMQ once the database transaction has actually committed - if the
transaction rolls back for any reason, no message is ever sent, so there's
no phantom event for an expense that doesn't exist.

A consumer (`ExpenseCreatedEventHandler`) maintains a running per-user
total in a `user_totals` table. Two things matter here that aren't obvious
from a basic producer/consumer tutorial:

- **Idempotency.** RabbitMQ's delivery guarantee is at-least-once, not
  exactly-once - a message can be redelivered (e.g. an ack lost after
  processing). A `processed_events` table, keyed on the event's own
  `expenseId`, is checked first; if the id's already there, the handler
  returns immediately and nothing else runs. No event can be double-counted,
  however many times it's redelivered.
- **Race-safe updates.** The running total is updated with a single atomic
  SQL `UPDATE ... SET total = total + :amount`, not a read-then-write in
  Java, which avoids a classic lost-update race between concurrent events
  for the same user. The first-ever expense for a user (no row to update
  yet) falls back to an insert; a unique constraint on `userId` means that
  if two first-time inserts for the same new user ever raced each other,
  one succeeds and one fails cleanly on the constraint - and because the
  whole method is one transaction, a failure there rolls back entirely
  (including the `processed_events` insert), so RabbitMQ's redelivery
  naturally retries the whole thing from a consistent state. No
  application-level retry logic needed on top of what the queue already
  guarantees.
- Consumer failures retry up to 3 times (`spring.rabbitmq.listener.simple.retry.max-attempts`)
  before being dropped - there's no dead-letter queue. Fine for a demo
  project; a production version would route failed messages somewhere
  inspectable instead of discarding them.

The automated test suite (`ExpenseMessagingIntegrationTest`) calls the
handler directly rather than publishing through a real broker round-trip -
deliberate, since it keeps the tests deterministic without needing to poll
for async delivery. The actual producer→broker→consumer wiring was verified
manually, via Postman and RabbitMQ's management UI.

## Testing
- `ExpenseTrackerIntegrationTest` - auth, validation, ownership boundaries,
  admin behavior, category/expense CRUD (18 tests)
- `ExpenseMessagingIntegrationTest` - event handler idempotency, atomic
  total updates, per-user isolation (4 tests)

## Design decisions
See Auth, Authorization, and Messaging sections above for the real
decisions made on this project (404-vs-403, admin/category rule,
AFTER_COMMIT publishing, idempotent/atomic consumption) - covered inline
rather than repeated here.

## Bug log
- **Caught before commit: retry logic inside a poisoned transaction.**
  An early version of the event handler wrapped the fallback insert in a
  `try/catch(DataIntegrityViolationException)` and retried the atomic
  update inside the same `catch` block, still inside the same
  `@Transactional` method. Once a `DataIntegrityViolationException` occurs,
  Spring marks the transaction rollback-only - catching the exception in
  application code doesn't undo that, so the retry inside the `catch`
  either failed outright or silently got discarded at commit time
  (`UnexpectedRollbackException`). Fixed by removing the catch entirely:
  on a genuine constraint violation, the whole transaction (including the
  `processed_events` insert) rolls back cleanly, and RabbitMQ's redelivery
- already configured with bounded retries - reprocesses the message from
  a consistent state instead of retrying inside broken transaction state.