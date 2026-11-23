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

## Frontend

```powershell
cd frontend
npm install
Copy-Item .env.example .env
npm run dev
```

El frontend lee `VITE_API_BASE_URL` desde el entorno. `.env.example` apunta al backend local. Para otro backend, configura esa variable en el entorno de Vite; no pongas claves ni contraseñas en el cliente.

Para probar localmente contra Railway mientras el backend desplegado no permita CORS, puedes configurar `VITE_API_BASE_URL=/` y `VITE_API_PROXY_TARGET=https://climaplan2-production.up.railway.app`; el proxy de Vite es solo de desarrollo.

El backend local permite por defecto el origen de Vite `http://localhost:5173`. En el backend desplegado, configura `CORS_ALLOWED_ORIGINS` con los orígenes frontend exactos separados por comas, por ejemplo `http://localhost:5173,https://tu-frontend.example.com`. No uses comodines con credenciales.
