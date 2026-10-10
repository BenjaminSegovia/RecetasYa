# RecetaYA — Resumen del proyecto

> Documento de orientación para el equipo. Explica qué es el proyecto, cómo está armado,
> cómo fluye una receta de principio a fin y cómo levantarlo.

---

## 1. ¿Qué es RecetaYA?

**RecetaYA** es una plataforma de gestión de recetas médicas para farmacias, construida con
**arquitectura de microservicios**. Permite que:

- Un **médico** registre una receta con los medicamentos y cantidades que receta.
- El sistema **reserve automáticamente el stock** en la sucursal indicada (o detecte falta de stock).
- Un **farmacéutico** vea las recetas con stock reservado y **dispense** los medicamentos.

El objetivo principal es **digitalizar y desacoplar** el proceso de recetar → reservar stock → dispensar,
para que cada parte del sistema evolucione de forma independiente.

---

## 2. Stack tecnológico

| Capa | Tecnología |

|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4 (Spring Web MVC, Spring Security, Spring Data JPA) |
| Autenticación | JWT (JSON Web Tokens) con roles, validado en cada servicio |
| Enrutamiento | Spring Cloud Gateway (`api-gateway`, punto de entrada único) |
| Bases de datos | PostgreSQL 16 (una por servicio) |
| Comunicación asíncrona | AWS SQS (emulado localmente con **LocalStack**): `reserva-stock-queue` y `notificacion-queue` |
| Comunicación sincrona | REST (RestClient entre servicios) |
| Notificaciones | JavaMailSender + **Mailpit** en local (SMTP de desarrollo) |
| Contenedores | Docker + Docker Build |
| Orquestación | Docker Compose |
| Build | Maven (wrapper `mvnw`) |
| Mapeo | Lombok |

---

## 3. Arquitectura: los 5 servicios + API Gateway

```                       ┌─────────────────────┐
                          │    auth-service     │  :8081
                          │  Login/Register     │
                          │   emite JWT         │
                          └──────────┬──────────┘
                                    token JWT (roles MEDICO / FARMACEUTICO)
                                      ▼
┌──────────────┐  REST   ┌─────────────────────┐  REST   ┌─────────────────────┐
│   receta-    │◄────────│  inventory-service  │◄────────│  dispensing-service │
│   service    │         │      :8083          │         │       :8084         │
│    :8082     │         └──────────┬──────────┘         └─────────────────────┘
└──────┬───────┘                    │
       │ publica mensaje            │ escucha y publica resultado
       ▼                            ▼
   ┌─────────────────────────────────────────────┐
   │            SQS (LocalStack) :4566           │
   │  cola: reserva-stock-queue                  │
   │  cola: notificacion-queue  ──────────────┐  │
   └──────────────────────────────────────────┼──┘
                                              ▼
                              ┌─────────────────────────┐
                              │  notification-service   │  :8085
                              │  consume notificacion-  │
                              │  queue y envía email    │──► Mailpit :1025 / :8025
                              └─────────────────────────┘

   ┌─────────────────────────┐
   │      api-gateway        │  :8080  ← punto de entrada único
   │  /api/<servicio>/**     │     para los clientes
   └─────────────────────────┘
```

### Tabla de servicios

| Servicio | Puerto | Base de datos | Responsabilidad |

|---|---|---|---|
| `api-gateway` | 8080 | — | **Punto de entrada único**: enruta `/api/<servicio>/**` a cada servicio |
| `auth-service` | 8081 | `auth_db` (5433) | Registro/login de usuarios, emisión y validación de JWT |
| `receta-service` | 8082 | `receta_db` (5434) | CRUD de recetas, publica eventos de reserva de stock |
| `inventory-service` | 8083 | `inventory_db` (5435) | Stock por sucursal, reserva de stock al recibir el mensaje SQS |
| `dispensing-service` | 8084 | `dispensing_db` (5436) | Registra la entrega (dispensación) de recetas |
| `notification-service` | 8085 | — | Consume `notificacion-queue` y avisa por email al médico (RESERVADA / SIN_STOCK) |

Infraestructura auxiliar:

