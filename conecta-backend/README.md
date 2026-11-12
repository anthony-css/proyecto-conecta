# Conecta Backend

API REST para administrar eventos comunitarios gratuitos y para que los participantes consulten el catálogo, sincronicen su perfil y se inscriban. El backend usa Java 17, Spring Boot, MySQL, Firebase Authentication, ZXing y PDFBox.

## Ejecutar en local

Usa JDK 17 o superior. Para una instalación nueva, importa [bd_conecta_demo.sql](../Base%20Datos/bd_conecta_demo.sql) una sola vez. Si ya tienes la base de demo en TiDB, reutilízala. El script no crea un administrador.

Configura las variables en **Run Configurations → Environment** de Spring Tool Suite, o en la terminal antes de ejecutar el Maven Wrapper:

```powershell
$env:DB_URL = '<URL JDBC de TiDB con TLS>'
$env:DB_USERNAME = '<usuario TiDB>'
$env:DB_PASSWORD = '<contraseña TiDB>'
$env:FIREBASE_PROJECT_ID = '<ID del proyecto Firebase>'
$env:FIREBASE_SERVICE_ACCOUNT = '<ruta al JSON privado>'
.\mvnw.cmd spring-boot:run
```

En Linux o macOS, usa `sh ./mvnw spring-boot:run` con las variables ya exportadas. La API local está en `http://localhost:8080/api`. Comprueba el arranque con `GET /actuator/health`. Hibernate valida el esquema al iniciar.

| Variable | Uso |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión y credenciales de TiDB o MySQL. Para TiDB, usa la URL JDBC con TLS de tu conexión. |
| `SERVER_PORT`, `PORT` | Puerto HTTP. Se toma primero `SERVER_PORT`, luego `PORT`, con `8080` como valor local. |
| `CORS_ALLOWED_ORIGINS` | Orígenes del panel, separados por comas. Valor local `http://localhost:4200`. En despliegue usa el origen HTTPS del portal. |
| `FIREBASE_PROJECT_ID` | ID del proyecto Firebase usado por los participantes. |
| `FIREBASE_SERVICE_ACCOUNT` | Ruta al JSON de cuenta de servicio. En el despliegue, apunta al archivo secreto del servidor. |
| `ASISTENCIA_VALIDAR_SOLO_DIA_EVENTO` | Con `true`, aplica la ventana de asistencia. Por defecto usa `false`. |

Para el flujo de participante, configura ambas variables de Firebase y usa un ID token del mismo proyecto. Firebase se inicializa al utilizar ese flujo. Las rutas públicas y el acceso administrativo usan la configuración de la base de datos.


## Crear un administrador local

Después de importar el esquema en una base MySQL local, crea una cuenta para entrar al portal. Si tu base ya tiene un administrador, puedes usar esa cuenta y omitir este paso.

1. Con Python 3, instala BCrypt y genera el hash de la contraseña que quieras usar. La contraseña se solicita de forma interactiva.

   ```powershell
   python -m pip install bcrypt
   python -c "import bcrypt, getpass; print(bcrypt.hashpw(getpass.getpass('Contrasena local: ').encode('utf-8'), bcrypt.gensalt()).decode('ascii'))"
   ```

2. En el editor SQL, selecciona la base local donde importaste el esquema. Sustituye `PEGA_AQUI_EL_HASH_BCRYPT` por el hash generado y ejecuta el bloque completo en la misma sesión. Si algún INSERT falla, detén la ejecución y ejecuta `ROLLBACK;` antes de reintentar.

   ```sql
   START TRANSACTION;

   INSERT INTO usuario (username, password, rol, activo)
   VALUES ('admin', 'PEGA_AQUI_EL_HASH_BCRYPT', 'ROLE_ADMIN', TRUE);

   INSERT INTO administrador (id_usuario, nombres, apellidos, email)
   VALUES (LAST_INSERT_ID(), 'Admin', 'Local', 'admin@example.com');

   COMMIT;
   ```

3. Arranca Spring y Angular. Entra al portal con el usuario `admin` y la contraseña que elegiste en el primer paso. Desde ahí puedes crear categorías y eventos de prueba.


## Integración del panel Angular

Usa `http://localhost:8080/api` como URL base. El login se realiza con `POST /auth/login` y el encabezado `Authorization: Basic …`. El backend devuelve el perfil administrativo. Envía el encabezado Basic en cada solicitud administrativa. Usa una cuenta administrativa provisionada en la base. En despliegue usa HTTPS.

