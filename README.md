# RapiEntrega · Reto 2 (Arquitectura Orientada a Servicios)

API en Spring Boot 4 / Java 21 para la plataforma de pedidos a domicilio que conecta restaurantes, clientes y repartidores.

## Cómo ejecutarlo

Requisitos: JDK 21 o superior y MySQL 8 en `localhost:3306`.

```bash
# 1. Clonar el repositorio
git clone https://github.com/SergioAC132/rapi
cd rapi

# 2. Configurar las credenciales de MySQL (ver la sección siguiente)

# 3. Ejecutar
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```

La API queda en `http://localhost:8080`.

La base `rapientrega` se crea sola: la URL de conexión lleva `createDatabaseIfNotExist=true` y Hibernate genera las tablas (`ddl-auto=update`).

### Configurar la conexión a la base de datos

El proyecto necesita un usuario de MySQL con permiso para crear la base `rapientrega`. Hay dos formas de indicar las credenciales; elige una.

#### Opción A: variables de entorno (recomendada)

No hay que tocar ningún archivo, así las credenciales nunca terminan en el repositorio.

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `DB_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/rapientrega?createDatabaseIfNotExist=true` |
| `DB_USERNAME` | Usuario de MySQL | *(sin valor: obligatoria)* |
| `DB_PASSWORD` | Contraseña de MySQL | *(sin valor: obligatoria si tu usuario tiene contraseña)* |

```bash
# Linux / macOS
export DB_USERNAME=mi_usuario
export DB_PASSWORD=mi_password
./mvnw spring-boot:run
```

```powershell
# Windows (PowerShell)
$env:DB_USERNAME="mi_usuario"
$env:DB_PASSWORD="mi_password"
.\mvnw.cmd spring-boot:run
```

Si tu MySQL no está en `localhost:3306`, define también `DB_URL`.

#### Opción B: editar `application.properties`

Abre `src/main/resources/application.properties` y haz esto:

1. **Comenta** las tres líneas que leen variables de entorno (agrégales `#` al inicio).
2. **Descomenta** las tres líneas de abajo y escribe tus datos.

```properties
# spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/rapientrega?createDatabaseIfNotExist=true}
# spring.datasource.username=${DB_USERNAME:}
# spring.datasource.password=${DB_PASSWORD:}

spring.datasource.url=jdbc:mysql://localhost:3306/rapientrega?createDatabaseIfNotExist=true
spring.datasource.username=mi_usuario
spring.datasource.password=mi_password
```

Después ejecuta normalmente con `./mvnw spring-boot:run`.

> **Importante:** si usas esta opción, no subas el archivo con tus credenciales reales a GitHub. Deja las líneas vacías en el repositorio y rellénalas solo en tu copia local.

### Pruebas

Las pruebas usan H2 en memoria, no necesitan MySQL:

```bash
./mvnw test
```

## Dominios y servicios

| Dominio | Servicios | Responsabilidad |
|---|---|---|
| Catálogo | `RestauranteService`, `ProductoService` | Restaurantes, menú y disponibilidad |
| Clientes | `ClienteService`, `CalificacionService` | Datos del cliente y calificación final |
| Pedidos | `PedidoService`, `DetallePedidoService` | Ciclo de vida del pedido y regla de los 5 minutos |
| Finanzas | `PagoService`, `FacturaService` | Cobro y reembolso con la pasarela externa, factura en el sistema heredado |
| Logística | `EntregaService`, `RepartidorService`, `SeguimientoService` | Asignación por cercanía, estados de entrega y seguimiento en tiempo real |

Cada dominio modifica solo sus propias entidades. Cuando un dominio necesita que otro reaccione, publica un evento en lugar de escribir en las tablas ajenas.

## Flujo «Pedido de comida»