- **4 bases PostgreSQL** (una por servicio → *database per service*).
- **LocalStack** (puerto 4566): emula AWS SQS en local para no depender de una cuenta AWS. Las dos colas se crean automáticamente al arrancar.
- **Mailpit** (SMTP 1025 / web 8025): captura los emails de `notification-service` en local, sin enviar nada a internet.

---

## 4. Roles y permisos

Dos roles, definidos en `auth-service` (`Role.java`) y aplicados en el `SecurityConfig` de cada servicio:

| Rol | Puede |

|---|---|
| **MEDICO** | Crear recetas (`POST /recetas`), ver recetas por id |
| **FARMACEUTICO** | Ver recetas reservadas (`GET /recetas/reservadas`), dispensar (`POST /dispensaciones`) |
| Cualquiera autenticado | Consultar/CRUD de inventario (`/inventario/**`) |

> Todas las rutas exigen `Authorization: Bearer <token>` (excepto `/auth/**` y `/actuator/**`).
> El endpoint `PATCH /recetas/{id}/estado` está **abierto** porque lo usan los servicios entre sí
> (es un punto a endurecer: debería autenticarse con un token de servicio).

---

## 5. Estados de una receta

```ACEPTADA_PENDIENTE_RESERVA ──► RESERVADA ──► DISPENSADA
           │
           └──────► SIN_STOCK
```

| Estado | Significado | Quién lo cambia |

|---|---|---|
| `ACEPTADA_PENDIENTE_RESERVA` | Receta creada, aún sin verificar stock | `receta-service` al crear |
| `RESERVADA` | Hay stock suficiente y ya fue descontado | `inventory-service` |
| `SIN_STOCK` | No había stock suficiente en la sucursal | `inventory-service` |
| `DISPENSADA` | El farmacéutico entregó los medicamentos | `dispensing-service` |

---

## 6. Diagrama de flujo del proceso principal

```mermaid
flowchart TD
    Start([Inicio]) --> A[Usuario registra cuenta<br/>POST /auth/register]
    A --> B{¿Usuario ya existe?}
    B -- Sí --> A2[Error: username en uso] --> A
    B -- No --> C[Se guarda hash de contraseña<br/>y se emite JWT con rol]
    C --> D[Usuario inicia sesión<br/>POST /auth/login]
    D --> E{¿Credenciales<br/>válidas?}
    E -- No --> E2[401: usuario o contraseña<br/>incorrectos] --> D
    E -- Sí --> F[Se retorna JWT<br/>rol MEDICO o FARMACEUTICO]

    F --> G[MÉDICO: crea la receta<br/>POST /recetas<br/>paciente, sucursal, medicamentos]
    G --> H[receta-service guarda la receta<br/>con estado ACEPTADA_PENDIENTE_RESERVA]
    H --> I[Publica mensaje en SQS<br/>cola reserva-stock-queue]
    I --> J([Respuesta inmediata 201<br/>médico no queda bloqueado])

    J --> K[inventory-service recibe<br/>el mensaje de la cola]
    K --> L{¿Hay stock suficiente<br/>de TODOS los medicamentos<br/>en la sucursal?}

    L -- Sí --> M[Descuenta el stock<br/>de forma transaccional]
    M --> N[PATCH /recetas/id/estado<br/>→ RESERVADA]
    L -- No --> O[PATCH /recetas/id/estado<br/>→ SIN_STOCK]
    O --> P([Receta no puede dispensarse])

    N --> NF[Publica en SQS<br/>cola notificacion-queue]
    O --> NF
    NF --> NG[notification-service recibe<br/>el resultado de la reserva]
    NG --> NH[Envía email al médico<br/>con el estado de su receta]
    NH --> NI([Email visible en Mailpit<br/>http://localhost:8025])

    N --> Q[FARMACEUTICO consulta<br/>GET /recetas/reservadas]
    Q --> R[FARMACEUTICO dispensa<br/>POST /dispensaciones]
    R --> S{¿Ya existe una<br/>dispensación?}
    S -- Sí --> S2[Error: dispensación ya existe] --> Q
    S -- No --> T[dispensing-service llama a<br/>receta-service GET /recetas/id]
    T --> U{¿Estado = RESERVADA?}
    U -- No --> U2[Error: receta no reservada] --> Q
    U -- Sí --> V[Registra la dispensación<br/>con fecha y farmacéutico]
    V --> W[PATCH /recetas/id/estado<br/>→ DISPENSADA]
    W --> End([Fin: receta entregada])
```

