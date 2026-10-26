# SPECS.md — Tienda de Café · App de Fidelización

Fuente de verdad del proyecto. Si otro documento la contradice, gana este. El orden de construcción vive en `ROADMAP.md`; las reglas de código, en `CODESTYLE.md`.

---

## 1. Resumen del proyecto

Aplicación web monolítica para una tienda de café. Permite que los clientes acumulen puntos con sus compras y los canjeen por recompensas.

Es el proyecto 002 del portafolio: una **demo final** para practicar y demostrar:

- Java 21
- Spring Boot, Spring MVC y Spring Data JPA
- PostgreSQL
- Thymeleaf y HTML/CSS
- Maven
- Validaciones, reglas de negocio, transacciones y concurrencia básica
- Despliegue de una aplicación Java en Render

El proyecto **no busca ser escalable ni estar preparado para producción**. Se priorizan claridad, funcionamiento completo y facilidad para explicar el código en una demo. La demo pública usa solo datos ficticios.

---

## 2. Objetivo funcional

El sistema debe permitir:

1. Registrar, editar y eliminar clientes.
2. Registrar compras asociadas a un cliente.
3. Calcular automáticamente los puntos de cada compra.
4. Consultar el saldo de puntos de un cliente.
5. Listar las recompensas activas.
6. Canjear una recompensa con los puntos acumulados.
7. Validar que el cliente tenga puntos suficientes antes de canjear.
8. Consultar el historial de compras y de canjes.

---

## 3. Alcance

### Incluido

- Gestión de clientes (alta, edición y eliminación).
- Registro de compras con acumulación automática de puntos.
- Consulta de puntos con el historial del cliente.
- Catálogo de recompensas (solo las activas).
- Canje de recompensas.
- Historial de compras y de canjes.
- Interfaz web con Thymeleaf.
- Persistencia con PostgreSQL.
- Validaciones y manejo básico de errores.
- Datos de demo y despliegue en Render (§22 y §30).

### Fuera del alcance

No se implementarán:

- Autenticación ni inicio de sesión.
- Roles y permisos.
- Panel administrativo avanzado.
- API pública para terceros.
- Microservicios.
- Docker Compose, Kubernetes, CI/CD e infraestructura como código (incluido `render.yaml`).
- Redis, Kafka u otros sistemas de infraestructura.
- CRUD de recompensas desde la interfaz.
- Edición o anulación de compras y canjes.
- Sistema de inventario de recompensas.
- Pagos en línea.
- Notificaciones por correo.
- Reportes avanzados.
- Auditoría empresarial.
- Configuración dinámica de reglas de puntos.
- Promociones o puntos extra.
- Internacionalización y multimoneda.
- Aplicación móvil.

**Excepción acotada:** se permite un único `Dockerfile` para desplegar en Render, porque Render solo ejecuta Java mediante una imagen Docker (§30). No se usa Docker en el desarrollo local.

La regla de puntos es fija dentro de la lógica de negocio para mantener el proyecto pequeño.

---

## 4. Regla de fidelización

> **1 punto por cada dólar (USD 1) de la compra, redondeando al dólar más cercano: desde USD 0.50 sube.**

El redondeo existe para que los precios terminados en `.99` (una práctica común de venta) no penalicen al cliente: USD 9.99 cuenta como USD 10.

### Ejemplos

| Monto de compra | Puntos |
|---:|---:|
| $0.49 | 0 |
| $0.50 | 1 |
| $1.00 | 1 |
| $9.49 | 9 |
| $9.99 | 10 |
| $10.00 | 10 |
| $25.49 | 25 |
| $25.99 | 26 |
| $50.00 | 50 |

### Fórmula

```text
puntos_obtenidos = monto redondeado a 0 decimales con HALF_UP
```

Se calcula con `BigDecimal.setScale(0, RoundingMode.HALF_UP)` y se convierte con `intValueExact()`. Nunca con `double` ni `float`.

Una compra menor a USD 0.50 se registra con 0 puntos. El saldo del cliente aumenta automáticamente al registrar una compra.

---

## 5. Reglas de negocio

### Cliente

- El nombre es obligatorio (máximo 100).
- El email es obligatorio, con formato válido (máximo 150).
- El email es único sin distinguir mayúsculas: se guarda sin espacios laterales y en minúsculas.
- La fecha de incorporación se registra automáticamente al crear el cliente.
- El saldo inicial de puntos es `0`.
- Se pueden editar el nombre y el email. Al editar, la unicidad del email excluye al propio cliente. El saldo nunca se edita a mano.

### Compra

- Pertenece a un cliente existente.
- El monto está en dólares (USD), es mayor que `0`, como máximo `1000.00` y admite hasta 2 decimales.
- Los puntos se calculan automáticamente (§4); la persona usuaria nunca los introduce.
- Al registrar la compra aumenta el saldo del cliente y los puntos quedan guardados en la compra.
- Las compras son inmutables: no se editan ni se anulan.

