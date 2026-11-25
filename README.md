# ClimaPlan

ClimaPlan es una aplicación para organizar planes de viaje y actividades con información del clima.

## Estructura

- `backend/`: API REST Spring Boot y wrapper Maven.
- `frontend/`: interfaz React, Vite y TypeScript.
- `docs/`: documentación de la API.

## Backend

Desde la raíz del monorepo:

```powershell
.\backend\mvnw.cmd spring-boot:run
.\backend\mvnw.cmd -DskipTests package
```

El backend local escucha en `http://localhost:8080`. La documentación OpenAPI está disponible en `/v3/api-docs`.

### Compartir planes

Los enlaces públicos usan un token generado a partir de 32 bytes de `SecureRandom`, codificados en Base64 URL-safe. Se persiste el token aleatorio en texto claro (con restricción única) para que el propietario autenticado pueda volver a consultar y copiar el enlace desde la interfaz; por tanto, el token debe tratarse como una credencial portadora. El enlace solo permite lectura del DTO público y se invalida al revocarlo o reemplazarlo.

La entidad `PlanShare` añade la tabla `plan_shares`. `spring.jpa.hibernate.ddl-auto=update` crea/actualiza esta tabla al iniciar el backend; no se requiere una migración manual para este cambio.

### Verificación de correo

El registro crea cuentas sin verificar y envía un código OTP de seis dígitos. El código se almacena como HMAC, vence en cinco minutos, permite hasta cinco intentos y solo puede usarse una vez. El reenvío tiene un cooldown de 60 segundos e invalida el código anterior.

Para enviar mediante la API HTTPS de Brevo, configura `EMAIL_PROVIDER=brevo_api`, `BREVO_API_KEY` y `MAIL_FROM` (remitente autorizado en Brevo). También se puede usar un relay SMTP con `EMAIL_PROVIDER=smtp`, `MAIL_FROM`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME` y `MAIL_PASSWORD`. `MAIL_SMTP_AUTH` y `MAIL_SMTP_STARTTLS_ENABLE` controlan autenticación y STARTTLS. No hay proveedor habilitado por defecto (`EMAIL_PROVIDER=none`); mientras no se configure, registro y reenvío responden con servicio no disponible sin exponer códigos.

Hibernate crea/actualiza la tabla `email_verification_codes` y agrega `usuarios.email_verified` como columna nullable, sin eliminar datos. Los usuarios existentes con valor `NULL` se consideran verificados; las cuentas nuevas se guardan explícitamente con `false`.

El segundo factor por correo puede activarse después de configurar el envío con `TWO_FACTOR_ENABLED=true`. Por defecto está desactivado; al habilitarlo, el login correcto envía un OTP y no emite JWT hasta verificarlo. Los challenges se almacenan en `two_factor_challenges` con hash HMAC, vencen en cinco minutos y permiten cinco intentos. Hay un cooldown de 60 segundos entre envíos para la misma cuenta.

### Chat privado por plan

El chat está disponible en `/plans/{planId}/chat` para el propietario y los participantes con invitación aceptada. El historial usa `GET /api/v1/plans/{planId}/messages` y los mensajes nuevos se envían por STOMP a `/app/plans/{planId}/chat`, con difusión en `/topic/plans/{planId}/chat`. El cliente STOMP envía el JWT existente en el encabezado `Authorization` del frame `CONNECT`; el token no se incluye en la URL.

Configura `VITE_API_BASE_URL` con el origen del backend para que el cliente conecte a `/ws` en ese mismo origen. El proxy de desarrollo actual solo proxya `/api/v1`, no WebSocket; por eso, con `VITE_API_BASE_URL=/`, la ruta `/ws` del frontend no se reenviará al backend local. No se modificó esa configuración como parte del chat. El endpoint público de planes compartidos no expone el historial ni tiene acceso al chat.

## Frontend

```powershell
cd frontend
npm install
Copy-Item .env.example .env
npm run dev
```

El frontend lee `VITE_API_BASE_URL` desde el entorno. `.env.example` apunta al backend local. Para otro backend, configura esa variable en el entorno de Vite; no pongas claves ni contraseñas en el cliente.

Para desplegar el frontend en Railway, selecciona `frontend` como directorio raíz, configura `VITE_API_BASE_URL` con el origen HTTPS del backend antes del build y usa `npm run build` para compilar y `npm start` para iniciar. El servidor usa el puerto `PORT` de Railway y sirve las rutas de React Router como SPA. Agrega el origen público del frontend a `CORS_ALLOWED_ORIGINS` en el backend.

Para probar localmente contra Railway mientras el backend desplegado no permita CORS, puedes configurar `VITE_API_BASE_URL=/` y `VITE_API_PROXY_TARGET=https://climaplan2-production.up.railway.app`; el proxy de Vite es solo de desarrollo.

El backend local permite por defecto el origen de Vite `http://localhost:5173`. En el backend desplegado, configura `CORS_ALLOWED_ORIGINS` con los orígenes frontend exactos separados por comas, por ejemplo `http://localhost:5173,https://tu-frontend.example.com`. No uses comodines con credenciales.
