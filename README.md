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

Tickets are saved in `./data/` (git-ignored), so they survive a restart. On a new day the dev profile fills the empty queue with the Figma sample day. To start over, stop the app and delete the `data/` folder.

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

Spring injects [`RequestQueueService`](src/main/java/com/queupid/pilago/service/RequestQueueService.java):

- **In memory:** today's tickets are kept in `ArrayList`s, with seniors and PWD ahead of regular tickets.
- **Saved right away:** every change is written through the [`RequestStorage`](src/main/java/com/queupid/pilago/storage/RequestStorage.java) interface.
  - `JpaRequestStorage` saves to H2 on a laptop and to PostgreSQL on Render.
  - The unit tests use `InMemoryRequestStorage` instead, so they need no database.
- **After a restart:** the first call of the day reloads today from storage, and the ticket numbers continue from the last one.
- **At midnight:** the queue starts empty and the numbers go back to 1.

`SampleDay` fills an empty day with the same tickets as the Figma screens: C-003 now serving, P-005 next, S-004 waiting for signature, S-002 ready, C-007 on hold. It runs when `pilago.sample-data=true`, which is on in dev and off on Render.

Useful model methods for templates:

- `request.ticketNumber`, `request.status.messageKey`, `request.status.cssModifier` (for `pg-badge--…`)
- `request.priorityGroup.messageKey`, `request.priorityGroup.cssModifier` (for `pg-tag--…`)
- `request.documentType.messageKey`, `request.purpose.messageKey`, `request.fee`, `request.free`
- `request.holdReason.messageKey`, `request.holdNote`
- `request.document.requirements` (message keys)

## Page text

The app is English-only and matches the Figma (it was English and Filipino side by side until 2026-11-23). Every label lives in a messages file, for example `status.waiting=Waiting`, and templates print it with `${@bi.t('status.waiting')}`.

- Put your words in your own file (`messages_resident.properties` or `messages_staff.properties`).
- Shared words are in `messages.properties`.
- A test fails if a text is empty or a Filipino `.fil` key comes back.

## Deploy (Render + Neon)

The app runs on [Render](https://render.com)'s free plan from the `Dockerfile`, with a free [Neon](https://neon.tech) PostgreSQL database. Both need an account, which the team lead creates. Secrets are typed into Render and never committed.

1. **Neon:** create a project in the Singapore region. Neon shows a connection string like:

   ```
   postgresql://USER:PASSWORD@ep-xxxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
   ```

   Split it into three values for Render:
   - `DATABASE_URL` = `jdbc:postgresql://ep-xxxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require`
   - `DATABASE_USERNAME` = `USER`
   - `DATABASE_PASSWORD` = `PASSWORD`
2. **Render:** choose New → Blueprint, pick this repository, and Render reads `render.yaml`. Fill in:
   - the three database values
   - `SECRETARY_PASSWORD` and `CAPTAIN_PASSWORD`: the first staff passwords, choose strong ones
   - `PILAGO_PUBLIC_URL`: the Render address, shown on the TV board
3. **Wait for the first build** (about 5–10 minutes). The tables are created on first start, and so are the staff accounts `SEC-001` and `CAP-001`.
4. **Check it:** open the Render link on a phone and run the full flow: get a ticket → secretary calls, takes payment and forwards → captain signs → secretary releases.

Every merge to `main` deploys again. The free plan sleeps after 15 minutes without visits, so the first visit after a while takes about a minute. The TV board's refresh keeps it awake during office hours.

## How we work

1. Branch from `main`: `feature/<part>-<thing>`, for example `feature/resident-wizard`.
2. Open small PRs, at least two a week. Name the Figma screen (for example "R2 · Step 1") and attach a phone and a desktop screenshot.
3. Isaac reviews and merges. CI (`./mvnw verify`) must pass.
4. Need something from the queue that isn't in the contract? Ask Isaac; don't work around it.
