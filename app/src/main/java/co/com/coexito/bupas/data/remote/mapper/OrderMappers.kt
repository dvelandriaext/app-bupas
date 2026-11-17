package co.com.coexito.bupas.data.remote.mapper

import co.com.coexito.bupas.data.remote.dto.ArticuloBupasDto
import co.com.coexito.bupas.data.remote.dto.ArticuloCantidadDto
import co.com.coexito.bupas.data.remote.dto.AssignedOrdersResponseDto
import co.com.coexito.bupas.data.remote.dto.OrderDto
import co.com.coexito.bupas.data.remote.dto.PrecioArticuloBupasDto
import co.com.coexito.bupas.data.remote.dto.SaveFieldCollectionRequestDto
import co.com.coexito.bupas.data.remote.dto.TerceroProvenienteDto
import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.AssignedOrdersResult
import co.com.coexito.bupas.domain.model.EstadoEnvioActa
import co.com.coexito.bupas.domain.model.EstadoGeneracionActa
import co.com.coexito.bupas.domain.model.FieldCollectionSubmission
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.PrecioArticuloBupas
import co.com.coexito.bupas.domain.model.TerceroProveniente
import co.com.coexito.bupas.domain.model.TipoBateriaProveniente

fun AssignedOrdersResponseDto.toDomain(
    pendingSyncIds: Set<String> = emptySet()
): AssignedOrdersResult = AssignedOrdersResult(
    orders = orders.map { dto ->
        dto.toDomain(isPendingSync = dto.numeroSolicitud.uppercase() in pendingSyncIds)
    },
    totalCount = totalCount
)

fun OrderDto.toDomain(isPendingSync: Boolean = false): OrdenRecoleccion = OrdenRecoleccion(
    numeroSolicitud = numeroSolicitud,
    identificadorOm = identificadorOm,
    cedi = cedi,
    fechaRecoleccion = fechaRecoleccion,
    nitCliente = nitCliente,
    razonSocialCliente = razonSocialCliente,
    direccionRecogida = direccionRecogida,
    departamento = departamento,
    ciudad = ciudad,
    vendedor = vendedor,
    tipoDevolucion = tipoDevolucion,
    observacionesIniciales = observacionesIniciales,
    estado = parseOrdenStatus(estado),
    articulos = articulos.map { it.toDomain() },
    aplicaProvenientes = aplicaProvenientes,
    provenientes = provenientes.map { it.toDomain() },
    numeroActaGenerada = numeroActaGenerada,
    detalleNovedad = detalleNovedad,
    observacionesCampo = observacionesCampo,
    nombreResponsableEntrega = nombreResponsableEntrega,
    identificacionResponsableEntrega = identificacionResponsableEntrega,
    fechaHoraInicioAtencion = fechaHoraInicioAtencion,
    fechaHoraFinalizacion = fechaHoraFinalizacion,
    firmaDigitalBase64 = firmaDigitalBase64,
    telefonoCliente = telefonoCliente,
    correoPrincipalCliente = correoPrincipalCliente,
    correoAlternoCliente = correoAlternoCliente,
    conductorNombre = conductorNombre,
    conductorCedula = conductorCedula,
    placaVehiculo = placaVehiculo,
    estadoGeneracionActa = estadoGeneracionActa?.let { EstadoGeneracionActa.fromRaw(it) },
    estadoEnvioActa = estadoEnvioActa?.let { EstadoEnvioActa.fromRaw(it) },
    detalleErrorActa = detalleErrorActa,
    isPendingSync = isPendingSync
)

fun FieldCollectionSubmission.toDto(): SaveFieldCollectionRequestDto = SaveFieldCollectionRequestDto(
    estado = estado.name,
    articulos = articulos.map { ArticuloCantidadDto(it.codigoArticulo, it.cantidadActa) },
    aplicaProvenientes = aplicaProvenientes,
    provenientes = provenientes.map { it.toDto() },
    detalleNovedad = detalleNovedad,
    observacionesCampo = observacionesCampo,
    nombreResponsableEntrega = nombreResponsableEntrega,
    identificacionResponsableEntrega = identificacionResponsableEntrega,
    finalizeCollection = finalizeCollection,
    fechaHoraInicioAtencion = fechaHoraInicioAtencion,
    fechaHoraFinalizacion = fechaHoraFinalizacion,
    firmaDigitalBase64 = firmaDigitalBase64
)

fun PrecioArticuloBupasDto.toDomain(): PrecioArticuloBupas = PrecioArticuloBupas(
    codigoArticulo = codigoArticulo,
    descripcion = descripcion,
    pesoUnitarioKg = pesoUnitarioKg,
    valorCompraUnitario = valorCompraUnitario
)

fun ArticuloBupas.toDto(): ArticuloBupasDto = ArticuloBupasDto(
    codigoArticulo = codigoArticulo,
    descripcion = descripcion,
    cantidadInicial = cantidadInicial,
    cantidadActa = cantidadActa,
    pesoUnitarioKg = pesoUnitarioKg,
    valorCompraUnitario = valorCompraUnitario
)

fun TerceroProveniente.toDto(): TerceroProvenienteDto = TerceroProvenienteDto(
    id = id,
    nit = nit,
    razonSocial = razonSocial,
    tipoBateria = tipoBateria.name,
    cantidadBaterias = cantidadBaterias,
    telefono = telefono,
    correoElectronico = correoElectronico
)

fun ArticuloBupasDto.toDomain(): ArticuloBupas = ArticuloBupas(
    codigoArticulo = codigoArticulo,
    descripcion = descripcion,
    cantidadInicial = cantidadInicial,
    cantidadActa = cantidadActa,
    pesoUnitarioKg = pesoUnitarioKg,
    valorCompraUnitario = valorCompraUnitario
)

fun TerceroProvenienteDto.toDomain(): TerceroProveniente = TerceroProveniente(
    id = id,
    nit = nit,
    razonSocial = razonSocial,
    tipoBateria = parseTipoBateria(tipoBateria),
    cantidadBaterias = cantidadBaterias,
    telefono = telefono,
    correoElectronico = correoElectronico
)

internal fun parseOrdenStatus(rawStatus: String): OrdenStatus {
    return OrdenStatus.entries.firstOrNull {
        it.name.equals(rawStatus, ignoreCase = true) ||
            it.displayName.equals(rawStatus, ignoreCase = true)
    } ?: OrdenStatus.ORDEN_ASIGNADA
}

private fun parseTipoBateria(rawType: String): TipoBateriaProveniente {
    return TipoBateriaProveniente.entries.firstOrNull {
        it.name.equals(rawType, ignoreCase = true) ||
            it.label.equals(rawType, ignoreCase = true)
    } ?: TipoBateriaProveniente.AUTO
}
