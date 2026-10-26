# Roadmap de Desarrollo — Tienda de Café · App de Fidelización

> Cada fase debe cerrar con la aplicación funcionando, sus pruebas en verde y los tests de esa fase creados antes de avanzar a la siguiente. Al cerrarla, se agrega `(completada)` a su título.

## Fase 1 — Inicializar el proyecto
- Crear el proyecto con Spring Initializr en la raíz del repositorio (Java 21, Maven).
- Agregar solo estas dependencias: Spring Web, Thymeleaf, Spring Data JPA, Validation y driver de PostgreSQL. Sin Lombok ni DevTools.
- Anotar en SPECS §7 la versión de Spring Boot elegida.
- Crear la estructura de paquetes de `com.example.coffeeloyalty`: `controller`, `service`, `repository`, `entity`, `dto`, `exception`, `config`.
- Configurar `application.properties` y `application-test.properties` según el SPECS §20.
- Crear las bases `coffee_loyalty` y `coffee_loyalty_test`.
- Crear `ClockConfig` con `Clock.systemUTC()` (SPECS §21).
- Crear el fragmento de navegación (`fragments/layout.html`), `style.css`, `index.html` y `HomeController`.
- Crear `.gitignore` (sin `target/`, archivos del IDE ni credenciales).
- Verificar que la aplicación arranque y que `/` se vea.

## Fase 2 — Clientes
- Crear la entidad `Customer`, `CustomerRepository`, `CustomerForm`, `CustomerService`, `CustomerController` y las vistas `list` y `form`.
- Rutas: `GET /customers`, `GET /customers/new`, `POST /customers`, `GET /customers/{id}/edit`, `POST /customers/{id}` y `POST /customers/{id}/delete`.
- Validar nombre y email con los límites del SPECS §18 y mensajes en español.
- Guardar el email sin espacios laterales y en minúsculas; rechazar duplicados sin distinguir mayúsculas, también al editar (excluyendo al propio cliente) y ante una condición de carrera en la base.
- Fijar `joinDate` con el `Clock` y `points = 0` al crear.
- Botón de eliminar con confirmación.
- Crear `GlobalExceptionHandler`, `CustomerNotFoundException`, `DuplicateEmailException` y la página `error.html`.
- Tests unitarios de `CustomerService` con `Clock.fixed`: alta, email duplicado con distinto uso de mayúsculas, edición y cliente inexistente.

## Fase 3 — Compras y puntos
- Crear la entidad `Purchase` con `@OnDelete(CASCADE)` hacia el cliente, `PurchaseRepository`, `PurchaseForm`, `PurchaseService` y `PurchaseController`, con las vistas `form` y `list`.
- Agregar `CustomerRepository.findByIdForUpdate` con bloqueo pesimista de escritura (SPECS §15).
- Calcular los puntos con `setScale(0, RoundingMode.HALF_UP)` según el SPECS §4, dentro de una transacción que guarda la compra y actualiza el saldo.
- Rutas: `GET /purchases`, `GET /purchases/new` y `POST /purchases`.
- Crear `PointsController` y las vistas `points/list` y `points/detail` (por ahora con el historial de compras).
- Redirigir a `/points/{customerId}` con un mensaje que incluya los puntos obtenidos.
- Verificar con `\d purchases` que la clave foránea tenga `ON DELETE CASCADE`.
- Tests: cálculo de puntos en los bordes (0.49, 0.50, 9.49, 9.99, 10.00, 25.99, 1000.00), cliente inexistente, y un test de integración que verifica que eliminar un cliente elimina sus compras.

## Fase 4 — Recompensas
- Crear la entidad `Reward`, `RewardRepository`, `RewardService`, `RewardController` y la vista `rewards/list`, con el botón **Canjear** que enlaza a `/redemptions/new?rewardId={id}`.
- Crear `RewardSeeder` (perfil `!test`, idempotente) con las cuatro recompensas del SPECS §22.
- El catálogo muestra solo las recompensas activas.
- Tests: listado de activas e inactivas, y la idempotencia del seeder.