### Recompensa

Tiene nombre, descripción, puntos requeridos y estado `active`.

- Las recompensas se cargan al arrancar (§22). No hay CRUD desde la interfaz.
- El catálogo muestra solo las recompensas activas.

### Canje

- Solo se canjean recompensas con `active = true`.
- El saldo del cliente debe ser mayor o igual al costo de la recompensa.
- El costo se toma de la recompensa en el momento del canje y se guarda en el canje (`pointsSpent`).
- Si el canje se aprueba, los puntos se descuentan y el canje queda registrado, en una sola transacción (§15).
- Si el saldo no alcanza, el canje se rechaza y el saldo no cambia.
- Los canjes son inmutables.

### Eliminación de clientes

El cliente puede eliminarse. Sus compras y canjes se eliminan con él mediante `ON DELETE CASCADE` en la base de datos: es una demo y no requiere conservar historial independiente. La interfaz pide confirmación antes de eliminar.

La clave foránea de los canjes hacia las recompensas **no** tiene cascada: las recompensas nunca se eliminan.

---

## 6. Arquitectura

Arquitectura por capas dentro de una única aplicación Spring Boot.

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

### Responsabilidades

- **Controller:** recibe peticiones HTTP, valida formularios con Bean Validation, delega en los Services, elige la vista y redirige tras operaciones exitosas.
- **Service:** contiene la lógica de negocio: cálculo de puntos, saldo, validación de recompensa activa y de saldo, unicidad del email y transacciones.
- **Repository:** acceso a PostgreSQL con Spring Data JPA. Sin DAOs manuales ni reglas de negocio.

Los controllers no llaman repositories ni modifican entidades persistentes. Las plantillas no contienen lógica de negocio.

---

## 7. Stack tecnológico

| Tecnología | Uso |
|---|---|
| Java 21 LTS | Lenguaje principal |
| Spring Boot (la versión estable que ofrezca Initializr; se anota aquí al crear el proyecto) | Framework principal |
| Spring Web / MVC | Controladores web |
| Spring Data JPA + Hibernate | Persistencia |
| PostgreSQL | Base de datos (local en desarrollo; Neon en la demo pública) |
| Thymeleaf | Motor de vistas |
| Bean Validation | Validaciones |
| Maven | Gestión del proyecto |
| JUnit 5, AssertJ y Mockito | Pruebas (vienen con Spring Boot Test) |
| HTML/CSS | Interfaz |
| Docker | Solo para desplegar en Render (§30) |

No se usa Lombok ni DevTools. No se requiere JavaScript; solo se admite el `confirm` en línea del botón de eliminar.

---

## 8. Modelo de datos

Los nombres de tablas y columnas están en inglés, en `snake_case`.

### 8.1 `customers`

| Campo | Tipo PostgreSQL | Restricciones |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `name` | `VARCHAR(100)` | NOT NULL |
| `email` | `VARCHAR(150)` | NOT NULL, UNIQUE |
| `join_date` | `DATE` | NOT NULL |
| `points` | `INTEGER` | NOT NULL, DEFAULT 0, >= 0 |

### 8.2 `purchases`

| Campo | Tipo PostgreSQL | Restricciones |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `customer_id` | `BIGINT` | FK a `customers`, NOT NULL, `ON DELETE CASCADE` |
| `amount` | `NUMERIC(12,2)` | NOT NULL, > 0 |
| `points_earned` | `INTEGER` | NOT NULL, >= 0 |
| `created_at` | `TIMESTAMP` | NOT NULL |

### 8.3 `rewards`

| Campo | Tipo PostgreSQL | Restricciones |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `name` | `VARCHAR(100)` | NOT NULL |
| `description` | `VARCHAR(255)` | NOT NULL |
| `required_points` | `INTEGER` | NOT NULL, > 0 |
| `active` | `BOOLEAN` | NOT NULL, DEFAULT TRUE |

### 8.4 `redemptions`

| Campo | Tipo PostgreSQL | Restricciones |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `customer_id` | `BIGINT` | FK a `customers`, NOT NULL, `ON DELETE CASCADE` |
| `reward_id` | `BIGINT` | FK a `rewards`, NOT NULL, sin cascada |
| `points_spent` | `INTEGER` | NOT NULL, > 0 |
| `created_at` | `TIMESTAMP` | NOT NULL |

### Relaciones

```text
customers 1 ─────── N purchases
customers 1 ─────── N redemptions
rewards   1 ─────── N redemptions
```

`ddl-auto=update` no genera restricciones `CHECK`: las condiciones `>` y `>=` se garantizan con validación y en los Services. El `ON DELETE CASCADE` se genera con `@OnDelete` de Hibernate (§10) y se verifica con `\d purchases` y `\d redemptions` en psql.

