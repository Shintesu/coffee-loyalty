# Tienda de Café · App de Fidelización

Aplicación web para una tienda de café donde los clientes acumulan puntos con sus compras y los canjean por recompensas. Es una demo de portafolio construida con Java 21, Spring Boot y PostgreSQL, con interfaz renderizada en el servidor con Thymeleaf.

> **Demo en línea:** _pendiente (se publica en la Fase 9 del roadmap)_. La demo corre en el plan gratuito de Render con la base en Neon, por lo que puede estar apagada y tardar en despertar. No tiene autenticación: usa solo datos ficticios.

## Capturas y demo

_Pendiente (Fase 8). Los archivos viven en `docs/`._

| Pantalla | Archivo |
|---|---|
| Detalle de puntos con historial | `docs/points.png` |
| Catálogo de recompensas | `docs/rewards.png` |
| Canje rechazado por saldo insuficiente | `docs/redemption-rejected.png` |
| Recorrido completo (GIF) | `docs/demo.gif` |

## Funcionalidades

- Alta, edición y eliminación de clientes.
- Registro de compras con cálculo automático de puntos.
- Consulta del saldo y del historial de compras y canjes de cada cliente.
- Catálogo de recompensas activas.
- Canje de recompensas con validación de saldo.
- Historial general de compras y de canjes.

## Regla de puntos

**1 punto por cada dólar, redondeando al dólar más cercano.** Así, los precios terminados en `.99` no penalizan al cliente.

| Monto | Puntos |
|---:|---:|
| $0.49 | 0 |
| $9.49 | 9 |
| $9.99 | 10 |
| $25.99 | 26 |
| $50.00 | 50 |

## Stack

- Java 21
- Spring Boot, Spring MVC, Spring Data JPA
- PostgreSQL (Neon en la demo pública)
- Thymeleaf y HTML/CSS
- Maven
- JUnit 6, AssertJ y Mockito
- Docker, solo para desplegar en Render

Sin Lombok: getters, setters y constructores están escritos a mano.

## Arquitectura

```text
Controller → Service → Repository → PostgreSQL
```

- **Controller:** recibe peticiones, valida formularios y elige la vista.
- **Service:** contiene las reglas de negocio y las transacciones.
- **Repository:** acceso a datos con Spring Data JPA.

## Modelo de datos

```text
customers 1 ─── N purchases
customers 1 ─── N redemptions
rewards   1 ─── N redemptions
```

Eliminar un cliente elimina sus compras y canjes (`ON DELETE CASCADE`). Las recompensas nunca se eliminan. Detalle de tablas en [SPECS.md](SPECS.md) §8.

## Reglas de negocio

- El email del cliente es único sin distinguir mayúsculas.
- El monto de una compra va de $0.01 a $1,000.00 y admite hasta 2 decimales.
- Un canje exige una recompensa activa y saldo suficiente; si se rechaza, el saldo no cambia.
- El saldo y el movimiento se guardan en una sola transacción, con el cliente bloqueado para evitar canjes simultáneos que lo dejen en negativo.
- Compras y canjes son inmutables.

## Rutas

| Sección | Rutas |
|---|---|
| Inicio | `GET /` |
| Clientes | `GET /customers`, `GET /customers/new`, `POST /customers`, `GET /customers/{id}/edit`, `POST /customers/{id}`, `POST /customers/{id}/delete` |
| Compras | `GET /purchases`, `GET /purchases/new`, `POST /purchases` |
| Puntos | `GET /points`, `GET /points/{customerId}` |
| Recompensas | `GET /rewards` |
| Canjes | `GET /redemptions`, `GET /redemptions/new`, `POST /redemptions` |

## Cómo ejecutarlo

Requisitos: Java 21 y PostgreSQL. El proyecto incluye Maven Wrapper, así que no hace falta instalar Maven.

1. Define la contraseña de la base como variable de entorno (nunca se guarda en el repositorio):

   ```bash
   export DB_PASSWORD='tu-contraseña'
   ```

