package co.com.coexito.bupas.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AssignedOrdersResponseDto(
    val orders: List<OrderDto>,
    val totalCount: Int
)

@Serializable
data class OrderDto(
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
    val estado: String,
    val articulos: List<ArticuloBupasDto> = emptyList(),
    val aplicaProvenientes: Boolean = false,
    val provenientes: List<TerceroProvenienteDto> = emptyList(),
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
    val estadoGeneracionActa: String? = null,
    val estadoEnvioActa: String? = null,
    val detalleErrorActa: String? = null
)

@Serializable
data class SaveFieldCollectionRequestDto(
    val estado: String,
    val articulos: List<ArticuloCantidadDto>,
    val aplicaProvenientes: Boolean,
    val provenientes: List<TerceroProvenienteDto>,
    val detalleNovedad: String? = null,
    val observacionesCampo: String = "",
    val nombreResponsableEntrega: String = "",
    val identificacionResponsableEntrega: String = "",
    val finalizeCollection: Boolean = false,
    val fechaHoraInicioAtencion: String? = null,
    val fechaHoraFinalizacion: String? = null,
    val firmaDigitalBase64: String? = null
)

@Serializable
data class PrecioArticuloBupasDto(
    val codigoArticulo: String,
    val descripcion: String,
    val pesoUnitarioKg: Double,
    val valorCompraUnitario: Double
)

@Serializable
data class ArticuloCantidadDto(
    val codigoArticulo: String,
    val cantidadActa: Int?
)

@Serializable
data class ArticuloBupasDto(
    val codigoArticulo: String,
    val descripcion: String,
    val cantidadInicial: Int,
    val cantidadActa: Int? = null,
    val pesoUnitarioKg: Double,
    val valorCompraUnitario: Double
)

@Serializable
data class TerceroProvenienteDto(
    val id: String,
    val nit: String,
    val razonSocial: String,
    val tipoBateria: String,
    val cantidadBaterias: Int,
    val telefono: String,
    val correoElectronico: String
)
