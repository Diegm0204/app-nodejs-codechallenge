# Solución del Reto Técnico

## Descripción

Implementé una solución de microservicios con Spring Boot y GraphQL para manejar transacciones con validación de fraude en tiempo real usando Kafka.

## Arquitectura

- Transaction Service: Maneja la creación y consulta de transacciones (Puerto 8080)
- Anti-Fraud Service: Valida transacciones en tiempo real usando reglas de negocio
- Kafka: Comunicación asíncrona entre servicios
- PostgreSQL: Persistencia de datos
- Redis: Caché distribuida

## Tecnologías

- Java 17
- Spring Boot 3.2.1
- GraphQL
- Apache Kafka 7.5.0
- PostgreSQL 15
- Redis 7
- Docker & Docker Compose
- Gradle 8.5

---

## Cómo levantar el proyecto

### Prerequisitos

- Docker Desktop instalado y corriendo
- Puerto 8080 disponible

### Levantar los servicios

```bash
# Desde la raíz del proyecto
docker-compose up -d
```

Esto levanta todos los servicios:
- PostgreSQL (puerto 5432)
- Redis (puerto 6379)
- Kafka + Zookeeper (puertos 9092, 2181)
- Transaction Service (puerto 8080)
- Anti-Fraud Service

### Verificar que todo esté corriendo

```bash
docker-compose ps
```

Deberías ver todos los contenedores como "healthy".

### Ver logs

```bash
# Ver logs de Transaction Service
docker-compose logs -f transaction-service

# Ver logs de Anti-Fraud Service
docker-compose logs -f anti-fraud-service

# Ver todos los logs
docker-compose logs -f
```

### Detener los servicios

```bash
docker-compose down
```

Para limpiar todo (incluyendo volúmenes):

```bash
docker-compose down -v
```

---

## Cómo ejecutar los tests

### Ejecutar todos los tests

```bash
docker run --rm -v ${PWD}:/app -w /app gradle:8.5-jdk17 gradle clean test --no-daemon
```

### Ejecutar tests de un servicio específico

```bash
# Solo Transaction Service
docker run --rm -v ${PWD}:/app -w /app gradle:8.5-jdk17 gradle :transaction-service:test --no-daemon

# Solo Anti-Fraud Service
docker run --rm -v ${PWD}:/app -w /app gradle:8.5-jdk17 gradle :anti-fraud-service:test --no-daemon
```

### Ver reportes de tests

Después de ejecutar los tests, abre en tu navegador:

- Transaction Service: `transaction-service/build/reports/tests/test/index.html`
- Anti-Fraud Service: `anti-fraud-service/build/reports/tests/test/index.html`

**Tests incluidos:**
- TransactionServiceTest: 5 tests unitarios
- FraudDetectionServiceTest: 5 tests unitarios
- Total: 10 tests (100% passing)

---

## Cómo probar las funcionalidades



### 2. Crear una transacción (con protección contra duplicados)

```graphql
mutation {
  createTransaction(input: {
    accountExternalIdDebit: "123e4567-e89b-12d3-a456-426614174000"
    accountExternalIdCredit: "123e4567-e89b-12d3-a456-426614174001"
    transferTypeId: 1
    value: 500
    idempotencyKey: "mi-uuid-unico-12345"
  }) {
    transactionId
    status
    value
    createdAt
  }
}
```

**Resultado esperado:** Transacción creada con estado PENDING, luego procesada por el Anti-Fraud Service.

**Nota:** El campo `idempotencyKey` es opcional pero recomendado para prevenir duplicados por reintentos o doble clic.

### 3. Consultar una transacción

```graphql
query {
  transaction(id: "PEGA_AQUI_EL_ID_DE_LA_TRANSACCION") {
    transactionId
    accountExternalIdDebit
    accountExternalIdCredit
    transactionType {
      name
    }
    status
    value
    createdAt
    updatedAt
  }
}
```


### 4. Listar transacciones (paginado)

```graphql
query {
  transactions(page: 0, size: 10) {
    content {
      transactionExternalId
      transactionStatus {
        name
      }
      value
      createdAt
    }
    totalElements
    totalPages
    currentPage
  }
}
```

---

## Casos de prueba

### Caso 1: Transacción aprobada (monto <= 1000)

```graphql
mutation {
  createTransaction(input: {
    accountExternalIdDebit: "d0d32142-74b7-4aca-9c68-838cbde24434"
    accountExternalIdCredit: "a1e56789-12b3-45cd-9876-543210fedcba"
    tranferTypeId: 1
    value: 500.00
  }) {
    transactionExternalId
    transactionStatus { name }
  }
}
```

**Resultado:** Estado cambia de PENDING a APPROVED

### Caso 2: Transacción rechazada (monto > 1000)

