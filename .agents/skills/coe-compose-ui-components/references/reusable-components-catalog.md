# Catálogo de Componentes UI Reutilizables — Proyecto BUPAS (`core/designsystem/components/`)

## 1. Botones (`BupasButtons.kt`)
- **`BupasPrimaryButton`**: Botón principal de acción (*Iniciar Sesión*, *Verificar*, *Siguiente*, *Finalizar Recolección*). Soporta `isLoading: Boolean`, `enabled: Boolean` e ícono opcional. Deshabilitado automáticamente cuando el formulario no es válido (`HU-001`).
- **`BupasSecondaryButton`**: Botón con borde (`OutlinedButton`) para navegación secundaria (*Atrás*, *Volver*, *+ Agregar Provenientes*).
- **`BupasTextButton`**: Enlace de acción ligera (*¿Olvidaste tu contraseña?*, *Volver al login*, *Ver Acta*).

## 2. Campos de Entrada con Validación en Rojo (`BupasTextFields.kt`)
- **`BupasTextField`**: Campo de texto general con soporte para `isRequired`, `isError`, `errorMessage` (ej. *"Campo requerido"*), `readOnly` y multilínea (para *Observaciones* y *Detalle Novedad*).
- **`BupasPasswordField`**: Campo de contraseña con alternancia de visibilidad (mostrar/ocultar) y validación local en rojo (`HU-001`).
- **`BupasOtpField`**: Entrada de código 2FA numérico de **6 dígitos** con indicador de vigencia (3 minutos) y estados visuales de error (*"Código de verificación incorrecto"* / *"El Código de verificación ha expirado"* - `HU-001`).
- **`BupasQuantityActaField`**: Control numérico especializado para capturar la **Cantidad Acta** por artículo BUPAS en campo (`HU-006`), con botones táctiles `-` / `+` e input directo, resaltado de obligatoriedad y bloqueo en solo lectura.

## 3. Badges y Tarjetas de Dominio (`BupasStatusBadge.kt`, `BupasCards.kt`)
- **`BupasStatusBadge`**: Chip/Badge semántico que renderiza el estado de la orden (`Orden Asignada`, `En Atención`, `Recolección Parcial`, `No Recolectada`, `Recolección Realizada`) usando `BupasTheme.extendedColors`.
- **`BupasOrderCard`**: Tarjeta resumen de una orden en la bandeja del conductor (`HU-005`) mostrando `Nro. Solicitud`, `Identificador OM`, `Fecha de recolección`, `Cliente (NIT / Razón Social)`, `Peso total (kg)`, `Valor Compra Total ($)`, `BupasStatusBadge` y botón `Ver detalle`.
- **`BupasSectionCard`**: Contenedor visual estructurado por sección para el flujo de recolección en campo (`HU-006`: Datos del Cliente, Artículos BUPAS, Provenientes, Confirmación).
- **`BupasSummaryTotalsBar`**: Barra/panel de resumen dinámico que muestra en tiempo real `Total Unidades`, `Total (kg)` y `Total ($)` a medida que el conductor ajusta la *Cantidad Acta* (`HU-006`).

## 4. Navegación Superior y Modales (`BupasTopBar.kt`, `BupasDialogs.kt`)
- **`BupasTopBar`**: TopAppBar corporativa con botón de retorno y acción de *Salida segura* (`HU-001`).
- **`BupasAlertModal`**: Ventana emergente con ícono informativo o de error y botón *OK* (para *"Usuario o contraseña inválido."* y *"Se ha enviado un código de verificación a su correo electrónico"* - `HU-001`).
- **`BupasConfirmDialog`**: Modal de confirmación con botones **Sí / No** (para *"¿Está seguro de finalizar la recolección? Una vez finalizada, no podrá modificar la información registrada."* - `HU-006`).
