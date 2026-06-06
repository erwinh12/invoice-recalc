# Invoice Recalculation — Prueba Técnica

## Estructura del proyecto

```
invoice-recalc/
├── backend/               ← Spring Boot (Java 17)
│   └── src/main/java/com/invoice/recalc/
│       ├── controller/    InvoiceController.java
│       ├── service/       InvoiceService.java
│       ├── repository/    InvoiceRepository.java
│                          InvoiceDetailRepository.java
│       ├── model/         Invoice.java, InvoiceDetail.java, User.java
│       ├── dto/           InvoiceDto.java
│       ├── exception/     GlobalExceptionHandler.java + custom exceptions
│       └── config/        DataInitializer.java
└── frontend-guide/        ← Archivos Angular (copiar a tu proyecto)
    ├── invoice.service.ts
    ├── invoice-recalc.component.ts
    └── invoice-recalc.component.html
```

---

## Backend — Cómo ejecutar

### Requisitos
- Java 17+
- Maven 3.8+

### Ejecutar
```bash
cd backend
mvn spring-boot:run
```

El servidor inicia en `http://localhost:8080`

### H2 Console (debug BD)
http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:invoicedb`
- User: `sa` / Password: (vacío)

---

## API Endpoints

| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/v1/invoices` | Listar todas las facturas |
| GET | `/api/v1/invoices/{id}` | Obtener factura por ID |
| POST | `/api/v1/invoices/{id}/recalculate/preview` | Vista previa sin guardar |
| POST | `/api/v1/invoices/{id}/recalculate/confirm` | Confirmar y guardar |

### Body de recálculo
```json
{
  "newSubtotal": 60000,
  "userType": "TIPO_A"
}
```

### Respuesta de errores
```json
{
  "status": 422,
  "error": "Límite de recálculo excedido",
  "message": "El usuario tipo TIPO_A no puede incrementar el subtotal más de $20000...",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## Reglas de negocio implementadas

### Distribución proporcional
- Factor = `nuevoSubtotal / subtotalOriginal`
- Cada `lineTotal` = `lineTotal * factor`
- El **último ítem** absorbe la diferencia de redondeo para garantizar exactitud

### Topes por tipo de usuario
| Usuario | Incremento máximo |
|---------|-------------------|
| TIPO_A (Operador) | +$20,000 |
| TIPO_B (Supervisor) | +$50,000 |

- Las **reducciones** no tienen restricción de tope.

### Códigos HTTP retornados
| Situación | Código |
|-----------|--------|
| Éxito | 200 OK |
| Validación de campos | 400 Bad Request |
| Factura no encontrada | 404 Not Found |
| Límite excedido | 422 Unprocessable Entity |
| Error interno | 500 Internal Server Error |

---

## Frontend — Cómo integrar

### Requisitos
```bash
ng new invoice-app --routing --style=scss
cd invoice-app
```

### Instalar dependencias
```bash
npm install  # Tailwind CSS es opcional, usa clases de Bootstrap o estilos propios
```

### Pasos de integración
1. Copia `invoice.service.ts` → `src/app/services/`
2. Crea componente: `ng generate component components/invoice-recalc`
3. Reemplaza los archivos `.ts` y `.html` generados con los del `frontend-guide/`
4. En `app.module.ts` importa `HttpClientModule`, `ReactiveFormsModule`, `CommonModule`
5. Agrega el componente en tu `app.component.html`: `<app-invoice-recalc></app-invoice-recalc>`

### app.module.ts (imports necesarios)
```typescript
import { HttpClientModule } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
```

---

## Tests

```bash
cd backend
mvn test
```

Casos cubiertos:
- Reducción proporcional correcta (80k → 60k = factor 0.75)
- Tipo A dentro del límite (+20k exacto: OK)
- Tipo A supera límite (+20,001: excepción)
- Tipo B dentro del límite (+50k exacto: OK)
- Tipo B supera límite (+50,001: excepción)
- Reducción sin restricción (siempre permitida)
- Factura inexistente (404)
- Suma de detalles = nuevo subtotal tras confirmar

---

## Datos de prueba cargados automáticamente

**FAC-001** — Empresa ABC S.A.S — Subtotal: $80,000
- Laptop Dell Inspiron × 1 → $50,000
- Mouse Inalámbrico × 2 → $15,000
- Teclado Mecánico × 1 → $15,000

**FAC-002** — Distribuciones XYZ Ltda — Subtotal: $120,000
- Monitor 27" × 2 → $70,000
- Silla Ergonómica × 1 → $30,000
- Escritorio de Pie × 1 → $20,000