### Diagrama de secuencia (vista técnica)

```mermaid
sequenceDiagram
    actor M as Médico
    participant A as auth-service<br/>:8081
    participant R as receta-service<br/>:8082
    participant Q as SQS<br/>(LocalStack)
    participant I as inventory-service<br/>:8083
    participant N as notification-service<br/>:8085
    actor F as Farmacéutico
    participant D as dispensing-service<br/>:8084

    M->>A: POST /auth/register (con email) | /auth/login
    A-->>M: JWT (rol MEDICO)

    M->>R: POST /recetas (Bearer JWT)
    R->>R: Guarda receta = ACEPTADA_PENDIENTE_RESERVA
    R->>Q: publica ReservaStockMessage en reserva-stock-queue
    R-->>M: 201 Created (respuesta inmediata)

    Q->>I: entrega el mensaje
    I->>I: ¿hayStockSuficiente(sucursal, meds)?
    alt Stock suficiente
        I->>I: reservarStock() [descuenta, @Transactional]
        I->>R: PATCH /recetas/{id}/estado → RESERVADA
    else Sin stock
        I->>R: PATCH /recetas/{id}/estado → SIN_STOCK
    end

    I->>Q: publica NotificacionRecetaMensaje en notificacion-queue
    Q->>N: entrega el mensaje
    N->>A: GET /internal/usuarios/{medicoUsername}
    A-->>N: email del médico
    N->>N: envía email con el estado (RESERVADA / SIN_STOCK)
    Note over N: En local el email se captura en Mailpit :8025

    F->>A: POST /auth/login
    A-->>F: JWT (rol FARMACEUTICO)
    F->>R: GET /recetas/reservadas
    R-->>F: lista de recetas RESERVADA

    F->>D: POST /dispensaciones {recetaId}
    D->>R: GET /recetas/{id} (Bearer JWT de F)
    R-->>D: receta con estado RESERVADA
    D->>D: valida y guarda la dispensación
    D->>R: PATCH /recetas/{id}/estado → DISPENSADA
    D-->>F: 201 Created
```

---

## 7. APIs principales

### api-gateway — `:8080` (punto de entrada único)

No expone lógica propia: solo enruta `/api/<servicio>/**` al servicio correspondiente,
quitando el prefijo con `StripPrefix=2` y reenviando el header `Authorization` tal cual
(cada resource server sigue validando su propio JWT).

| Prefijo | Se enruta a |
|---|---|
| `/api/auth/**` | `auth-service` (`:8081`) |
| `/api/recetas/**` | `receta-service` (`:8082`) |
| `/api/inventario/**` | `inventory-service` (`:8083`) |
| `/api/dispensaciones/**` | `dispensing-service` (`:8084`) |
| `/api/notificaciones/**` | `notification-service` (`:8085`) |

### auth-service — `:8081`

| Método | Ruta | Descripción |

|---|---|---|
| POST | `/auth/register` | Crea usuario (username, password, nombreCompleto, email, role) y retorna JWT |
| POST | `/auth/login` | Valida credenciales y retorna JWT |
| GET | `/internal/usuarios/{username}` | interno | Entrega username, nombreCompleto y email (lo usa notification-service) |

### receta-service — `:8082`

| Método | Ruta | Rol | Descripción |

|---|---|---|---|
| POST | `/recetas` | MEDICO | Crea receta y dispara la reserva de stock |
| GET | `/recetas/reservadas` | FARMACEUTICO | Lista recetas con stock reservado |
| GET | `/recetas/{id}` | MEDICO / FARMACEUTICO | Detalle de receta |
| PATCH | `/recetas/{id}/estado` | interno | Cambia el estado (lo usan inventory y dispensing) |

### inventory-service — `:8083`

| Método | Ruta | Rol | Descripción |