| Módulo | Rutas relativas a `/api` |
| --- | --- |
| Categorías | `GET /categorias` para consulta pública. `GET` y `POST /admin/categorias`, y `GET`, `PUT`, `DELETE /admin/categorias/{id}` para gestión. |
| Eventos | `GET /eventos`, `GET /eventos/{id}`, `GET /eventos/destacados`. `GET` y `POST /admin/eventos`, y `GET`, `PUT`, `DELETE /admin/eventos/{id}`. |
| Estado de eventos | `PATCH /admin/eventos/{id}/publicar`, `PATCH /admin/eventos/{id}/cancelar` y `PATCH /admin/eventos/{id}/realizar`. |
| Inscritos y asistencias | `GET /admin/eventos/{id}/inscripciones`, `DELETE /admin/eventos/{id}/inscripciones/{idInscripcion}`, `GET /admin/eventos/para-asistencia`, `GET /admin/eventos/{id}/asistencias` y `DELETE /admin/eventos/{id}/asistencias/{idAsistencia}`. |
| Registro de asistencia | `POST /admin/asistencias/validar-qr` con `qr_token` e `id_evento`. `POST /admin/inscripciones/{id}/asistencia-manual` con `observacion` opcional. |
| Dashboard y reportes | `GET /admin/dashboard` y `GET /admin/reportes/eventos/{id}/asistencia.pdf`. |

- **Eventos.** Envía `application/json` al crear o editar. `imagen_url` es una dirección directa HTTP o HTTPS de hasta 255 caracteres. Usa `null` para quitar la imagen. La edición reemplaza los campos del evento y acepta `version` para detectar una edición concurrente.
- **Cierre.** Tras la hora de término en Lima, marcar como realizado convierte las confirmadas pendientes en `NO_ASISTIO`. Los eventos vencidos se excluyen del listado y detalle públicos.
- **Listados.** Usa `page` desde cero, `size` hasta 100 y `sort`, por ejemplo `fecha,desc`. Los eventos admiten `q`, `id_categoria` y `fecha`. El listado administrativo también admite `estado`. La respuesta paginada contiene `content`, `page`, `size`, `total_elements` y `total_pages`.
- **JSON.** Los campos usan `snake_case`. Las fechas siguen `yyyy-MM-dd` y las horas `HH:mm:ss`. Los errores contienen `mensaje`, `codigo`, `detalle` y `timestamp`. Usa `codigo` para identificar cada caso.
- **Archivos.** Descarga el reporte como `Blob` desde Angular. El servidor devuelve `application/pdf` y el nombre en `Content-Disposition`. El QR del participante se sirve como `image/png`.

## Flujo del participante

Las rutas `/api/participante/**` requieren `Authorization: Bearer <ID token>`. La app envía su perfil con `PUT /participante/perfil` y lo consulta con `GET` en la misma ruta. El correo debe coincidir con el token y admite hasta 100 caracteres porque se usa también como nombre de usuario en MySQL.

El participante puede reservar con `POST /participante/inscripciones` y un cuerpo con `id_evento`. Puede listar sus inscripciones, consultar una por ID, cancelarla con `DELETE /participante/inscripciones/{id}` antes del inicio y obtener su QR con `GET /participante/inscripciones/{id}/qr`. La cancelación conserva el historial y libera el cupo. La base aplica una inscripción por participante y evento.

## Verificación con Postman

Importa [Conecta.postman_collection.json](Conecta.postman_collection.json) y configura las variables de la colección. Usa `base_url` sin `/api`, completa la contraseña administrativa y, para las rutas de participante, pega un ID token vigente en `firebase_token` como valor local. Completa `participante_email` con el correo del token. Las altas guardan los IDs en la colección. Revisa esos IDs antes de ejecutar una solicitud de edición, cancelación o baja.

Un recorrido de prueba es iniciar sesión, crear una categoría y un evento, publicar el evento, sincronizar el perfil, inscribirse, abrir el QR y registrar asistencia por QR o manualmente. Para validar por QR, copia el contenido leído del PNG en `qr_contenido`. Luego consulta la asistencia y descarga el reporte. La carpeta **Errores esperados** contiene comprobaciones adicionales.

Las notificaciones FCM corresponden a la fase opcional posterior.
