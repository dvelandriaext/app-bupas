# Especificación de Tokens de Theme y Tipografía `Plus Jakarta Sans` — Proyecto BUPAS

## 1. Paleta de Colores Corporativa y Semántica (`Color.kt`)

| Token | Hex (Modo Claro) | Uso en Proyecto BUPAS |
| :--- | :--- | :--- |
| `BupasPrimary` | `#004B87` | Azul corporativo Coéxito (acciones primarias, encabezados, navegación activa) |
| `BupasOnPrimary` | `#FFFFFF` | Texto e íconos sobre botón/superficie primaria |
| `BupasPrimaryContainer` | `#D4E4FF` | Contenedores destacados y cabeceras de resumen |
| `BupasSecondary` | `#F59E0B` | Acento energético / logístico Coéxito |
| `BupasBackground` | `#F8FAFC` | Fondo general limpio para alta legibilidad en campo |
| `BupasSurface` | `#FFFFFF` | Tarjetas de órdenes (`HU-005`) y secciones de recolección (`HU-006`) |
| `BupasError` | `#D32F2F` | Resaltado en rojo obligatorio (*"Campo requerido"*, 2FA inválido/expirado) |

### Tokens Semánticos por Estado de Orden (`BupasExtendedColors`)

| Estado de Orden | Container Hex | Content Hex | Regla de Negocio |
| :--- | :--- | :--- | :--- |
| `ORDEN_ASIGNADA` | `#E0F2FE` | `#0369A1` | Automático post-planeación OTM |
| `EN_ATENCION` | `#FEF3C7` | `#B45309` | Seleccionable por conductor en sitio |
| `RECOLECCION_PARCIAL` | `#EDE9FE` | `#6D28D9` | Seleccionable por conductor (exige observación) |
| `NO_RECOLECTADA` | `#FEE2E2` | `#B91C1C` | Seleccionable por conductor (exige novedad) |
| `RECOLECCION_REALIZADA` | `#DCFCE7` | `#15803D` | Automático al confirmar cierre en campo |

---

## 2. Escala Tipográfica `Plus Jakarta Sans` (`Type.kt`)

| Estilo Material 3 | Peso (`FontWeight`) | Tamaño (`sp`) | Line Height (`sp`) | Uso en App Móvil BUPAS |
| :--- | :--- | :--- | :--- | :--- |
| `headlineLarge` | `Bold (700)` | `28.sp` | `36.sp` | Títulos de acceso (Iniciar Sesión / 2FA) |
| `headlineMedium` | `Bold (700)` | `24.sp` | `32.sp` | Encabezados principales de módulo |
| `titleLarge` | `SemiBold (600)` | `20.sp` | `28.sp` | TopAppBar, modales de confirmación |
| `titleMedium` | `SemiBold (600)` | `16.sp` | `24.sp` | Nro. Solicitud / Razón Social en tarjetas y cabeceras de sección |
| `titleSmall` | `Medium (500)` | `14.sp` | `20.sp` | Subtítulos, etiquetas de campos obligatorios |
| `bodyLarge` | `Normal (400)` | `16.sp` | `24.sp` | Texto de entrada en inputs (`Usuario`, `Cantidad Acta`) |
| `bodyMedium` | `Normal (400)` | `14.sp` | `20.sp` | Detalles de dirección, artículos BUPAS, observaciones |
| `labelLarge` | `SemiBold (600)` | `14.sp` | `20.sp` | Texto de botones primarios/secundarios |
| `labelMedium` | `Medium (500)` | `12.sp` | `16.sp` | Badges de estado (`OrdenStatus`) y mensajes de validación en rojo |
