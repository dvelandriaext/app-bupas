# Reglas de Arquitectura y Estándares del Proyecto — App Móvil BUPAS (`app-bupas`)

Este repositorio contiene el aplicativo móvil Android nativo en **Kotlin + Jetpack Compose (Material 3)** para el **Proyecto BUPAS (Logística Reversiva de Baterías Usadas Plomo-Ácido)** de **Coéxito S.A.S.**

## 1. Skills y Agentes Especializados del Workspace

Antes de diseñar, implementar, refactorizar o auditar código en este proyecto, activa y sigue los lineamientos de los siguientes Skills y Agentes locales en `.agents/skills/`:

1. **[`coe-kotlin-architecture`](.agents/skills/coe-kotlin-architecture/SKILL.md)**:
   - Arquitectura Limpia (`core`, `domain`, `data`, `presentation`) + patrón UDF (MVVM/MVI con `StateFlow` inmutable y `collectAsStateWithLifecycle()`).
   - Los 4 Pilares de Diseño Coéxito: archivos `<= 600` líneas ideal (`< 1000` máx), SRP estricto, funciones concisas (`<= 40` líneas, anidamiento `<= 2`) y *Composed Method*.
2. **[`coe-compose-theme-system`](.agents/skills/coe-compose-theme-system/SKILL.md)**:
   - Sistema de diseño basado en `BupasTheme` (Material 3 + `BupasExtendedColors` + `BupasSpacing`).
   - Tipografía corporativa obligatoria **Plus Jakarta Sans** (`PlusJakartaSansFontFamily`) en toda la escala tipográfica.
   - Cero colores (`Color(0x...)`) o tamaños tipográficos hardcodeados fuera de `core/designsystem/theme/`.
3. **[`coe-compose-ui-components`](.agents/skills/coe-compose-ui-components/SKILL.md)**:
   - Catálogo de componentes UI reutilizables en `core/designsystem/components/` bajo el patrón *Stateless Composable* con *State Hoisting*.
   - Soporte obligatorio de validación en rojo (`isError`, `errorMessage`) y bloqueo en modo solo lectura (`readOnly`).

## 2. Alcance Funcional del Aplicativo Móvil (Rol Conductor en Campo)

- **`HU-001` — Iniciar Sesión y 2FA**: Autenticación con credenciales, validación local en rojo, código 2FA de 6 dígitos al correo (vigencia de 3 minutos), recuperación de contraseña y salida segura.
- **`HU-005` — Consultar Órdenes de Recolección Asignadas**: Bandeja de órdenes del conductor con filtro por rango de fechas, visualización de cliente, NIT, OM, peso estimado, valor estimado, estado y acceso al detalle conservando los filtros al volver.
- **`HU-006` — Recolección de BUPAS en Campo**: Registro de estado (`En Atención`, `Recolección Parcial`, `No Recolectada`), ajuste de *Cantidad Acta* por artículo con recálculo dinámico de `Peso Total (kg)` y `Valor Compra Total ($)`, gestión de terceros *Provenientes*, datos del responsable que entrega y cierre definitivo (`Recolección Realizada` en modo solo lectura).
- **`HU-007` — Generación, Envío y Consulta de Acta Respel**: Disparo automático del Acta PDF (`INS-XXXX` / `POS-XXXX`) al finalizar recolección con cantidad `> 0` y consulta mediante la acción *Ver Acta*.