```graphql
mutation {
  createTransaction(input: {
    accountExternalIdDebit: "d0d32142-74b7-4aca-9c68-838cbde24434"
    accountExternalIdCredit: "a1e56789-12b3-45cd-9876-543210fedcba"
    tranferTypeId: 1
    value: 1500.00
  }) {
    transactionExternalId
    transactionStatus { name }
  }
}
```

**Resultado:** Estado cambia de PENDING a REJECTED

### Caso 3: Validación de monto inválido

```graphql
mutation {
  createTransaction(input: {
    accountExternalIdDebit: "d0d32142-74b7-4aca-9c68-838cbde24434"
    accountExternalIdCredit: "a1e56789-12b3-45cd-9876-543210fedcba"
    tranferTypeId: 1
    value: -100.00
  }) {
    transactionExternalId
  }
}
```

**Resultado:** Error de validación (GraphQL devuelve 200 con error en campo `errors`)

### Caso 4: Tipo de transacción inválido

```graphql
mutation {
  createTransaction(input: {
    accountExternalIdDebit: "d0d32142-74b7-4aca-9c68-838cbde24434"
    accountExternalIdCredit: "a1e56789-12b3-45cd-9876-543210fedcba"
    tranferTypeId: 999
    value: 100.00
  }) {
    transactionExternalId
  }
}
```

**Resultado:** Error "ID de tipo de transacción inválido"

---

##  Verificar el flujo completo

1. **Crear transacción** → Estado PENDING
2. **Kafka procesa** → Transaction Service publica evento `TransactionCreatedEvent`
3. **Anti-Fraud valida** → Aplica regla de negocio (monto > 1000)
4. **Kafka actualiza** → Anti-Fraud publica evento `TransactionStatusUpdatedEvent`
5. **Estado final** → Transaction Service actualiza a APPROVED o REJECTED

### Verificar en logs:

```bash
docker-compose logs -f transaction-service | grep "Transacción creada"
docker-compose logs -f anti-fraud-service | grep "Validando transacción"
docker-compose logs -f transaction-service | grep "Estado de transacción actualizado"
```

---

## 🔍 Endpoints de monitoreo

- Health Check: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics

---

##  Estructura del proyecto

```
yape/
├── transaction-service/       # Servicio de transacciones (GraphQL)
├── anti-fraud-service/        # Servicio de detección de fraude
├── shared-domain/             # Eventos compartidos (Kafka)
├── docker-compose.yml         # Orquestación de servicios
└── README_SOLUCION.md        # Este archivo
```

---

### Ver logs de errores

```bash
docker-compose logs transaction-service
docker-compose logs anti-fraud-service
docker-compose logs kafka
```

---

## Checklist de funcionalidades implementadas

-  Crear transacciones con GraphQL
-  Consultar transacción por ID
-  Listar transacciones con paginación
-  Filtrar transacciones por status
-  Validación de campos (monto, IDs, tipo)
-  **Protección contra duplicados (Idempotency Keys)**
-  Detección de fraude asíncrona
-  Comunicación con Kafka (eventos)
-  Persistencia en PostgreSQL
-  Caché con Redis
-  Bloqueo pesimista (concurrencia)
-  Health checks
-  Tests unitarios (13 tests)
-  Logs estructurados
-  Dockerización completa

---

## Decisiones técnicas

- **GraphQL**: Para queries flexibles y evitar overfetching
- **Kafka**: Para comunicación asíncrona y desacoplamiento
- **Redis**: Para mejorar performance en consultas frecuentes
- **Arquitectura Hexagonal**: Para mantener el dominio separado de la infraestructura
- **Docker Compose**: Para facilitar el deployment y testing local
- **Idempotencia**: Protección contra duplicados por reintentos o doble clic

---

##  Protección contra Duplicados (Idempotencia)

La aplicación implementa **Idempotency Keys** para prevenir transacciones duplicadas causadas por:
- Reintentos automáticos del cliente
- Doble clic del usuario
- Fallos de red con retry

### ¿Cómo funciona?

1. **El cliente genera un UUID único** antes de enviar la transacción
2. **Lo envía en el campo `idempotencyKey`**
3. **Si la misma key se envía 2 veces**, el sistema devuelve la transacción original (no crea un duplicado)

### Ejemplo con idempotencyKey:

```graphql
mutation {
  createTransaction(input: {
    accountExternalIdDebit: "123e4567-e89b-12d3-a456-426614174000"
    accountExternalIdCredit: "123e4567-e89b-12d3-a456-426614174001"
    transferTypeId: 1
    value: 500
    idempotencyKey: "client-generated-uuid-12345"
  }) {
    transactionId
    status
    value
  }
}
```

**Primera ejecución:** Crea la transacción → status PENDING  
**Segunda ejecución (misma key):** Devuelve la transacción existente (NO crea duplicado)

### Implementación técnica:

- Campo `idempotency_key` en base de datos con **índice único**
- Validación en capa de servicio **antes de crear la transacción**
- Tests unitarios para ambos casos (con y sin idempotencyKey)

---