---

## 9. Decisiones de tipos Java

- **Dinero:** `BigDecimal`. Nunca `double` ni `float`.
- **Puntos:** `Integer` en las entidades; la regla nunca genera fracciones.
- **Fecha de incorporación:** `LocalDate`.
- **Fecha de compra y de canje:** `LocalDateTime`, tomada de un `Clock` inyectado (§21).

---

## 10. Entidades JPA

Sin Lombok: constructor sin argumentos, constructores, getters y setters escritos a mano. Las entidades no sobrescriben `equals`, `hashCode` ni `toString`.

### `Customer`

```text
id, name, email, joinDate, points
```

Sin colecciones: las compras y los canjes se consultan por repository.

### `Purchase`

```text
id, customer, amount, pointsEarned, createdAt
```

`@ManyToOne Customer customer` con `@OnDelete(action = OnDeleteAction.CASCADE)`.

### `Reward`

```text
id, name, description, requiredPoints, active
```

### `Redemption`

```text
id, customer, reward, pointsSpent, createdAt
```

`@ManyToOne Customer customer` con `@OnDelete(action = OnDeleteAction.CASCADE)` y `@ManyToOne Reward reward` sin cascada.

Las asociaciones `@ManyToOne` usan el fetch por defecto (EAGER): el volumen es de demo y se evita `LazyInitializationException` con `open-in-view=false`.

---

## 11. Formularios y DTOs

Se usan objetos de formulario solo donde separan los datos de la vista de las entidades (beans mutables, porque Thymeleaf enlaza con getters y setters).

| Form | Campos | Uso |
|---|---|---|
| `CustomerForm` | `name`, `email` | Alta y edición de cliente |
| `PurchaseForm` | `customerId`, `amount` | Registro de compra |
| `RedemptionForm` | `customerId`, `rewardId` | Registro de canje |

La fecha de incorporación, los puntos obtenidos y los puntos gastados los establece el backend. No hay capa de DTOs de respuesta: las vistas consumen las entidades.

---

## 12. Paquetes

Paquete base: `com.example.coffeeloyalty`.

```text
src/main/java/com/example/coffeeloyalty
│
├── controller
│   ├── HomeController.java
│   ├── CustomerController.java
│   ├── PurchaseController.java
│   ├── PointsController.java
│   ├── RewardController.java
│   ├── RedemptionController.java
│   └── GlobalExceptionHandler.java
│
├── dto
│   ├── CustomerForm.java
│   ├── PurchaseForm.java
│   └── RedemptionForm.java
│
├── entity
│   ├── Customer.java
│   ├── Purchase.java
│   ├── Reward.java
│   └── Redemption.java
│
├── repository
│   ├── CustomerRepository.java
│   ├── PurchaseRepository.java
│   ├── RewardRepository.java
│   └── RedemptionRepository.java
│
├── service
│   ├── CustomerService.java
│   ├── PurchaseService.java
│   ├── RewardService.java
│   └── RedemptionService.java
│
├── exception
│   ├── CustomerNotFoundException.java
│   ├── RewardNotFoundException.java
│   ├── DuplicateEmailException.java
│   ├── InactiveRewardException.java
│   └── InsufficientPointsException.java
│
└── config
    ├── ClockConfig.java
    ├── RewardSeeder.java
    └── DemoDataLoader.java
```

No se crean paquetes para funcionalidades que no existan.

---

## 13. Repositories

| Repository | Métodos propios |
|---|---|
| `CustomerRepository` | `existsByEmail`, `existsByEmailAndIdNot`, `findAllByOrderByNameAsc`, `findByIdForUpdate` (bloqueo pesimista de escritura, §15) |
| `PurchaseRepository` | `findAllByOrderByCreatedAtDesc`, `findByCustomerIdOrderByCreatedAtDesc` |
| `RewardRepository` | `findByActiveTrueOrderByRequiredPointsAsc` |
| `RedemptionRepository` | `findAllByOrderByCreatedAtDesc`, `findByCustomerIdOrderByCreatedAtDesc` |

No se crean más consultas hasta que una funcionalidad las necesite.

---

## 14. Servicios y lógica de negocio

### `CustomerService`

- Listar y buscar por id (lanza `CustomerNotFoundException`).
- Crear: normaliza el email, valida unicidad y fija `joinDate` y `points = 0`.
- Editar nombre y email, excluyendo al propio cliente en la unicidad.
- Eliminar.
- Si la base rechaza el email por duplicado (condición de carrera), lo traduce a `DuplicateEmailException` conservando la causa.

### `PurchaseService`

