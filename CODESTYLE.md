# Code Style

Rules for writing code in this repository. Short and opinionated. No formatter or linter is configured, so every rule is caught in review.

If a rule below conflicts with what an IDE, a formatter preset or a tutorial suggests, this file wins. If it conflicts with `SPECS.md`, the SPECS wins.

---

## 0. The four rules that matter most

1. **English for all code.** Identifiers (domain names included), comments, commits, logs, tests, branch names. Spanish only for text shown to users (see §1).
2. **No decorative separators in source code.** Never `// ------------`, never `/* ===== */`, never box drawings.
3. **No over-explained comments.** If the code says it, the comment must not repeat it.
4. **Credentials stay out of logs and messages.** `DB_PASSWORD` and any connection string that carries credentials never reach a log line or an exception message.

---

## 1. Language

All code is written in English: identifiers (domain names included), table names, routes, template names, comments, commit messages, log lines, test names and branch names. `SPECS.md` §33 lists the English name of every domain term.

Spanish is used only for text the user sees on the interface:
- Thymeleaf labels and page text.
- Validation messages.
- Flash messages.
- The message of business exceptions.
- Reward names and descriptions in the seed data.

Exceptions that signal programming errors (illegal state, null parameter) use English messages and never reach the user.

```java
// bad
// Recorre las compras del cliente y suma los puntos
for (Compra compra : compras) { }
```

```java
// good
// Round to the nearest dollar so charm prices such as 9.99 still earn the full point (SPECS §4).
int earnedPoints = amount.setScale(0, RoundingMode.HALF_UP).intValueExact();
```

Identifiers use ASCII only. No accents, no emoji, no non-Latin scripts in names, package paths or file names. String literals shown to users may contain any Unicode they need (`ñ`, accents).

---

## 2. Comments

### 2.1 Do not explain what the code already says

```java
// bad
// increment the counter by one
counter++;

// bad
/**
 * Gets the name.
 * @return the name
 */
public String getName() { return name; }
```

Delete both. The second one is worse than nothing: it costs a review, it costs a merge conflict, and it says less than the signature.

### 2.2 Comment the *why*, the non-obvious, and the trap

```java
// good
// Compare against the injected clock, never LocalDate.now(): tests rely on a fixed date.
LocalDate today = LocalDate.now(clock);
```

```java
// good
// Lock the customer row: two concurrent redemptions must not read the same balance (SPECS §15).
Customer customer = customerRepository.findByIdForUpdate(id).orElseThrow(...);
```

Good comment triggers: a workaround for a framework or database quirk, an ordering constraint, a time-handling decision, a spec reference (`SPECS §4`), a deliberate deviation from the obvious approach.

### 2.3 Javadoc

Javadoc is optional. Use it only where a method has a contract the signature cannot express: preconditions, thrown exceptions, side effects. As filler it is forbidden.

Write it as a contract, not a paraphrase:

```java
/**
 * Redeems a reward for the given customer.
 *
 * @throws CustomerNotFoundException if the customer does not exist
 * @throws RewardNotFoundException if the reward does not exist
 * @throws InactiveRewardException if the reward is not active
 * @throws InsufficientPointsException if the customer balance is lower than the reward cost
 */
public Redemption redeem(RedemptionForm form) {
```

No `@author` tags. Git knows. No `@version` tags. The build knows.

### 2.4 Banned comment forms

- Separator banners of any kind, including Unicode box characters.
- Commented-out code. Delete it; git has it.
- Changelog comments (`// modified by X on 2026-01-01`).
- `// TODO` without an issue.

```java
// bad
// TODO fix this later

// good
// TODO(#12): paginate the purchase history.
```

`FIXME` and `HACK` are allowed under the same rule. A `TODO` that outlives the roadmap phase it was written in is a bug report, not a comment.

---

## 3. Formatting

- 4 spaces, no tabs.
- 120 column soft limit. Break at a semantic boundary, not at column 121.
- Braces on the same line, always present, even for a one-statement `if`.
- One statement per line. One declaration per line.
- Blank lines separate logical blocks. A method needing five blank-line groups probably wants to be five methods.
- Imports: no wildcards, no unused, ordered `java` → `javax` → third-party → `com.example`.
- Files end with a newline. No trailing whitespace.
- `final` on parameters is not required. `final` on fields is (see §7).

No formatter is configured. Set the IDE to 4 spaces and 120 columns. Do not argue formatting in review; change this file or move on.

### 3.1 Separators, explicitly

```java
// bad
// =============================================================================
// Points rules
// =============================================================================

// bad
/* --------------------------- helpers --------------------------- */

// bad
//////////////////////////////////////////////////////////
```