## Fase 5 — Canjes
- Crear la entidad `Redemption` con `@OnDelete(CASCADE)` hacia el cliente y sin cascada hacia la recompensa, `RedemptionRepository`, `RedemptionForm`, `RedemptionService` y `RedemptionController`, con las vistas `form` y `list`.
- Validar recompensa activa y saldo suficiente; tomar el costo de la recompensa y guardarlo en el canje; todo en una transacción con el cliente bloqueado.
- Crear `InactiveRewardException`, `InsufficientPointsException` y `RewardNotFoundException`, con mensajes en español que nombren al cliente y a la recompensa.
- Mostrar los errores de negocio dentro del formulario, conservando la selección.
- Mostrar en cada opción el saldo del cliente y el costo de la recompensa.
- Agregar el historial de canjes a `points/detail`.
- Rutas: `GET /redemptions`, `GET /redemptions/new` (con `rewardId` opcional) y `POST /redemptions`.
- Verificar con `\d redemptions` la cascada hacia el cliente y la ausencia de cascada hacia la recompensa.
- Tests: canje válido, saldo insuficiente sin cambios, recompensa inactiva, reversión de la transacción, cascada al eliminar un cliente y dos canjes simultáneos que superan el saldo.

## Fase 6 — Datos de demo
- Crear `DemoDataLoader` (perfil `demo`, después de `RewardSeeder`) con tres clientes ficticios creados a través de los Services: uno con saldo suficiente para canjear, uno con saldo bajo y uno sin movimientos.
- Cargar solo si la tabla `customers` está vacía.
- No incluir un cliente llamado Sebastián (el escenario A del SPECS §29 lo registra).
- Preparar los tres escenarios del SPECS §29.

## Fase 7 — Pruebas y pulido
- Completar los tests prioritarios del SPECS §28 que falten.
- Verificar la coherencia del saldo: puntos de las compras menos puntos de los canjes.
- Revisar validaciones, mensajes en español y navegación entre pantallas.
- Mejorar la presentación visual sin sobrecargar el proyecto.
- Revisar que no haya `@Data`, Lombok, `System.out`, `LocalDate.now()` ni `LocalDateTime.now()` en la lógica.

## Fase 8 — Documentación y preparación del portafolio
- Completar el README con capturas y GIF de los escenarios de demo (en `docs/`).
- Verificar que no haya credenciales en el repositorio ni en su historial.
- Revisar la consistencia entre `SPECS.md` y la implementación final.
- Preparar la explicación de las decisiones técnicas para entrevistas: redondeo de puntos con `HALF_UP`, transacciones, bloqueo pesimista del saldo, cascada con `@OnDelete`, `Clock` inyectado y por qué Java necesita Docker en Render.

## Fase 9 — Despliegue en Render
- Desplegar solo cuando el proyecto esté terminado y listo para mostrarse.
- Releer la documentación vigente de Render y de Neon (plan gratuito y límites) antes de crear recursos.
- Crear el `Dockerfile` multietapa (`maven:3.9-eclipse-temurin-21` para compilar, `eclipse-temurin:21-jre` para ejecutar; `mvn package -DskipTests`) y un `.dockerignore` con `target`, `.git`, `.idea` y `*.iml`.
- Crear la base en Neon, en una región cercana a la del servicio de Render, y copiar desde su panel el host, el nombre de la base, el usuario y la contraseña.
- Armar la URL JDBC (`jdbc:postgresql://HOST/BASE?sslmode=require`) con el usuario y la contraseña en variables aparte: la cadena del panel no es JDBC.
- Crear el Web Service con runtime Docker desde el repositorio, configurado desde el panel (sin `render.yaml`).
- Variables en el panel: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SPRING_PROFILES_ACTIVE=demo` y `JAVA_TOOL_OPTIONS` con el heap limitado.
- En el primer arranque, `ddl-auto=update` crea el esquema y los loaders cargan los datos.
- Medir el arranque en frío y confirmar que cabe en 512 MB; si no, evaluar el plan de pago de Render.
- La aplicación no tiene autenticación: usar solo datos de demo.
- Recorrer los escenarios de demo contra la URL pública.
- Documentar el despliegue y el enlace de la demo en el README, sin secretos y avisando que puede estar apagada.

---

**Principios a mantener durante todo el desarrollo:**
- Simplicidad y buenas prácticas por encima de tecnología innecesaria.
- Lógica de negocio únicamente en los Services.
- El saldo solo cambia en `PurchaseService` y `RedemptionService`, dentro de una transacción.
- Dinero con `BigDecimal`, nunca con `double`.
- Fecha y hora siempre desde el `Clock` inyectado.
- Credenciales fuera del repositorio.
- Docker solo en la Fase 9; despliegue solo al final y solo con datos de demo.