```text
customer = bloquear cliente por id (si no existe: CustomerNotFoundException)
earnedPoints = amount.setScale(0, HALF_UP).intValueExact()
purchase = nueva compra(customer, amount, earnedPoints, ahora según el Clock)
customer.points += earnedPoints
guardar compra y cliente
```

### `RewardService`

Listar las recompensas activas y obtener una por id (lanza `RewardNotFoundException`). Sin operaciones de escritura desde la interfaz.

### `RedemptionService`

```text
customer = bloquear cliente por id
reward = buscar recompensa por id

si !reward.active:
    InactiveRewardException

si customer.points < reward.requiredPoints:
    InsufficientPointsException

customer.points -= reward.requiredPoints
redemption = nuevo canje(customer, reward, pointsSpent = reward.requiredPoints, ahora)
guardar cliente y canje
```

---

## 15. Transacciones y concurrencia

`register` de `PurchaseService` y `redeem` de `RedemptionService` son `@Transactional`: el movimiento y el saldo se guardan juntos o no se guarda nada. Las excepciones de negocio son sin chequear y revierten la transacción.

Ambos métodos leen al cliente con `findByIdForUpdate` (`SELECT ... FOR UPDATE`). Así, dos canjes simultáneos del mismo cliente (por ejemplo, un doble clic) se ejecutan en serie: el segundo ve el saldo ya descontado y se rechaza si no alcanza.

Tras un `POST` exitoso se redirige (patrón Post/Redirect/Get) para que recargar la página no repita la operación.

---

## 16. Rutas web

La aplicación se renderiza en el servidor con Thymeleaf. Las rutas están en inglés.

### Inicio

```http
GET /
```

### Clientes

```http
GET  /customers
GET  /customers/new
POST /customers
GET  /customers/{id}/edit
POST /customers/{id}
POST /customers/{id}/delete
```

### Compras

```http
GET  /purchases
GET  /purchases/new
POST /purchases
```

### Puntos

```http
GET /points
GET /points/{customerId}
```

`/points` lista los clientes con su saldo y un enlace a su detalle. El detalle muestra el nombre, el saldo y el historial de compras y de canjes del cliente, del más reciente al más antiguo.

### Recompensas

```http
GET /rewards
```

### Canjes

```http
GET  /redemptions
GET  /redemptions/new
POST /redemptions
```

`GET /redemptions/new` acepta el parámetro opcional `rewardId`, que preselecciona la recompensa (lo usa el botón "Canjear" del catálogo).

### Redirecciones tras éxito

| Operación | Destino | Mensaje |
|---|---|---|
| Crear o editar cliente | `/customers` | Confirmación |
| Eliminar cliente | `/customers` | Confirmación |
| Registrar compra | `/points/{customerId}` | Puntos obtenidos y saldo |
| Registrar canje | `/points/{customerId}` | Recompensa canjeada y saldo |

Los errores de validación y los errores de negocio (email duplicado, recompensa inactiva, saldo insuficiente) se muestran dentro del formulario, conservando lo escrito.

---

## 17. Pantallas Thymeleaf

Todas comparten un fragmento de navegación (`fragments/layout.html`). Los montos se muestran como `$1,234.50`, con separadores explícitos para no depender del locale; las fechas, como `dd/MM/yyyy HH:mm`.

### 17.1 Inicio (`/`)

Nombre de la aplicación y accesos a clientes, compras, puntos, recompensas y canjes.

### 17.2 Clientes (`/customers`)

Tabla con nombre, email, fecha de incorporación, saldo y acciones **Editar** y **Eliminar** (con confirmación). Enlace a **Nuevo cliente**.

### 17.3 Formulario de cliente (`/customers/new`, `/customers/{id}/edit`)

```text
Nombre
Email
[Guardar]
```

### 17.4 Registro de compra (`/purchases/new`)

```text
Cliente (nombre y saldo actual)
Monto (USD)
[Registrar compra]
```

Texto de ayuda: "1 punto por cada dólar, redondeado al más cercano". Tras registrar: "Compra registrada correctamente. Puntos obtenidos: 10".

### 17.5 Historial de compras (`/purchases`)

Fecha, cliente, monto y puntos obtenidos.

### 17.6 Puntos (`/points`, `/points/{customerId}`)

La lista muestra cada cliente con su saldo. El detalle muestra nombre, saldo actual y dos tablas (compras y canjes) con atajos para registrar una compra o un canje.

### 17.7 Catálogo (`/rewards`)

Cada recompensa activa muestra nombre, descripción, costo en puntos y el botón **Canjear**.

### 17.8 Registro de canje (`/redemptions/new`)

```text
Cliente (nombre y saldo actual)
Recompensa (nombre y costo en puntos)
[Canjear]
```

El costo es visible antes de confirmar porque cada opción lo incluye en su texto.

### 17.9 Historial de canjes (`/redemptions`)