If a file needs visual sections to be navigable, the file is too big. Split it. Reviewers reject any comment line made of three or more repeated punctuation characters.

Markdown documentation may use `---` as a horizontal rule. That is documentation, not source.

---

## 4. Naming

| Kind              | Convention                | Example                                |
| ----------------- | ------------------------- | -------------------------------------- |
| Package           | lowercase, no underscores | `com.example.coffeeloyalty.service`    |
| Class / interface | `PascalCase`              | `RedemptionService`                    |
| Method / field    | `camelCase`               | `findByIdForUpdate`, `requiredPoints`  |
| Constant          | `UPPER_SNAKE_CASE`        | `MAX_PURCHASE_AMOUNT`                  |
| Type parameter    | single letter or short    | `T`, `ID`                              |
| Test method       | `camelCase`, a sentence   | `roundsNinetyNineCentsUpToTheNextPoint` |

Rules:

- No Hungarian notation, no `m_` or `_` prefixes, no `I` prefix on interfaces.
- No abbreviations except universally known ones (`id`, `dto`, `sql`, `uuid`).
- No `Impl` suffix. A class with a single implementation does not need an interface: `PurchaseService` is a class. If an interface is introduced, name it for the role and its implementations for the specific.
- Layer suffixes are part of the name: `CustomerController`, `CustomerService`, `CustomerRepository`, `PurchaseForm`, `InsufficientPointsException`.
- Form objects end in `Form`. Read-only view models, if any, end in `View`.
- Boolean methods read as assertions: `isActive()`.
- Do not name a class `Utils`, `Helper`, `Manager` or `Common`. Name it for what it does.

---

## 5. Packages and layer boundaries

```
com.example.coffeeloyalty.controller   HTTP and views: receive, validate, delegate, return a view
com.example.coffeeloyalty.service      business rules
com.example.coffeeloyalty.repository   Spring Data JPA access
com.example.coffeeloyalty.entity       JPA entities
com.example.coffeeloyalty.dto          forms, only where they help
com.example.coffeeloyalty.exception    business exceptions
com.example.coffeeloyalty.config       Clock bean, reward seeder, demo data loader
```

Hard boundaries:

- Dependencies point one way: `controller` → `service` → `repository`.
- Controllers do not call repositories and contain no business rules.
- Services own the business rules. They do not depend on web types (`HttpServletRequest`, `Model`).
- Repositories contain no business rules.
- Templates contain no business logic. They display values the controller or the service prepared; points are never computed in a template.
- Forms exist only where they separate form data from JPA entities (SPECS §11). Do not create them to follow a rigid structure.

---

## 6. Dependencies

The stack is defined in `SPECS.md` §7 and §26. Do not add a dependency outside it.

Adding one requires: an issue explaining why the existing stack cannot cover it, its license (MIT / Apache-2.0 / BSD only), and explicit approval. Expect to be told no.

- **No Lombok and no DevTools.** Constructors, getters and setters are written by hand. That is part of the exercise.
- Services and controllers use hand-written constructors for injection.
- AssertJ and Mockito come bundled with the Spring Boot test starters and need no separate approval. SLF4J comes with Spring Boot.
- Docker is not a dependency of the code. A single `Dockerfile` is allowed for the Render deployment only (SPECS §30). No docker-compose, no CI/CD.

---

## 7. Design

- **Money is `BigDecimal`.** Never `double` or `float`. Points are whole numbers.
- **Form objects that back Thymeleaf forms are plain mutable beans**, because form binding works through getters and setters. Read-only view models may be `record`.
- **JPA entities** keep a no-arg constructor and mutable fields, as JPA requires. They do not override `equals`, `hashCode` or `toString`.
- **Fields in controllers, services and components are `private final`** and injected through the constructor. No field injection.
- **Time comes from an injected `Clock`.** Never call `LocalDate.now()` or `LocalDateTime.now()` in business logic (SPECS §21). Tests use `Clock.fixed`.
- **The balance changes in one place per movement.** Only `PurchaseService` and `RedemptionService` modify `Customer.points`, inside a transaction, after reading the customer with `findByIdForUpdate`.
- **No static mutable state.** Shared state lives in Spring-managed beans.
- **Constructors do not do work.** No I/O, no database access in a constructor.
- **Return `Optional<T>` for a genuinely absent single result.** Never return `Optional` from a field, never as a parameter, never for collections. Return an empty collection.
- **Never return `null` from a public method.** Empty collection, `Optional`, or throw.
- **Never accept `null` silently.** Validate at the boundary with Bean Validation on forms. Inside services, throw `NullPointerException` with the parameter name (`Objects.requireNonNull(value, "value")`), or accept it explicitly and document it.
- **No checked exceptions.** Business exceptions are unchecked.

