# AGENTS.md

Instrucciones para agentes de IA y para quien contribuya a **Tienda de Café · App de Fidelización**. Este archivo apunta a los demás documentos en lugar de copiarlos: ante cualquier duda, el documento enlazado manda.

---

## Orden de lectura y precedencia

Leer en este orden antes de escribir código:

1. `SPECS.md`: qué se construye y qué queda fuera. Es la fuente de verdad.
2. `CODESTYLE.md`: cómo se escribe el código.
3. `ROADMAP.md`: en qué orden se construye.
4. `README.md`: vista externa del proyecto terminado.

Si dos documentos se contradicen, gana el de menor número. Si el SPECS parece incompleto o ambiguo, señalarlo y preguntar; no desviarse en silencio.

---

## Proyecto y comandos

Aplicación web monolítica en Java 21 con Spring Boot, Spring MVC, Thymeleaf, Spring Data JPA, PostgreSQL y Maven. Sin Lombok. Paquete base: `com.example.coffeeloyalty`.

```bash
./mvnw spring-boot:run
SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run
./mvnw test
```

Variables de entorno: `DB_PASSWORD` (obligatoria, sin valor por defecto), `DB_URL`, `DB_USERNAME`, `PORT` y `TEST_DB_URL` (opcionales, con valores por defecto para entorno local). `./mvnw test` necesita una base PostgreSQL local llamada `coffee_loyalty_test`; la aplicación usa `coffee_loyalty`.

---

## Reglas del proyecto

Detalle en el SPECS; estas son las que no se negocian.

- Arquitectura por capas: `Controller` → `Service` → `Repository`. La lógica de negocio vive solo en los Services. Los controllers reciben, validan y devuelven vistas.
- Los objetos de formulario se usan solo donde separan los datos de la vista de las entidades JPA (SPECS §11). No crear DTOs de respuesta.
- Regla de puntos (SPECS §4): 1 punto por cada dólar, redondeando al dólar más cercano con `setScale(0, RoundingMode.HALF_UP)`. USD 9.99 produce 10 puntos y USD 0.49 produce 0. Nunca `double` ni `float` para dinero.
- El monto de una compra es mayor que 0, como máximo 1000.00 y con hasta 2 decimales.
- El saldo de puntos solo cambia en `PurchaseService` y `RedemptionService`, dentro de una transacción y leyendo al cliente con `findByIdForUpdate` (bloqueo pesimista, SPECS §15). El saldo nunca se edita a mano.
- Un canje solo se aprueba si la recompensa está activa y el saldo alcanza. El costo se toma de la recompensa y se guarda en el canje. Si se rechaza, el saldo no cambia.
- Compras y canjes son inmutables: no se editan ni se anulan.
- El email del cliente es único sin distinguir mayúsculas: se guarda sin espacios laterales y en minúsculas. Al editar, la unicidad excluye al propio cliente.
- Eliminar un cliente elimina sus compras y canjes mediante `@OnDelete(CASCADE)` y la cascada de la base; la interfaz pide confirmación. La clave foránea hacia las recompensas no tiene cascada.
- La fecha y hora actuales salen siempre de un `Clock` inyectado (SPECS §21). Nunca `LocalDate.now()` ni `LocalDateTime.now()` en la lógica de negocio.
- Longitudes máximas de texto (SPECS §18): nombre de cliente 100, email 150, nombre de recompensa 100 y descripción de recompensa 255. La longitud de columna debe coincidir.
- El catálogo muestra solo recompensas activas. No hay CRUD de recompensas.
- Las recompensas las carga `RewardSeeder` (todo perfil menos `test`); los clientes de ejemplo, `DemoDataLoader` (perfil `demo`). Ambos son idempotentes. No usar `data.sql`.
- Las pruebas de integración usan la base PostgreSQL local `coffee_loyalty_test` con el perfil `test` (SPECS §28). Sin H2 ni Testcontainers.
- Los mensajes y textos que ve la persona usuaria están en español (SPECS §18 y §19).
- Locale fijo `en_US` y montos con separadores explícitos (SPECS §17 y §20).
- No hay autenticación ni autorización. No agregarlas.
- El esquema se gestiona con `ddl-auto=update` según el SPECS §20.
- Credenciales y secretos nunca se versionan; la configuración entra por variables de entorno.
- Sin Lombok y sin DevTools: getters, setters y constructores se escriben a mano.

---

## Alcance cerrado

No agregar nada de la lista de exclusiones del SPECS §3: autenticación, roles, panel administrativo, API pública, microservicios, Docker Compose, Kubernetes, CI/CD, infraestructura como código (`render.yaml` incluido), Redis o mensajería, CRUD de recompensas, edición o anulación de compras y canjes, pagos, correos, reportes, auditoría, promociones, reglas de puntos configurables, internacionalización ni aplicación móvil. Tampoco dependencias fuera del SPECS §26. Si algo parece necesario, preguntar primero.

**Docker, única excepción:** Render solo ejecuta Java mediante una imagen Docker (SPECS §30). Se permite un único `Dockerfile` multietapa y un `.dockerignore`, y solo en la Fase 9. No se usa Docker en el desarrollo local.

El despliegue en Render (con la base en Neon) es la Fase 9 y se configura desde el panel. En Render solo se usan datos de demo, porque la aplicación no tiene autenticación.

---

## Estilo de código

`CODESTYLE.md` aplica completo. Resumen de lo que más se rompe:

- Todo el código está en inglés: identificadores (incluidos los nombres del dominio: `Customer`, `Purchase`, `Reward`, `Redemption`), tablas, rutas, plantillas, comentarios, commits, logs y tests. El vocabulario completo está en el SPECS §33.
- El español se usa solo en lo que ve la persona usuaria: textos de la interfaz, mensajes de validación y mensajes de excepciones de negocio.
- Sin separadores decorativos, sin comentarios que repiten el código, sin código comentado.
- Commits convencionales en inglés, por ejemplo `feat(purchase): round points to the nearest dollar`.
- Logging con SLF4J, nunca `System.out`, y nunca se registran credenciales de la base de datos.

---

## Forma de trabajo

- Seguir el `ROADMAP.md` fase por fase. No empezar una fase sin cerrar la anterior.
- Cerrar una fase significa que la aplicación arranca, `./mvnw test` está en verde y los tests de esa fase existen. Después, agregar `(completada)` al título de la fase en `ROADMAP.md`.
- Cambios pequeños y enfocados: una fase, un tema por commit.
- Ante una ambigüedad entre lo pedido y el SPECS, preguntar en lugar de asumir.

---

## Definición de terminado

- [ ] Compila y `./mvnw test` pasa
- [ ] La aplicación arranca y el flujo afectado funciona en el navegador
- [ ] Respeta las reglas del proyecto y el CODESTYLE
- [ ] Sin credenciales, sin `System.out`, sin código comentado, sin Lombok
- [ ] Mensajes de commit en inglés y convencionales
- [ ] Fase marcada en `ROADMAP.md`
- [ ] `SPECS.md` y `README.md` actualizados si cambió el comportamiento