Fecha, cliente, recompensa y puntos gastados.

---

## 18. Validaciones y límites de texto

Bean Validation en los formularios. Los mensajes están en español.

| Campo | Reglas | Mensajes de ejemplo |
|---|---|---|
| `CustomerForm.name` | `@NotBlank`, `@Size(max = 100)` | "El nombre es obligatorio." |
| `CustomerForm.email` | `@NotBlank`, `@Email`, `@Size(max = 150)` | "El email no es válido." |
| `PurchaseForm.customerId` | `@NotNull` | "Seleccione un cliente." |
| `PurchaseForm.amount` | `@NotNull`, `@DecimalMin("0.01")`, `@DecimalMax("1000.00")`, `@Digits(integer = 4, fraction = 2)` | "El monto debe ser mayor que cero." |
| `RedemptionForm.customerId` | `@NotNull` | "Seleccione un cliente." |
| `RedemptionForm.rewardId` | `@NotNull` | "Seleccione una recompensa." |

Longitudes máximas: nombre de cliente 100, email 150, nombre de recompensa 100 y descripción de recompensa 255. La longitud de cada columna coincide con su límite.

Las reglas de negocio (saldo suficiente, recompensa activa, email único) las valida el Service, no el controller.

---

## 19. Manejo de errores

Excepciones de negocio, todas sin chequear, con el mensaje en español que ve la persona usuaria y el dato concreto:

| Excepción | Cuándo | Mensaje de ejemplo |
|---|---|---|
| `CustomerNotFoundException` | No existe el cliente | "No existe el cliente con id 7." |
| `RewardNotFoundException` | No existe la recompensa | "No existe la recompensa con id 7." |
| `DuplicateEmailException` | Email ya registrado | "Ya existe un cliente con el email ana@example.com." |
| `InactiveRewardException` | Recompensa inactiva | "La recompensa Combo de café no está disponible." |
| `InsufficientPointsException` | Saldo menor al costo | "Ana Torres tiene 20 puntos y la recompensa Combo de café requiere 50." |

- Los errores de formulario y los errores de negocio de un `POST` se muestran dentro del formulario.
- `GlobalExceptionHandler` (un único `@ControllerAdvice`) atiende solo "no encontrado" y muestra la página `error` con estado 404.
- Cualquier error inesperado muestra una página de error genérica. Nunca se muestra un stack trace, SQL ni mensajes del proveedor.
- Los mensajes nunca contienen credenciales.

---

## 20. Configuración

Todo por variables de entorno, con valores por defecto solo para el entorno local.

| Variable | Obligatoria | Por defecto | Uso |
|---|---|---|---|
| `DB_PASSWORD` | Sí | ninguno | Contraseña de la base |
| `DB_URL` | No | `jdbc:postgresql://localhost:5432/coffee_loyalty` | URL JDBC |
| `DB_USERNAME` | No | `postgres` | Usuario |
| `PORT` | No | `8080` | Puerto HTTP |
| `TEST_DB_URL` | No | `jdbc:postgresql://localhost:5432/coffee_loyalty_test` | Base de pruebas |
| `SPRING_PROFILES_ACTIVE` | No | vacío | `demo` carga datos de ejemplo |

`application.properties`:

```properties
spring.application.name=coffee-loyalty
server.port=${PORT:8080}

spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/coffee_loyalty}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

spring.web.locale=en_US
spring.web.locale-resolver=fixed
```

