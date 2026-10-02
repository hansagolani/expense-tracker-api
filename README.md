# Expense Tracker API

A Spring Boot REST API for tracking personal expenses, with JWT-based
authentication, role-based access control (USER/ADMIN), and an event-driven
piece using RabbitMQ. Second portfolio project, built to demonstrate breadth
beyond Project 2 (Library Management API) — specifically security and
messaging, which that project didn't cover.

## Status
In progress. Sections will be filled in with actual implementation details, 
and design-decision writeups as each part is built and tested.
This project is a local portfolio demo, not a live deployment - there's no hosted instance.

Completed:
- USER/ADMIN role-based authorization
- JWT authentication
- Domain model and JPA relationships
- Category and expense CRUD
- Per-user ownership rules
- Registration and login
- Validation and centralized exception handling
- Integration tests for authentication, authorization, ownership, and CRUD rules

Remaining:
- RabbitMQ producer/consumer
- Deployment to Render
- Final documentation and design decisions

## Stack
- Java 17 
- Spring Boot 3.3 (Web, Data JPA, Validation, Security, AMQP)
- Spring Security + JWT via JJWT
- RabbitMQ
- H2 locally, Postgres on deploy
- Maven
- Render (free tier)

## Domain
- `User` - username, email, hashed password, roles (USER/ADMIN)
- `Category` - owned by a user, name unique per owner
- `Expense` - belongs to a category, owned by a user

Ownership matters throughout: a regular `USER` only ever sees/modifies their
own categories and expenses. An `ADMIN` can see everything, for oversight
purposes. But creating an expense still requires owning the category - 
no exceptions, even for admin. Keeps `expense.owner` and `expense.category.owner` 
always in sync.

## Auth
- `POST /api/auth/register` / `POST /api/auth/login` - both public, return a JWT
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

## Testing
18 MockMvc integration tests (`ExpenseTrackerIntegrationTest`), covering
register/login, validation failures, ownership boundaries on categories and
expenses, admin's view-all/delete-all/can't-bypass-category-ownership
behavior, the admin-only endpoint, and category-with-expenses delete
protection.

## Messaging
Planned: publish an event on expense creation, consume it to keep a running
total. Details once I get there.

## Design decisions
See Auth and Authorization sections above for the two real decisions so
far (404-vs-403, admin/category rule) - covered inline rather than repeated
here.

## Bug log
Nothing to report yet - the test suite was built alongside the
implementation, so most of what would've been caught manually before it ever ran. 
