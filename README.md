# CONECTA

Sistema de gestión de eventos comunitarios gratuitos. El panel permite administrar eventos, inscripciones y asistencia. La API también expone el catálogo y las operaciones de participantes previstas para la app iOS.

- [Backend Spring Boot](conecta-backend/README.md)
- [Panel Angular](conecta-frontend/README.md)
- [App iOS](conecta-ios/README.md)
- [Esquema SQL de demo](Base%20Datos/bd_conecta_demo.sql)

## Inicio local

Configura Spring con las variables de entorno de la guía del backend. Si ya importaste el esquema en TiDB, usa la base existente. El script SQL es para instalar el esquema una sola vez en una base nueva. El administrador se provisiona por separado.

Arranca el backend y luego el panel Angular. En desarrollo, el panel consume `http://localhost:8080/api`.

## Despliegue

El backend se desplegará desde `conecta-backend` y el portal desde `conecta-frontend`. Configura TiDB y Firebase en el entorno del backend y la URL pública de la API en la compilación del portal. El despliegue y el proyecto Xcode se realizan por separado.