- `spring.jpa.show-sql` y el caché de Thymeleaf quedan con sus valores por defecto. Para depurar en local se activan con variables de entorno (`SPRING_JPA_SHOW_SQL=true`, `SPRING_THYMELEAF_CACHE=false`), nunca en el archivo versionado.
- El locale `en_US` fijo evita que un navegador en español rinda `12,50` en un campo numérico y lo rechace.
- `ddl-auto=update` gestiona el esquema en esta demo.
- `application-test.properties` (perfil `test`): `TEST_DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `ddl-auto=create-drop`.
- Las credenciales reales nunca se versionan.

---

## 21. Tiempo y zona horaria

La fecha y hora actuales salen siempre de un `Clock` inyectado, definido como bean en `ClockConfig` con `Clock.systemUTC()`. Nunca `LocalDate.now()` ni `LocalDateTime.now()` en la lógica de negocio. Las pruebas usan `Clock.fixed`.

Las fechas se muestran en UTC (Render corre en UTC); el README lo indica.

---

## 22. Datos de demo

Dos componentes en el paquete `config`, ambos idempotentes y ejecutados con `CommandLineRunner`:

- **`RewardSeeder`** (perfil `!test`, primero): inserta las recompensas solo si la tabla `rewards` está vacía. Como no hay CRUD de recompensas, la aplicación necesita este catálogo en cualquier ejecución que no sea de pruebas.
- **`DemoDataLoader`** (perfil `demo`, después): si la tabla `customers` está vacía, crea tres clientes ficticios a través de los Services, de modo que los saldos son coherentes con sus movimientos.

### Recompensas iniciales

| Nombre | Puntos |
|---|---:|
| Café americano | 10 |
| Cappuccino | 20 |
| Postre del día | 30 |
| Combo de café | 50 |

Todas activas.

### Clientes de ejemplo

Ana Torres, Luis Mendoza y María Pérez (emails `@example.com`): uno con saldo suficiente para canjear, uno con saldo bajo y uno sin movimientos. Ninguno se llama Sebastián, para que el escenario de §29 pueda registrarlo.

Las fechas de los movimientos de demo son las del momento de la carga.

---

## 23. Estructura de recursos

```text
src/main/resources
│
├── static
│   └── css
│       └── style.css
│
├── templates
│   ├── index.html
│   ├── error.html
│   ├── fragments
│   │   └── layout.html
│   ├── customers
│   │   ├── list.html
│   │   └── form.html
│   ├── purchases
│   │   ├── list.html
│   │   └── form.html
│   ├── points
│   │   ├── list.html
│   │   └── detail.html
│   ├── rewards
│   │   └── list.html
│   └── redemptions
│       ├── list.html
│       └── form.html
│
├── application.properties
└── application-test.properties
```

---

## 24. Flujos principales

### Compra

```text
Persona usuaria → selecciona cliente → ingresa monto
  ↓
POST /purchases
  ↓
PurchaseService (transacción, cliente bloqueado)
  ↓
Calcula puntos → suma al saldo → guarda compra
  ↓
Redirección a /points/{customerId} con mensaje
```

### Canje

```text
Persona usuaria → selecciona cliente → selecciona recompensa
  ↓
POST /redemptions
  ↓
RedemptionService (transacción, cliente bloqueado)
  ↓
¿Recompensa activa?  No → error en el formulario
  ↓ Sí
¿Saldo suficiente?   No → error en el formulario
  ↓ Sí
Descuenta puntos → guarda canje
  ↓
Redirección a /points/{customerId} con mensaje
```

---

## 25. Criterios de aceptación

### Clientes

- [ ] Se puede registrar un cliente.
- [ ] No se permite un email duplicado, sin distinguir mayúsculas.
- [ ] La fecha de incorporación se genera automáticamente.
- [ ] El cliente comienza con 0 puntos.
- [ ] Se puede consultar la lista de clientes.
- [ ] Se puede editar nombre y email, y el email sigue siendo único.
- [ ] Se puede eliminar un cliente, con confirmación, y desaparecen sus compras y canjes.

### Compras

- [ ] Se puede registrar una compra para un cliente existente.
- [ ] No se permiten montos menores a 0.01 ni mayores a 1000.00.
- [ ] Los puntos se calculan automáticamente.
- [ ] $9.99 produce 10 puntos y $9.49 produce 9.
- [ ] Los puntos se suman al saldo y quedan guardados en la compra.

### Recompensas

- [ ] El catálogo lista solo las recompensas activas.
- [ ] Cada recompensa muestra su costo en puntos.
- [ ] Las recompensas están cargadas al arrancar sin intervención manual.

### Canjes

- [ ] Se puede seleccionar un cliente y una recompensa, viendo el costo.
- [ ] Una recompensa inactiva no puede canjearse.
- [ ] Un cliente sin puntos suficientes no puede canjear y su saldo no cambia.
- [ ] Los puntos se descuentan en un canje válido y el canje queda registrado.
- [ ] Dos canjes simultáneos del mismo cliente nunca dejan el saldo negativo.

### Interfaz

- [ ] Hay navegación entre todas las pantallas.
- [ ] Los errores de validación y de negocio se muestran en el formulario, conservando lo escrito.
- [ ] Las operaciones exitosas muestran una confirmación.
- [ ] Todos los textos visibles están en español.

---

## 26. Dependencias Maven

```text
spring-boot-starter-web
spring-boot-starter-thymeleaf
spring-boot-starter-data-jpa
spring-boot-starter-validation
postgresql (runtime)
spring-boot-starter-test (test)
```

No se agrega nada fuera de esta lista. Sin Lombok ni DevTools.

---

## 27. Convenciones de implementación

- Java 21; todo el código en inglés (identificadores, tablas, rutas, plantillas, comentarios, commits, logs y tests). El español se limita a lo que ve la persona usuaria. Vocabulario completo en §33.
- Sufijos de capa obligatorios: `XController`, `XService`, `XRepository`, `XForm`, `XException`.
- La lógica de negocio solo vive en los Services.
- Los controllers no modifican entidades persistentes: delegan en un Service.
- El orden de construcción está en `ROADMAP.md` (9 fases).
- El resto de reglas de código está en `CODESTYLE.md`.

---

## 28. Estrategia de pruebas

- **Unitarias de Services:** JUnit 5, AssertJ y Mockito; sin base de datos ni contexto Spring; `Clock.fixed`.
- **Integración:** contra la base PostgreSQL local `coffee_loyalty_test`, perfil `test`, `ddl-auto=create-drop`. Se crea la base antes de ejecutar las pruebas. Sin H2 ni Testcontainers.

### Pruebas prioritarias

- Puntos: 0.49 → 0, 0.50 → 1, 9.49 → 9, 9.99 → 10, 10.00 → 10, 25.99 → 26, 1000.00 → 1000.
- Compra: aumenta el saldo, guarda los puntos y rechaza un cliente inexistente.
- Cliente: email duplicado (con distinto uso de mayúsculas), edición que excluye al propio cliente y borrado.
- Canje válido: descuenta el costo de la recompensa y lo guarda en el canje.
- Canje con saldo insuficiente: el saldo no cambia y no se crea ningún canje.
- Canje de recompensa inactiva: rechazado.
- Reversión: si falla el guardado del canje, el saldo no cambia (integración).
- Cascada: eliminar un cliente elimina sus compras y canjes y no toca las recompensas (integración).
- Coherencia: el saldo es igual a los puntos de las compras menos los puntos de los canjes.
- Concurrencia: dos canjes simultáneos que juntos superan el saldo; uno se rechaza (integración).

Las rutas y plantillas se verifican recorriendo los escenarios de §29.

---

## 29. Escenarios de demo

### Escenario A: compra, acumulación y canje

```text
1. Registrar el cliente "Sebastián" (sebastian@example.com).

