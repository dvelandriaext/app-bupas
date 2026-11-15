# Guía de Clean Architecture, UDF y Estructura de Paquetes — Proyecto BUPAS Mobile (Kotlin)

## 1. Estructura Modular de Paquetes (`co.com.coexito.bupas`)

```text
co.com.coexito.bupas/
├── BupasApplication.kt                 # @HiltAndroidApp
├── MainActivity.kt                     # ComponentActivity + BupasTheme + BupasNavHost
├── core/
│   ├── common/                         # AppResult, Dispatchers, extensiones de formato (Kg, COP, Fechas)
│   ├── designsystem/
│   │   ├── theme/                      # Color.kt, Type.kt (Plus Jakarta Sans), Shape.kt, Spacing.kt, Theme.kt
│   │   └── components/                 # Componentes UI reutilizables de BUPAS
│   ├── network/                        # Interceptores JWT/2FA, clientes HTTP y manejo de errores de red
│   └── navigation/                     # BupasDestination, BupasNavHost y protección de sesión
├── domain/
│   ├── model/                          # Entidades de negocio puras (OrdenRecoleccion, ArticuloBupas, OrdenStatus, etc.)
│   ├── repository/                     # Interfaces de repositorios (AuthRepository, OrderRepository, ActaRepository)
│   └── usecase/                        # Casos de uso (CalculateCollectionTotalsUseCase, ValidateLoginUseCase, etc.)
├── data/
│   ├── local/                          # Room Entities/DAOs y DataStore (sesión y borradores offline en campo)
│   ├── remote/                         # APIs REST, DTOs y Mappers
│   └── repository/                     # Implementaciones de repositorios de dominio
└── presentation/
    ├── auth/                           # HU-001: Login, 2FA (vigencia 3 min) y Recuperar Contraseña
    ├── orders/                         # HU-005: Bandeja de Órdenes Asignadas con filtro de fechas
    ├── collection/                     # HU-006: Atención en campo (Datos Cliente, Cantidad Acta, Provenientes, Cierre)
    └── certificate/                    # HU-007: Consulta y visualización del Acta de Recolección PDF (Respel)
```

## 2. Patrón Unidirectional Data Flow (UDF) con MVI / MVVM

Cada feature en `presentation/<feature>/` sigue el contrato:

```kotlin
@Immutable
data class FeatureUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Estado específico de la vista
)

sealed interface FeatureUiEvent {
    // Intenciones del usuario en la pantalla
}

sealed interface FeatureUiEffect {
    // Efectos de un solo disparo (navegación, diálogos, snackbars)
}
```

## 3. Reglas de Dominio Críticas para el Rol Conductor (BUPAS)

1. **Estados de la Orden (`OrdenStatus`)**:
   - `ORDEN_ASIGNADA` y `RECOLECCION_REALIZADA` son estados controlados **exclusivamente por el sistema**.
   - El conductor en campo solo puede seleccionar manualmente: `EN_ATENCION`, `RECOLECCION_PARCIAL` y `NO_RECOLECTADA`.
2. **Flujo `NO_RECOLECTADA` (`HU-006`)**:
   - Oculta/deshabilita las secciones de artículos, provenientes y confirmación.
   - Exige obligatoriamente el campo `detalleNovedad` antes de permitir guardar y bloquear la orden en solo lectura.
3. **Recálculo Dinámico de Artículos BUPAS (`HU-006`)**:
   - Cada artículo carga desde la lista de precios del cliente (incluso con `cantidadInicial == 0`).
   - `pesoTotalKg = cantidadActa * pesoUnitarioKg`
   - `valorCompraTotal = cantidadActa * valorUnitarioCompra`
   - Ningún ítem puede quedar con `cantidadActa == null` al avanzar; si no se recolectó, debe registrarse explícitamente `0`.
4. **Cierre y Acta Respel (`HU-006` y `HU-007`)**:
   - Al confirmar la finalización (`Sí` en el modal), la orden pasa a `RECOLECCION_REALIZADA`, queda en modo **solo lectura** y dispara la generación automática del PDF (`INS-XXXX` o `POS-XXXX`) cuando la cantidad total recolectada es `> 0`.