|---|---|---|---|
| POST | `/inventario` | autenticado | Crea o actualiza stock (medicamento + sucursal) |
| GET | `/inventario` | autenticado | Lista todo el stock |
| GET | `/inventario/{id}` | autenticado | Stock por id |
| PUT | `/inventario/{id}` | autenticado | Actualiza stock |
| DELETE | `/inventario/{id}` | autenticado | Elimina stock |
| — | listener SQS | — | Escucha `reserva-stock-queue` y reserva/amarra stock |

### dispensing-service — `:8084`

| Método | Ruta | Rol | Descripción |

|---|---|---|---|
| POST | `/dispensaciones` | FARMACEUTICO | Dispensa una receta (debe estar RESERVADA) |

### notification-service — `:8085`

No tiene endpoints propios para los usuarios: es un worker sin base de datos.

| Componente | Descripción |
|---|---|
| `NotificacionRecetaListener` | Escucha `notificacion-queue` (SQS) y dispara la notificación |
| `NotificacionService` | Busca el email del médico en `auth-service` y le envía un correo con el estado |
| `AuthClient` | Cliente REST que llama a `GET /internal/usuarios/{username}` de `auth-service` |

Canales de salida:

- **En local**: SMTP apunta a **Mailpit** (`:1025`); los emails se ven en
  `http://localhost:8025` sin salir a internet.
- **En producción**: configurar `MAIL_HOST` / `MAIL_PORT` (y credenciales) hacia un
  SMTP real.

---

## 8. Cómo levantar el proyecto

### Prerrequisitos

- Docker Desktop instalado y corriendo
- Git
- Una cuenta gratuita en https://app.localstack.cloud (necesaria para obtener un
  Auth Token, ya que LocalStack lo exige incluso en su plan gratuito)

### 1. Clonar el repositorio

```bash
git clone https://github.com/BenjaminSegovia/RecetasYa.git
cd RecetasYa
```

### 2. Configurar el archivo `.env`

Crea un archivo `.env` en la raíz del proyecto (junto a `docker-compose.yml`).
Para no partir de cero, copia la plantilla incluida en el repo:

```bash
cp .env.example .env
```

Luego edita `.env` y rellena tus valores reales:

```bash
LOCALSTACK_AUTH_TOKEN=tu_token_aqui
POSTGRES_USER=recetaya
POSTGRES_PASSWORD=tu_password_aqui
JWT_SECRET=tu_secreto_largo_y_aleatorio_aqui
```

> Este archivo está en `.gitignore` — cada persona que clone el proyecto
> necesita generar sus propios valores (el token de LocalStack es gratuito
> en app.localstack.cloud). Docker Compose lo lee automáticamente al hacer
> `docker compose up`.
> **Ningún secreto está hardcodeado** ni en el `docker-compose.yml` ni en los
> `application.yaml`: todo viene de estas variables de entorno.

### 3. Levantar todo

```bash
# 1. Levanta todo (bases de datos, LocalStack, Mailpit y los 5 servicios + gateway)
docker compose up --build

# 2. Verificar (los clientes solo necesitan el gateway)
# api-gateway          http://localhost:8080   ← punto de entrada único
#
# Servicios internos (por si necesitas probarlos directo):
# auth-service         http://localhost:8081
# receta-service       http://localhost:8082
# inventory-service    http://localhost:8083
# dispensing-service   http://localhost:8084
# notification-service http://localhost:8085
# LocalStack SQS       http://localhost:4566
# Mailpit (emails)     http://localhost:8025
```

Las dos colas SQS (`reserva-stock-queue` y `notificacion-queue`) se crean
**automáticamente** al arrancar LocalStack; ya no hay que crearlas a mano.
Si ejecutas `docker compose down -v` se borran los volúmenes, pero al volver
a levantar se recrean solas.

Para correr un servicio en local (fuera de Docker):

```bash
cd receta-service
./mvnw spring-boot:run     # en Windows: mvnw.cmd spring-boot:run
```

### Prueba rápida de punta a punta

Todos los ejemplos pasan por el gateway (`:8080`) bajo el prefijo `/api/<servicio>`.