2. Registrar una compra de $50.00.
   → Se generan 50 puntos.

3. Consultar puntos.
   → Saldo: 50 puntos.

4. Abrir el catálogo de recompensas.
   → "Postre del día": 30 puntos.

5. Canjear la recompensa.
   → Canje aprobado.

6. Consultar puntos de nuevo.
   → Saldo: 20 puntos.

7. Intentar canjear "Combo de café" (50 puntos).
   → Rechazado por saldo insuficiente, con el mensaje en el formulario.
```

### Escenario B: precio terminado en .99

```text
1. Registrar una compra de $9.99.
   → Se generan 10 puntos, igual que una compra de $10.00.
```

### Escenario C: corregir y eliminar

```text
1. Editar el email de un cliente para corregirlo.
   → El historial se conserva.

2. Eliminar un cliente con movimientos, confirmando.
   → Desaparecen el cliente, sus compras y sus canjes.
```

---

## 30. Despliegue en Render

Destino de la demo pública: Render, con datos ficticios.

### Hechos verificados en la documentación de Render (octubre de 2026)

- Render ejecuta de forma nativa Node.js/Bun, Python, Ruby, Go, Rust y Elixir. **Java requiere desplegar una imagen Docker.**
- Un servicio web gratuito se apaga tras 15 minutos sin tráfico y tarda alrededor de un minuto en despertar (con una JVM puede tardar más: se mide en la Fase 9).
- El sistema de archivos del servicio gratuito es efímero.
- Las bases PostgreSQL gratuitas de Render expiran a los 30 días de crearse.
- El servicio gratuito tiene 512 MB de RAM y 0.1 CPU, según fuentes de terceros; se confirma en la Fase 9.

### Decisiones

- Un único `Dockerfile` multietapa en la raíz: compilación con `maven:3.9-eclipse-temurin-21` y ejecución con `eclipse-temurin:21-jre`. La compilación usa `mvn package -DskipTests`, porque las pruebas necesitan una base local. Un `.dockerignore` excluye `target`, `.git`, `.idea` y `*.iml`.
- La imagen se construye en Render a partir del repositorio; no se instala Docker en el entorno local.
- Se limita el heap con `JAVA_TOOL_OPTIONS` (por ejemplo `-Xmx` acorde con 512 MB), definido en el panel de Render.
- La configuración se hace desde el panel de Render, sin `render.yaml`.
- Variables en el panel: `DB_URL` (formato JDBC, con `?sslmode=require`), `DB_USERNAME`, `DB_PASSWORD`, `SPRING_PROFILES_ACTIVE=demo` y `JAVA_TOOL_OPTIONS`. Render define `PORT`.
- **Base de datos: Neon**, PostgreSQL gestionado con plan gratuito sin vencimiento (0.5 GB y 100 horas de cómputo por proyecto al mes, verificado en octubre de 2026). Se suspende tras unos minutos de inactividad y se reactiva sola; el primer acceso tras la pausa es más lento. Se descartaron la base gratuita de Render (expira a los 30 días), Supabase (se pausa tras 7 días de inactividad y se reactiva a mano) y Aiven (se apaga por inactividad y se enciende a mano).
- **URL de conexión:** las cadenas de Postgres gestionado (la de Render, por ejemplo, es `postgresql://USUARIO:CLAVE@HOST:PUERTO/BASE`) no son JDBC y llevan las credenciales dentro. Se arma `DB_URL=jdbc:postgresql://HOST/BASE?sslmode=require` y el usuario y la contraseña van en `DB_USERNAME` y `DB_PASSWORD`. Los datos se copian desde el panel de Neon en la Fase 9.
- El primer arranque crea el esquema (`ddl-auto=update`) y carga recompensas y datos de demo.
- La aplicación no tiene autenticación: cualquiera con la URL puede crear, editar y eliminar datos. Solo datos ficticios.
- Sin dominio propio la demo corre en el dominio de Render.

