# Expense Tracker API

A Spring Boot REST API for tracking personal expenses, with JWT-based
authentication, role-based access control (USER/ADMIN), and an event-driven
piece using RabbitMQ. Second portfolio project, built to demonstrate breadth
beyond Project 2 (Library Management API) — specifically security and
messaging, which that project didn't cover.

## Status
In progress. Entities, repositories, and the security plumbing are in place.

## Stack
- Java 17, Spring Boot 3.3 (Web, Data JPA, Validation, Security, AMQP)
- JWT via JJWT
- RabbitMQ
- H2 locally, Postgres on deploy
- Render (free tier)

## Domain
- `User` - username, email, hashed password, roles (USER/ADMIN)
- `Category` - owned by a user
- `Expense` - belongs to a category, owned by a user

A regular user only sees/touches their own data. Admin can see everything
and delete anything (moderation), but creating an expense still requires
owning the category - no exceptions, even for admin. Keeps `expense.owner`
and `expense.category.owner` always in sync instead of needing a special
case to explain.

## Auth
- `POST /api/auth/register` / `POST /api/auth/login` - both public, return a JWT
- Everything else needs `Authorization: Bearer <token>`
- Stateless - no server-side sessions
- No endpoint to grant ADMIN - that's deliberate, done out-of-band for this project

## Endpoints
- `GET/POST /api/categories`, `GET/DELETE /api/categories/{id}`
- `GET/POST /api/expenses` (`?categoryId=`), `GET/DELETE /api/expenses/{id}`

## Error handling
Centralized pattern — one `GlobalExceptionHandler` mapping:
- `ResourceNotFoundException` → 404
- `DuplicateResourceException` → 409 (e.g. username/email already taken)
- `InvalidOperationException` → 400
- `AccessDeniedException` → 403 (role/ownership violations)
- `DataIntegrityViolationException` → 409 (DB constraint backstop)
- `MethodArgumentNotValidException` → 400, field-level messages

## Messaging
Planned: publish an event on expense creation, consume it to keep a running
total. Details once I get there.

## Deployment
Planned: Render, Postgres instead of H2, JWT secret as an env var.

## Design decisions
(filling in as I go)

## Testing
(planned: MockMvc suite covering auth + ownership boundaries)

## Bug log
(to be done after baseline is done - bugs, before/after, root cause)