---

## 8. Errors

- Business exceptions are unchecked and named for the cases in SPECS §19: `CustomerNotFoundException`, `RewardNotFoundException`, `DuplicateEmailException`, `InactiveRewardException`, `InsufficientPointsException`.
- A business exception carries the message shown to the user, in Spanish, without technical details.
- Expected business errors (invalid form, not found, duplicate email, inactive reward, insufficient points) reach the user as a readable message in the page (SPECS §19). Unexpected errors show a generic error page. Never show a stack trace, SQL or a vendor message.
- Every message names the concrete thing: the customer, the reward, the points involved. `"Datos inválidos"` is not a message. `"Ana Torres tiene 20 puntos y la recompensa Combo de café requiere 50."` is.
- Messages never contain credentials.
- Persistence exceptions are translated into business exceptions and keep the original as the cause.
- Never `catch (Exception e) { }`. Never catch to log-and-rethrow the same thing twice.
- Fail early: format validation lives in the Form, business rules in the Service, both before touching the database.
- Do not use exceptions for control flow. Ask with `existsBy...`; do not catch `CustomerNotFoundException` to decide.

---

## 9. Logging

### 9.1 The rules

- Log through SLF4J: `private static final Logger log = LoggerFactory.getLogger(PurchaseService.class);`. Never `System.out.println`, never `System.err.println`, never `e.printStackTrace()`.
- Messages are parameterized, never concatenated.

```java
// bad
log.debug("registered purchase " + purchaseId + " for customer " + customerId);

// good
log.debug("registered purchase {} for customer {}", purchaseId, customerId);
```

- Guard expensive argument computation with `log.isDebugEnabled()`.
- Never log database credentials or connection strings that carry them.
- Expected business errors (invalid form, not found, duplicate email, insufficient points) are not logged above `DEBUG`.

### 9.2 Levels

| Level   | Use for                                                           |
| ------- | ----------------------------------------------------------------- |
| `ERROR` | The operation failed unexpectedly (a 500)                         |
| `WARN`  | Handled but suspicious                                            |
| `INFO`  | Once-per-lifecycle facts: application started, seed data loaded   |
| `DEBUG` | Per-operation detail: purchase registered, reward redeemed        |
| `TRACE` | Per-item detail                                                   |

`INFO` must stay quiet in a steady-state application. If a request handler can reach it, it is `DEBUG`.

---

## 10. Tests

- JUnit 6, AssertJ and Mockito. Mockito is for unit tests of Services, replacing repositories. If a unit needs heavy mocking, the design is wrong.
- Unit tests of Services run without a database or a Spring context, with `Clock.fixed` wherever dates matter.
- Integration tests run against the local `coffee_loyalty_test` PostgreSQL database under the `test` profile. No H2, no Testcontainers (SPECS §28).
- Test names are sentences: `rejectsRedemptionWhenBalanceIsTooLow`, `deletesPurchasesWhenTheCustomerIsDeleted`.
- One behaviour per test. No assertion-free tests. No `Thread.sleep`.
- Priority tests follow SPECS §28: points rounding at the edges, balance updates, insufficient points, inactive reward, rollback, cascade delete, balance consistency and concurrent redemptions.
- Every bug fix begins with a failing test.
- Route and template changes are verified by walking the demo scenarios (SPECS §29).

---

## 11. Git

- Conventional commits: `feat(purchase): round points to the nearest dollar`, `fix(redemption): lock the customer before reading the balance`, `docs(readme): …`, `refactor`, `test`, `perf`, `build`, `chore`.
- Imperative mood, lowercase subject, no trailing period, under 72 characters.
- The body explains *why*. The diff already explains *what*.
- One logical change per commit. Formatting-only changes go in their own commit.
- Branches: `feat/redemption-form`, `fix/duplicate-email-case`.
- Never commit build output (`target/`), IDE files, or credentials.

---

## 12. PR checklist

Copy this into the PR description:

```
- [ ] English everywhere except text shown to users
- [ ] No separator comments, no commented-out code, no redundant javadoc
- [ ] No dependency added outside the SPECS stack, no Lombok
- [ ] Business rules only in Services, no business logic in templates
- [ ] Time comes from the injected Clock
- [ ] Balance changes only in PurchaseService and RedemptionService, inside a transaction
- [ ] No credentials in code, logs or exception messages
- [ ] Tests added and ./mvnw test is green
- [ ] SPECS.md and README.md updated if behavior changed
```

---

## 13. Enforcement

No formatter or linter is configured in this project. Everything above is caught in review. Reviewers are expected to cite the rule number.

```bash
./mvnw test
```