---

## 31. Checklist de portabilidad

- [ ] Toda la configuración por variables de entorno; `server.port=${PORT:8080}`.
- [ ] Sin estado local en disco.
- [ ] PostgreSQL estándar, sin funciones propias de un proveedor: cambiar de proveedor solo cambia `DB_URL`.
- [ ] Las bases gestionadas suelen exigir TLS: `?sslmode=require` en la URL JDBC.
- [ ] Perfil `demo` con datos ficticios; demo pública sin autenticación solo con datos de prueba.
- [ ] Sin contraseñas en el repositorio ni en su historial.
- [ ] El README indica que la demo puede estar apagada.

---

## 32. Definición de terminado

El proyecto está terminado cuando:

- La aplicación compila, arranca y `mvn test` está en verde.
- Se pueden registrar, editar y eliminar clientes desde la interfaz.
- Se puede registrar una compra y los puntos se calculan según §4.
- Se puede consultar el saldo y el historial.
- Se puede realizar un canje válido y se rechazan el canje sin saldo y el de una recompensa inactiva.
- Eliminar un cliente no deja registros huérfanos.
- Los escenarios de §29 se recorren completos en el navegador, en local y contra la URL pública.
- No hay credenciales en el repositorio ni en su historial.

---

## 33. Glosario

| Español | Inglés (código) |
|---|---|
| Cliente | `Customer` |
| Compra | `Purchase` |
| Recompensa | `Reward` |
| Canje | `Redemption` |
| Puntos / saldo de puntos | `points` |
| Monto | `amount` |
| Puntos obtenidos | `pointsEarned` |
| Puntos requeridos | `requiredPoints` |
| Puntos gastados | `pointsSpent` |
| Fecha de incorporación | `joinDate` |
| Fecha de compra o de canje | `createdAt` |
| Activa | `active` |
| Cliente no encontrado | `CustomerNotFoundException` |
| Recompensa no encontrada | `RewardNotFoundException` |
| Email duplicado | `DuplicateEmailException` |
| Recompensa inactiva | `InactiveRewardException` |
| Puntos insuficientes | `InsufficientPointsException` |

Rutas: `/customers`, `/purchases`, `/points`, `/rewards`, `/redemptions`. Tablas: `customers`, `purchases`, `rewards`, `redemptions`.

---

## 34. Versión y registro de cambios

### 1.1 — 2026-10-25

- Es el proyecto 002 del portafolio (el borrador lo numeraba 003).
- Base de datos de la demo cerrada: Neon. Se descartan la base gratuita de Render, Supabase y Aiven.
- Imágenes base del `Dockerfile` y formato de la URL JDBC cerrados.

### 1.0 — 2026-10-24

Versión inicial de los cinco documentos del proyecto, a partir del borrador original. Cambios respecto al borrador:

- Montos en USD (el borrador usaba COP): 1 punto por dólar, redondeado al más cercano, para que los precios `.99` no penalicen.
- Código y modelo de datos en inglés; el español queda solo para la interfaz.
- Se permite un `Dockerfile` para Render; Docker sigue fuera del desarrollo local.
- Datos iniciales con `CommandLineRunner` (`RewardSeeder` y `DemoDataLoader`) en lugar de `data.sql`.
- Se agrega la edición de clientes; compras y canjes quedan inmutables.
- Cascada de borrado con `@OnDelete`, bloqueo pesimista del cliente en compras y canjes, y `Clock` inyectado.
- Configuración sin `show-sql` ni caché desactivado por defecto, y locale fijo.
- Se agregan estrategia de pruebas, escenarios de demo, despliegue en Render y checklist de portabilidad.
- Sin Lombok ni DevTools.

---

**Principio del proyecto:** mantenerlo pequeño, funcional y entendible. Cualquier funcionalidad, dependencia o patrón que no contribuya directamente a la demo o al aprendizaje de Java y Spring Boot queda fuera del alcance.