2. Crea las bases de datos:

   ```bash
   PGPASSWORD="$DB_PASSWORD" psql -h localhost -U postgres -c "CREATE DATABASE coffee_loyalty;"
   PGPASSWORD="$DB_PASSWORD" psql -h localhost -U postgres -c "CREATE DATABASE coffee_loyalty_test;"
   ```

3. Arranca la aplicación con datos de demo:

   ```bash
   SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run
   ```

4. Abre `http://localhost:8080`.

Sin el perfil `demo` la aplicación arranca con las recompensas y sin clientes.

### Variables de entorno

| Variable | Obligatoria | Por defecto |
|---|---|---|
| `DB_PASSWORD` | Sí | ninguno |
| `DB_URL` | No | `jdbc:postgresql://localhost:5432/coffee_loyalty` |
| `DB_USERNAME` | No | `postgres` |
| `PORT` | No | `8080` |
| `TEST_DB_URL` | No | `jdbc:postgresql://localhost:5432/coffee_loyalty_test` |

Las fechas se registran y se muestran en UTC.

## Pruebas

```bash
./mvnw test
```

Necesita la base `coffee_loyalty_test` creada. Las pruebas unitarias de los Services usan `Clock.fixed` y Mockito; las de integración corren contra PostgreSQL local, sin H2 ni Testcontainers.

## Escenarios de demo

1. **Compra, acumulación y canje:** registrar un cliente, comprar $50.00 (50 puntos), canjear una recompensa de 30 puntos (quedan 20) e intentar una de 50, que se rechaza.
2. **Precio terminado en .99:** una compra de $9.99 genera 10 puntos.
3. **Corregir y eliminar:** editar un email y eliminar un cliente con movimientos, con confirmación.

El guion completo está en [SPECS.md](SPECS.md) §29.

## Decisiones técnicas

- **Redondeo de puntos con `HALF_UP` y `BigDecimal`:** el dinero nunca usa `double`, y los precios `.99` no restan un punto.
- **Bloqueo pesimista del cliente:** compras y canjes leen el saldo con `SELECT ... FOR UPDATE`, así que dos canjes simultáneos se ejecutan en serie.
- **Transacciones:** el movimiento y el saldo se guardan juntos o no se guarda nada.
- **Cascada con `@OnDelete`:** el borrado en cascada lo hace la base de datos, y se verifica en el esquema generado.
- **`Clock` inyectado:** la fecha y hora salen de un bean, lo que permite probar con fechas fijas.
- **Datos iniciales con `CommandLineRunner`:** idempotentes y creados por los Services, para que los saldos sean coherentes con los movimientos.
- **Java en Render requiere Docker:** Render ejecuta Java solo mediante una imagen Docker, así que el proyecto incluye un único `Dockerfile` usado solo para desplegar.
- **Neon para la base de la demo:** su plan gratuito no vence y se reactiva solo tras suspenderse, a diferencia de la base gratuita de Render, que expira a los 30 días.

## Alcance excluido

Autenticación, roles, API pública, microservicios, Docker Compose, Kubernetes, CI/CD, CRUD de recompensas, edición o anulación de compras y canjes, pagos, correos, reportes, promociones, internacionalización y aplicación móvil. Lista completa en [SPECS.md](SPECS.md) §3.

## Despliegue

_Pendiente (Fase 9)._ Se desplegará en Render con una imagen Docker y la base de datos en Neon. Como la aplicación no tiene autenticación, la demo usa solo datos ficticios. Ver [SPECS.md](SPECS.md) §30.

## Documentación del proyecto

| Archivo | Contenido |
|---|---|
| [SPECS.md](SPECS.md) | Qué se construye: alcance, reglas, modelo, rutas y criterios |
| [CODESTYLE.md](CODESTYLE.md) | Cómo se escribe el código |
| [ROADMAP.md](ROADMAP.md) | En qué orden se construye |
| [AGENTS.md](AGENTS.md) | Instrucciones para agentes de IA y colaboradores |
