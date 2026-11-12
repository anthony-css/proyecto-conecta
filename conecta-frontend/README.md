# CONECTA Panel administrativo

Portal Angular para organizar eventos comunitarios gratuitos y registrar la asistencia de sus participantes. Consume la API de Spring Boot y MySQL que comparte con la app iOS.

## Funcionalidades

- **Agenda** con próximos eventos publicados o completos, ocupación y tres indicadores.
- **Eventos** con búsqueda, filtros, paginación y acciones por estado. La ficha reúne Información e Inscritos.
- **Categorías**, accesible desde Eventos, con nombre, descripción y estado.
- **Asistencia** con QR individual y búsqueda manual por nombre o código, historial persistente por evento y corrección de marcaciones.
- **Reportes** con gráfico por estado de inscripción y descarga de PDF y CSV. Los eventos realizados siguen disponibles.
- **Inscritos** en la ficha del evento, con datos de contacto, búsqueda y cancelación administrativa antes del inicio.

El portal usa Angular 21, TypeScript, Signals, RxJS, formularios reactivos, Tailwind CSS, iconos Lucide y ZXing. La identidad visual combina carbón, naranja quemado y dorado. La navegación tiene cuatro destinos en escritorio y un menú desplegable en móvil. Inter se carga desde los archivos del portal. Los estados llevan texto y color. El acceso permite mostrar u ocultar la contraseña y el cierre de sesión se confirma.

## Ejecutar en local

1. Inicia Spring Boot con TiDB o MySQL siguiendo la [guía del backend](../conecta-backend/README.md).
2. Comprueba que `http://localhost:8080/actuator/health` responde.
3. Abre una terminal en `conecta-frontend` y ejecuta:

```powershell
npm.cmd ci
npm.cmd start
```

4. Abre `http://localhost:4200` e ingresa con tu cuenta administrativa.
5. Para generar la versión de producción, ejecuta `npm.cmd run build`. Los archivos quedan en `dist/conecta-frontend/browser`.

En desarrollo, la API apunta a `http://localhost:8080/api`. En Cloudflare Pages, define `API_URL` con la URL pública de Render, incluido `/api` (por ejemplo, `https://tu-backend.onrender.com/api`), y usa `npm run build`. Configura `CORS_ALLOWED_ORIGINS` en el backend con el origen HTTPS del portal.

## Datos y comportamiento

El acceso administrativo usa HTTP Basic y conserva la sesión en la pestaña. Las rutas privadas requieren el rol administrativo y un error 401 devuelve al acceso.

Las imágenes de eventos se guardan como URL absoluta HTTP o HTTPS de hasta 255 caracteres. Quitar una imagen envía `null`. Los formularios simplificados conservan las coordenadas del evento y el color e icono de categorías que ya existían. La versión del evento permite detectar una edición concurrente.

Inscritos exporta las filas visibles a CSV. El CSV de Reportes incluye todas las filas del resumen. Reportes calcula el gráfico desde las inscripciones reales del evento y permite revisar también su historial de cancelaciones. Un evento sin inscripciones muestra un estado vacío y mantiene deshabilitada la descarga. El PDF conserva el nombre `asistencia-evento-{id}.pdf`.

## Recorrido de demostración

1. Desde Eventos, abre Categorías y crea una categoría activa.
2. Crea un evento con fecha futura. Guárdalo como borrador y publícalo desde su menú de acciones.
3. Confirma que aparece en la Agenda y en el catálogo público.
4. Usa un evento con inscripciones confirmadas para probar filtros, copia de código y exportación CSV.
5. Abre Asistencia con ese evento. Escanea un pase QR o pega su contenido. En Registro manual, busca otra inscripción confirmada y confirma su asistencia.
6. Repite una validación para comprobar el aviso de duplicado.
7. Recarga Asistencia, verifica el historial y deshaz una marcación de prueba.
8. Usa un evento cuya hora de término ya pasó para marcarlo como realizado. Revisa las ausencias y compara los estados y totales en Reportes, PDF y CSV.

La cámara requiere localhost o HTTPS y permiso del navegador. El registro manual queda disponible si la cámara no puede usarse. La ventana de fecha y horario de asistencia depende de la configuración del backend.

## Validación de cierre

El build de producción se comprobó en Angular. En Edge se verificaron los controles de acceso y salida, la navegación a 360, 768 y 1440 px y la descarga CSV con datos simulados. En una instancia aislada de MySQL se comprobó el recorrido de API con autenticación administrativa, reservas, cancelaciones, lectura del QR, registro manual, corrección, cierre y PDF. La verificación de tokens Firebase se simuló en esa prueba.

El ensayo presencial debe comprobar la cámara física del equipo de la demo. La autenticación con un ID token real y la conexión desde Xcode se comprueban durante la integración de iOS.

## Integración con iOS

La app consume el mismo catálogo mediante `GET /eventos`, `GET /eventos/{id}`, `GET /eventos/destacados` y `GET /categorias`, relativos a la URL base terminada en `/api`. El listado pagina desde cero y devuelve `content` y `total_pages`.

Configura `ConectaAPIBaseURL` según dónde corre Spring Boot. Si está en la misma Mac que el simulador, usa `http://localhost:8080/api`. Si está en otro equipo, usa su dirección de red accesible desde la Mac. Comprueba esa conexión desde el laboratorio y revisa la configuración de transporte local al crear el [proyecto iOS](../conecta-ios/README.md).

Las rutas `/participante/**` requieren un ID token de Firebase. La app solicita el PNG del pase en `GET /participante/inscripciones/{id}/qr` y lo presenta al lector del portal. El recorrido con Xcode, Firebase y un dispositivo o simulador iOS se verifica en el laboratorio.
