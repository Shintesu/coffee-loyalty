---
description: Ejecuta una fase del ROADMAP
---
Trabaja solo dentro de este repositorio.
Lee en este orden AGENTS.md, SPECS.md, CODESTYLE.md y ROADMAP.md, y revisa el código existente.
Ejecuta únicamente la Fase $ARGUMENTS del ROADMAP. No avances a otra fase.
Si algo del SPECS es ambiguo o contradictorio, pregunta; no asumas.
Antes de escribir código, resume qué archivos vas a crear o modificar y qué decisiones tomas, y espera mi confirmación.
No modifiques pom.xml, application.properties, application-test.properties ni ningún .md, y no agregues dependencias. No edites el ROADMAP: propón el cambio de título y lo aplico yo.
La variable DB_PASSWORD ya está definida: no la imprimas ni la escribas en ningún archivo.
Usa archivos temporales solo dentro de target/, nunca en /tmp ni fuera del repositorio.
Al terminar: ejecuta ./mvnw test y confirma que pasa; arranca con PORT=8081 ./mvnw spring-boot:run, sin setsid, y verifica las rutas de la fase con curl; DETÉN la aplicación y confirma con ss -ltnp | grep 8081 que el puerto quedó libre; borra tus temporales.
Informa qué verificaste, qué decisiones tomaste que no estaban en el SPECS y propón el mensaje de commit. No hagas commits.
