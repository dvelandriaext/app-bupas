package co.com.coexito.bupas.domain.model

import androidx.compose.runtime.Immutable

/**
 * Estados oficiales de una Orden de Recolección BUPAS (HU-005 y HU-006).
 * [isSelectableByDriver] indica si el conductor puede elegirlo manualmente en campo.
 */
enum class OrdenStatus(
    val displayName: String,
    val isSelectableByDriver: Boolean,
    val isReadOnlyLocked: Boolean
) {
    ORDEN_ASIGNADA(
        displayName = "Orden Asignada",
        isSelectableByDriver = false,
        isReadOnlyLocked = false
    ),
    EN_ATENCION(
        displayName = "En Atención",
        isSelectableByDriver = true,
        isReadOnlyLocked = false
    ),
    RECOLECCION_PARCIAL(
        displayName = "Recolección Parcial",
        isSelectableByDriver = true,
        isReadOnlyLocked = false
    ),
    NO_RECOLECTADA(
        displayName = "No Recolectada",
        isSelectableByDriver = true,
        isReadOnlyLocked = true
    ),
    RECOLECCION_REALIZADA(
        displayName = "Recolección Realizada",
        isSelectableByDriver = false,
        isReadOnlyLocked = true
    );

    companion object {
        fun selectableByDriver(): List<OrdenStatus> = entries.filter { it.isSelectableByDriver }
    }
}

enum class TipoBateriaProveniente(val label: String) {
    AUTO("Auto"),
    MOTO("Moto"),
    SCRAP("Scrap")
}

@Immutable
data class PrecioArticuloBupas(
    val codigoArticulo: String,
    val descripcion: String,
    val pesoUnitarioKg: Double,
    val valorCompraUnitario: Double
)

@Immutable
data class ArticuloBupas(
    val codigoArticulo: String,
    val descripcion: String,
    val cantidadInicial: Int,
    val cantidadActa: Int?,
    val pesoUnitarioKg: Double,
    val valorCompraUnitario: Double
) {
    val pesoTotalKg: Double
        get() = (cantidadActa ?: 0) * pesoUnitarioKg

    val valorCompraTotal: Double
        get() = (cantidadActa ?: 0) * valorCompraUnitario
}

@Immutable
data class TerceroProveniente(
    val id: String,
    val nit: String,
    val razonSocial: String,
    val tipoBateria: TipoBateriaProveniente,
    val cantidadBaterias: Int,
    val telefono: String,
    val correoElectronico: String
)

@Immutable
data class OrdenRecoleccion(
    val numeroSolicitud: String,
    val identificadorOm: String,
    val cedi: String,
    val fechaRecoleccion: String,
    val nitCliente: String,
    val razonSocialCliente: String,
    val direccionRecogida: String,
    val departamento: String,
    val ciudad: String,
    val vendedor: String,
    val tipoDevolucion: String,
    val observacionesIniciales: String,
    val estado: OrdenStatus,
    val articulos: List<ArticuloBupas> = emptyList(),
    val aplicaProvenientes: Boolean = false,
    val provenientes: List<TerceroProveniente> = emptyList(),
    val numeroActaGenerada: String? = null,
    val detalleNovedad: String? = null,
    val observacionesCampo: String = "",
    val nombreResponsableEntrega: String = "",
    val identificacionResponsableEntrega: String = "",
    val fechaHoraInicioAtencion: String? = null,
    val fechaHoraFinalizacion: String? = null,
    val firmaDigitalBase64: String? = null,
    val telefonoCliente: String = "3164208890",
    val correoPrincipalCliente: String = "ambiental@cliente.com.co",
    val correoAlternoCliente: String? = null,
    val conductorNombre: String = "Juan Carlos Ramírez",
    val conductorCedula: String = "1144089231",
    val placaVehiculo: String = "WXY-482",
    val estadoGeneracionActa: EstadoGeneracionActa? = null,
    val estadoEnvioActa: EstadoEnvioActa? = null,
    val detalleErrorActa: String? = null
) {
    val totalUnidadesActa: Int
        get() = articulos.sumOf { it.cantidadActa ?: 0 }

    val canViewActaPdf: Boolean
        get() = !numeroActaGenerada.isNullOrBlank() &&
            (estadoGeneracionActa ?: EstadoGeneracionActa.GENERADA) == EstadoGeneracionActa.GENERADA

    val pesoTotalEstimadoKg: Double
        get() = if (articulos.any { it.cantidadActa != null }) {
            articulos.sumOf { it.pesoTotalKg }
        } else {
            articulos.sumOf { it.cantidadInicial * it.pesoUnitarioKg }
        }

    val valorCompraTotalEstimado: Double
        get() = if (articulos.any { it.cantidadActa != null }) {
            articulos.sumOf { it.valorCompraTotal }
        } else {
            articulos.sumOf { it.cantidadInicial * it.valorCompraUnitario }
        }
}
