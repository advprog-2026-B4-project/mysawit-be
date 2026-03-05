# MySawit Backend - Agent Rulebook

## Architecture: Strict Hexagonal (Ports & Adapters)

```
src/main/java/id/ac/ui/cs/advprog/mysawitbe/
  modules/
    [module]/
      domain/          -- Pure Java: domain models, value objects, domain events (NO @Entity, NO Spring annotations)
      application/
        dto/           -- Java Records (immutable request/response contracts)
        event/         -- Domain event records published via ApplicationEventPublisher
        port/
          in/          -- Use Case interfaces (what the outside world can ask the app to do)
          out/         -- Repository/External Port interfaces (what the app needs from infrastructure)
      infrastructure/
        persistence/   -- JPA @Entity classes + JpaRepository + MapStruct mappers
        web/           -- @RestController (thin adapters, delegate to Use Cases only)
        external/      -- Cloudflare R2 adapters, Midtrans gateway adapters
```

## Mandatory Rules

### Mapping
- Use **MapStruct** (`@Mapper(componentModel = "spring")`) for ALL conversions between domain models and JPA entities / DTOs.
- NEVER manually write `new DomainModel(entity.getX(), ...)` mapping code.

### Database Migrations
- ALL schema changes go through **Flyway** migration scripts in `src/main/resources/db/migration/`.
- Naming: `V{version}__{description}.sql`
- NEVER use `spring.jpa.hibernate.ddl-auto=create` or `update`.

### Persistence
- **PostgreSQL** for all relational data.
- **Redis** for session caching and rate limiting.

### Storage
- Use `StoragePort` (port/out) for file operations. The Cloudflare R2 adapter is the concrete implementation.
- All upload URLs returned as `String` (pre-signed or public CDN URL).
cd 
### Authentication & Authorization
- **Dual-Auth:** Email/password (Spring Security) + Google OAuth2.
- JWT issued after successful login; use Spring Security filter chain for stateless auth.
- `@PreAuthorize("hasRole('ADMIN')")` etc. at the Use Case interface level via AOP, NOT inside domain.

### Events (Async Inter-Module Communication)
- Use Spring's `ApplicationEventPublisher` to publish domain events.
- Listeners use `@EventListener` (same thread) or `@TransactionalEventListener` for post-commit.
- Event records live in `application/event/`. Listeners live in the consuming module's `application/` layer.

### Testing
- Target **75% line coverage** minimum (enforced by JaCoCo in `build.gradle.kts`).
- Unit-test domain logic and use case implementations independently (no Spring context).
- Integration tests use `@SpringBootTest` + Testcontainers (postgres + redis).

### API Response Wrapper
- ALL REST endpoints MUST return `ApiResponse<T>` from `common.dto.ApiResponse`.
- Use static factory methods: `ApiResponse.success(data)`, `ApiResponse.error(message)`, `ApiResponse.fail(message, data)`.
- Controllers return `ResponseEntity<ApiResponse<T>>` with appropriate HTTP status codes.
- The wrapper provides: `success: boolean`, `message: String`, `data: T`, `error: Object`, `timestamp: String`.

### SOLID Principles (MANDATORY)
ALL code must strictly adhere to SOLID principles:

- **S - Single Responsibility Principle**: Each class has ONE reason to change. Controllers only handle HTTP; services only coordinate business logic; repositories only manage persistence.
- **O - Open/Closed Principle**: Extend behavior via interfaces and composition, NOT modification. Use Strategy pattern for varying algorithms; use port/adapter pattern for infrastructure swaps.
- **L - Liskov Substitution Principle**: Implementations must be substitutable for their interfaces without breaking behavior. All `port/out` implementations must honor the contract fully.
- **I - Interface Segregation Principle**: Use case interfaces should be cohesive and minimal. Split fat interfaces into focused ones (e.g., `CommandUseCase` vs `QueryUseCase`). Clients should not depend on methods they don't use.
- **D - Dependency Inversion Principle**: High-level modules (use cases) depend on abstractions (`port/out`), NOT concrete implementations. NEVER import infrastructure classes from application layer; always inject via ports.

### Strictly Forbidden
- `@Entity` or `@Table` annotations on classes in `domain/`.
- Business logic inside `@RestController` or `@Repository`.
- Direct calls between module `infrastructure/` layers (use events or ports).
- Hardcoded credentials anywhere; use `application.yml` env-var placeholders.
- Violating any SOLID principle (will be caught in code review).