1. `POST /api/pedidos`: crea el pedido (`PendientePago`).
2. `POST /api/detalles-pedido`: agrega productos y recalcula el total.
3. `POST /api/pagos`: cobra con la pasarela (síncrono). Responde `201` si se aprueba o `402` si se rechaza (se puede reintentar). Si se aprueba, el pedido pasa a `PendienteAceptacion` con límite de 5 minutos.
4. `PATCH /api/pedidos/{id}/aceptar?restauranteId=` o `/rechazar`.
   - Al aceptar se publica `PedidoAceptadoEvent`: Logística crea la entrega y asigna al repartidor disponible más cercano al restaurante (asíncrono). Si no hay repartidores, cada 30 s se reintenta.
   - Al rechazar, cancelar o vencer el plazo se publica `PedidoCanceladoEvent`: Pagos reembolsa automáticamente (asíncrono).
   - Una tarea programada revisa cada 10 s los pedidos que pasaron sus 5 minutos y los cancela.
5. `GET /api/entregas/pedido/{pedidoId}/seguimiento`: flujo SSE (`text/event-stream`) que recibe cada cambio de estado y de ubicación de la entrega.
6. `PATCH /api/entregas/{id}/recogida`, `/iniciar`, `/ubicacion` (body `{"latitud":..,"longitud":..}`) y `/confirmar`.
7. `POST /api/calificaciones`: el cliente califica restaurante y repartidor.
8. `POST /api/facturas`: responde `202`; la factura se envía al sistema heredado de forma asíncrona y termina en `Emitida` o `Error` (`POST /api/facturas/{id}/reintentar`).

## Patrones aplicados

Nombres del catálogo de Erl (*SOA Design Patterns*) visto en la Unidad 2.

| Patrón | Dónde | Problema del caso que resuelve |
|---|---|---|
| Abstracción de entidad | `RestauranteService`, `ProductoService`, `ClienteService`, `RepartidorService`… | Restaurantes, menú y repartidores se usan en búsqueda, pedido y asignación; su lógica no se repite en cada proceso |
| Abstracción de proceso | `PedidoService`, `PagoService`, `EntregaService`, `FacturaService` | La regla de los 5 minutos y el orden pago → aceptación → entrega viven en servicios de tarea, separados de las entidades |
| Abstracción de utilidad | `SeguimientoService`, adaptadores de `integration/` | Notificar en tiempo real y hablar con sistemas externos son funciones técnicas, no del negocio |
| Envoltorio de sistema heredado (Legacy Wrapper) | `integration/facturacion/SistemaFacturacionLegadoAdapter` | El sistema de facturación de 2012 no se puede reemplazar; el envoltorio traduce su formato de texto delimitado a un contrato estandarizado |
| Fachada de servicio (Service Façade) | `integration/pagos/PasarelaPagos` + `PasarelaPagosSimulada` | La pasarela es externa con API propia; cambiar de proveedor no altera el contrato de Pago |
| Esquema canónico (Canonical Schema) | `exceptions/ManejadorErrores` | Los equipos que consumen los servicios en paralelo reciben todos los errores con el mismo esquema (`ProblemDetail`) |

*Contrato desacoplado* no está aplicado: las respuestas devuelven las entidades de base de datos. Queda como trabajo futuro.

## Comunicación

| Interacción | Estilo | Patrón de intercambio |
|---|---|---|
| Cliente → Pedidos / Detalle / Pago | Síncrona (REST) | Petición-respuesta |
| Pagos → Pasarela externa (cobro) | Síncrona | Petición-respuesta |
| Pedidos → Logística (pedido aceptado) | Asíncrona | Publicación-suscripción |
| Pedidos → Finanzas (pedido cancelado → reembolso) | Asíncrona | Publicación-suscripción |
| Cliente → Facturación (`solicitarFactura`) | Asíncrona | Petición-respuesta con acuse `202` y consulta de estado |
| Logística → Cliente (seguimiento) | Asíncrona (SSE) | Publicación-suscripción |
| Temporizador → Pedidos (vencimiento) | Asíncrona | Unidireccional |

## Errores

Todas las respuestas de error usan `application/problem+json` (`ProblemDetail`): `404` si el recurso no existe, `400` si hay datos inválidos, `409` si la operación no es válida en el estado actual y `402` si la pasarela rechaza el pago.

## Simulación de servicios externos

`PasarelaPagosSimulada` aprueba cualquier token excepto `tok_rechazada`. `SistemaFacturacionLegadoAdapter` rechaza los RFC que no tengan 12 o 13 caracteres. Para conectar los servicios reales basta con otra implementación de `PasarelaPagos` o `SistemaFacturacion`.