```bash
# 1. Registrar un médico (con email, para que pueda recibir notificaciones)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"medico1","password":"secret123","nombreCompleto":"Dr. Prueba","email":"medico1@correo.cl","role":"MEDICO"}'

# 2. Registrar un farmacéutico
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"farma1","password":"secret123","nombreCompleto":"Farma Prueba","role":"FARMACEUTICO"}'

# 3. Cargar stock (con el JWT de cualquier usuario)
curl -X POST http://localhost:8080/api/inventario \
  -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" \
  -d '{"nombreMedicamento":"Ibuprofeno 400mg","sucursal":"Santiago Centro","cantidadDisponible":100}'

# 4. Crear la receta (JWT del médico)
curl -X POST http://localhost:8080/api/recetas \
  -H "Authorization: Bearer <TOKEN_MEDICO>" -H "Content-Type: application/json" \
  -d '{"pacienteNombre":"Juan Pérez","sucursal":"Santiago Centro",
       "medicamentos":[{"nombreMedicamento":"Ibuprofeno 400mg","cantidad":2}]}'

# 5. Ver recetas reservadas (JWT del farmacéutico)
curl http://localhost:8080/api/recetas/reservadas -H "Authorization: Bearer <TOKEN_FARMA>"

# 6. Dispensar (JWT del farmacéutico)
curl -X POST http://localhost:8080/api/dispensaciones \
  -H "Authorization: Bearer <TOKEN_FARMA>" -H "Content-Type: application/json" \
  -d '{"recetaId":1}'
```

Cuando la receta queda `RESERVADA` o `SIN_STOCK`, `notification-service` le
envía un email al médico. En local ese email **no sale a internet**: se captura
en Mailpit y lo ves en http://localhost:8025.

---

## 9. Convenciones de código

Cada servicio sigue la misma estructura por capas:

```src/main/java/cl/duoc/<servicio>/
├── controller/    → expone la API REST
├── service/       → lógica de negocio
├── repository/    → acceso a datos (Spring Data JPA)
├── model/         → entidades JPA
├── dto/           → objetos de entrada/salida
├── security/      → SecurityConfig + JwtService
├── config/        → configuración (p. ej. SqsConfig)
├── exception/     → excepciones + manejo de errores
└── messaging/     → publishers/listeners SQS (inventory y notification)
```

Paquete base: `cl.duoc.<nombre-servicio>`.

---

## 10. Estado actual y pendientes

**Funciona hoy:**

- Registro/login con JWT y control de roles.
- Ciclo completo: crear receta → reserva de stock por SQS → dispensación.
- **`notification-service` implementado**: consume `notificacion-queue` y envía un
  email al médico cuando su receta queda `RESERVADA` o `SIN_STOCK`. El endpoint
  interno `GET /internal/usuarios/{username}` de `auth-service` entrega el email.
  En local los emails se capturan en **Mailpit** (`http://localhost:8025`).
- **API Gateway** (`:8080`): punto de entrada único; enruta `/api/<servicio>/**`
  a cada servicio, así los clientes no necesitan conocer los puertos internos.
- **Secretos externalizados**: `JWT_SECRET`, credenciales de PostgreSQL y el token
  de LocalStack salen de variables de entorno (plantilla en `.env.example`).
- Las colas SQS se crean automáticamente al arrancar LocalStack.
- 5 servicios + gateway + Mailpit contenedorizados con `docker compose up --build`.

**Pendiente / puntos a mejorar:**

1. **No hay frontend.** Hoy todo se consume vía API REST.
2. **`PATCH /recetas/{id}/estado` está `permitAll`**: debería exigir un token de servicio.
3. **No hay semillas de datos** (usuarios ni stock iniciales): hay que registrarlos a mano.
4. **`INVENTORY_SERVICE_URL` está configurado en dispensing-service pero no se usa** en el código;
   hoy el stock no se vuelve a descontar ni conciliar al dispensar.
5. **Faltan tests**: solo existe el test de arranque (`*ApplicationTests`) de cada servicio.
   `notification-service` aún no tiene tests de su listener ni de su servicio de email.
