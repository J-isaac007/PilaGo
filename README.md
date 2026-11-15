# PilaGo

Barangay document ticketing and queue system. Residents get a ticket on their own phone for a barangay clearance, certificate of indigency or certificate of residency. The secretary calls tickets (seniors and PWD first), the captain signs, and a TV board shows who is being served.

Team Queupid · CS-2101 · OOP 101 · Batangas State University

- Design: Figma file "PilaGo: UI/UX" (`TtV36VNpX8yrZLU2i6DKIj`)
- Stack: Java 25, Spring Boot 4.1, Thymeleaf + htmx, Spring Security, Spring Data JPA, H2 (dev) / PostgreSQL (prod)

## Run it

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Open http://localhost:8080. Dev staff logins: `SEC-001` / `secretary123`, `CAP-001` / `captain123`. The H2 console is at `/h2-console` (JDBC URL `jdbc:h2:file:./data/pilago`).

Run the tests:

```bash
./mvnw verify
```

## Who owns what

| Part | Owner | Folders |
|---|---|---|
| 1 · Core and queue engine | Isaac | `model/`, `exception/`, `service/`, `storage/`, `config/` (not `SecurityConfig`), `messages.properties`, `pom.xml`, `.github/` |
| 2 · Resident pages and TV board | Carryl | `web/resident/`, `web/display/`, `templates/resident/`, `templates/display/`, `templates/fragments/`, `templates/layout.html`, `static/`, `messages_resident.properties` |
| 3 · Staff, captain and security | Precious | `web/staff/`, `web/captain/`, `config/SecurityConfig.java`, `exception/GlobalExceptionHandler.java`, `templates/staff/`, `templates/captain/`, `templates/auth/`, `templates/error/`, `messages_staff.properties`, `src/test/.../web/` |

All packages live under `src/main/java/com/queupid/pilago/`.

## The queue contract

Pages talk to the queue only through [`RequestQueue`](src/main/java/com/queupid/pilago/service/RequestQueue.java). Inject the interface:

```java
@Controller
class ResidentPage {
    private final RequestQueue queue;
    ResidentPage(RequestQueue queue) { this.queue = queue; }
}
```

Today Spring injects `FakeRequestQueue`, which holds the same sample day as the Figma screens: C-003 now serving, P-005 next, S-004 waiting for signature, S-002 ready, C-007 on hold. It enforces every rule (priority, transitions, duplicates, holds) but saves nothing, so a restart brings the sample day back. The database-backed queue replaces it in week 4 without changing your code.

Useful model methods for templates:

- `request.ticketNumber`, `request.status.messageKey`, `request.status.cssModifier` (for `pg-badge--…`)
- `request.priorityGroup.messageKey`, `request.priorityGroup.cssModifier` (for `pg-tag--…`)
- `request.documentType.messageKey`, `request.purpose.messageKey`, `request.fee`, `request.free`
- `request.holdReason.messageKey`, `request.holdNote`
- `request.document.requirements` (message keys)

## Bilingual copy

Every label is English and Filipino side by side, with no language switch. Each key has a pair: `status.waiting=Waiting` and `status.waiting.fil=Naghihintay`. Put your words in your own file (`messages_resident.properties` or `messages_staff.properties`). Shared words are in `messages.properties`. A test fails if a key is missing its `.fil` twin.

## How we work

1. Branch from `main`: `feature/<part>-<thing>`, for example `feature/resident-wizard`.
2. Open small PRs, at least two a week. Name the Figma screen (for example "R2 · Step 1") and attach a phone and a desktop screenshot.
3. Isaac reviews and merges. CI (`./mvnw verify`) must pass.
4. Need something from the queue that isn't in the contract? Ask Isaac; don't work around it.
