package co.com.coexito.bupas.data.remote

import co.com.coexito.bupas.data.remote.api.OrderApiService
import co.com.coexito.bupas.data.remote.dto.ArticuloBupasDto
import co.com.coexito.bupas.data.remote.dto.AssignedOrdersResponseDto
import co.com.coexito.bupas.data.remote.dto.OrderDto
import co.com.coexito.bupas.data.remote.dto.SaveFieldCollectionRequestDto
import co.com.coexito.bupas.data.remote.dto.TerceroProvenienteDto
import co.com.coexito.bupas.domain.model.OrdenStatus
import kotlinx.coroutines.delay

/**
 * Implementación simulada de [OrderApiService] para HU-005 y HU-006.
 * Simula latencia de red, filtrado en servidor, persistencia en sesión y generación de número de acta.
 */
class FakeOrderApiService(
    private val networkDelayMillis: Long = DEFAULT_NETWORK_DELAY_MS
) : OrderApiService {

    private val ordersCatalog: MutableList<OrderDto> = buildDummyCatalog().toMutableList()

    override suspend fun getAssignedOrders(
        startDate: String?,
        endDate: String?,
        status: String?,
        clientQuery: String?,
        numeroSolicitud: String?,
        identificadorOm: String?
    ): AssignedOrdersResponseDto {
        if (networkDelayMillis > 0L) {
            delay(networkDelayMillis)
        }

        val filtered = ordersCatalog
            .filter { order ->
                matchesDateRange(order.fechaRecoleccion, startDate, endDate) &&
                    matchesStatus(order.estado, status) &&
                    matchesClient(order, clientQuery) &&
                    matchesContains(order.numeroSolicitud, numeroSolicitud) &&
                    matchesContains(order.identificadorOm, identificadorOm)
            }
            .sortedByDescending { it.fechaRecoleccion }

        return AssignedOrdersResponseDto(
            orders = filtered,
            totalCount = ordersCatalog.size
        )
    }

    override suspend fun getOrderBySolicitud(solicitudId: String): OrderDto {
        if (networkDelayMillis > 0L) {
            delay(networkDelayMillis)
        }
        return ordersCatalog.firstOrNull {
            it.numeroSolicitud.equals(solicitudId, ignoreCase = true)
        } ?: throw NoSuchElementException("No se encontró la orden $solicitudId")
    }

    override suspend fun saveFieldCollection(
        solicitudId: String,
        request: SaveFieldCollectionRequestDto
    ): OrderDto {
        if (networkDelayMillis > 0L) {
            delay(networkDelayMillis)
        }
        val index = ordersCatalog.indexOfFirst {
            it.numeroSolicitud.equals(solicitudId, ignoreCase = true)
        }
        if (index == -1) {
            throw NoSuchElementException("No se encontró la orden $solicitudId")
        }

        val current = ordersCatalog[index]
        val updatedArticulos = current.articulos.map { articulo ->
            val submitted = request.articulos.firstOrNull { it.codigoArticulo == articulo.codigoArticulo }
            articulo.copy(cantidadActa = submitted?.cantidadActa ?: articulo.cantidadActa)
        }

        val finalStatus = if (request.finalizeCollection && request.estado != OrdenStatus.NO_RECOLECTADA.name) {
            OrdenStatus.RECOLECCION_REALIZADA.name
        } else {
            request.estado
        }

        val totalUnidades = updatedArticulos.sumOf { it.cantidadActa ?: 0 }
        val generatedActa = if (finalStatus == OrdenStatus.RECOLECCION_REALIZADA.name && totalUnidades > 0) {
            current.numeroActaGenerada ?: buildActaNumber(current.tipoDevolucion, current.numeroSolicitud)
        } else {
            current.numeroActaGenerada
        }

        val updatedOrder = current.copy(
            estado = finalStatus,
            articulos = updatedArticulos,
            aplicaProvenientes = request.aplicaProvenientes,
            provenientes = request.provenientes,
            detalleNovedad = request.detalleNovedad,
            observacionesCampo = request.observacionesCampo,
            nombreResponsableEntrega = request.nombreResponsableEntrega,
            identificacionResponsableEntrega = request.identificacionResponsableEntrega,
            numeroActaGenerada = generatedActa
        )
        ordersCatalog[index] = updatedOrder
        return updatedOrder
    }

    private fun buildActaNumber(tipoDevolucion: String, numeroSolicitud: String): String {
        val prefix = if (tipoDevolucion.startsWith("POS", ignoreCase = true)) "POS" else "INS"
        val suffix = numeroSolicitud.takeLast(4).padStart(4, '0')
        return "$prefix-$suffix"
    }

    private fun matchesDateRange(
        orderDate: String,
        startDate: String?,
        endDate: String?
    ): Boolean {
        val afterStart = startDate.isNullOrBlank() || orderDate >= startDate
        val beforeEnd = endDate.isNullOrBlank() || orderDate <= endDate
        return afterStart && beforeEnd
    }

    private fun matchesStatus(orderStatus: String, statusFilter: String?): Boolean {
        if (statusFilter.isNullOrBlank()) return true
        return orderStatus.equals(statusFilter, ignoreCase = true)
    }

    private fun matchesClient(order: OrderDto, clientQuery: String?): Boolean {
        if (clientQuery.isNullOrBlank()) return true
        val query = clientQuery.trim()
        return order.razonSocialCliente.contains(query, ignoreCase = true) ||
            order.nitCliente.contains(query, ignoreCase = true)
    }

    private fun matchesContains(value: String, query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        return value.contains(query.trim(), ignoreCase = true)
    }

    companion object {
        const val DEFAULT_NETWORK_DELAY_MS = 350L
    }
}

private fun buildDummyCatalog(): List<OrderDto> = listOf(
    OrderDto(
        numeroSolicitud = "SOL-2026-00148",
        identificadorOm = "OM-889450",
        cedi = "CEDI Cali",
        fechaRecoleccion = "2026-11-15",
        nitCliente = "890.300.211-4",
        razonSocialCliente = "Serviteca El Dorado S.A.S.",
        direccionRecogida = "Av. 3N # 45-12",
        departamento = "Valle del Cauca",
        ciudad = "Cali",
        vendedor = "Carlos Pérez",
        tipoDevolucion = "POS-Posconsumo",
        observacionesIniciales = "Recoger en bodega posterior. Portería solicita cédula.",
        estado = OrdenStatus.ORDEN_ASIGNADA.name,
        articulos = listOf(
            ArticuloBupasDto(
                codigoArticulo = "BUP-AUTO-42",
                descripcion = "Batería Usada Auto 42R",
                cantidadInicial = 15,
                cantidadActa = null,
                pesoUnitarioKg = 14.5,
                valorCompraUnitario = 55000.0
            ),
            ArticuloBupasDto(
                codigoArticulo = "BUP-MOTO-12",
                descripcion = "Batería Usada Moto 12V",
                cantidadInicial = 8,
                cantidadActa = null,
                pesoUnitarioKg = 4.0,
                valorCompraUnitario = 18000.0
            )
        )
    ),
    OrderDto(
        numeroSolicitud = "SOL-2026-00145",
        identificadorOm = "OM-889431",
        cedi = "CEDI Cali",
        fechaRecoleccion = "2026-11-14",
        nitCliente = "900.512.334-8",
        razonSocialCliente = "Lubricantes y Baterías del Sur S.A.S.",
        direccionRecogida = "Calle 25 # 8-19",
        departamento = "Valle del Cauca",
        ciudad = "Jamundí",
        vendedor = "Marta Lucía Díaz",
        tipoDevolucion = "POS-Posconsumo",
        observacionesIniciales = "Confirmar entrega con jefe de patio.",
        estado = OrdenStatus.EN_ATENCION.name,
        articulos = listOf(
            ArticuloBupasDto(
                codigoArticulo = "BUP-PESADA-31",
                descripcion = "Batería Usada Grupo 31H",
                cantidadInicial = 10,
                cantidadActa = 10,
                pesoUnitarioKg = 26.0,
                valorCompraUnitario = 95000.0
            )
        ),
        aplicaProvenientes = true,
        provenientes = listOf(
            TerceroProvenienteDto(
                id = "PROV-001",
                nit = "805.019.223-1",
                razonSocial = "Taller Automotriz El Cruce",
                tipoBateria = "AUTO",
                cantidadBaterias = 6,
                telefono = "3154829011",
                correoElectronico = "contacto@tallerelcruce.com"
            )
        )
    ),
    OrderDto(
        numeroSolicitud = "SOL-2026-00142",
        identificadorOm = "OM-889412",
        cedi = "CEDI Cali",
        fechaRecoleccion = "2026-11-11",
        nitCliente = "805.004.712-9",
        razonSocialCliente = "Centro Automotriz Norte Ltda.",
        direccionRecogida = "Cra. 1 # 62-80",
        departamento = "Valle del Cauca",
        ciudad = "Cali",
        vendedor = "Carlos Pérez",
        tipoDevolucion = "INS-Institucional",
        observacionesIniciales = "Cliente entregó parcialmente por falta de estiba.",
        estado = OrdenStatus.RECOLECCION_PARCIAL.name,
        articulos = listOf(
            ArticuloBupasDto(
                codigoArticulo = "BUP-AUTO-34",
                descripcion = "Batería Usada Auto 34",
                cantidadInicial = 18,
                cantidadActa = 12,
                pesoUnitarioKg = 16.0,
                valorCompraUnitario = 62000.0
            )
        )
    ),
    OrderDto(
        numeroSolicitud = "SOL-2026-00139",
        identificadorOm = "OM-889390",
        cedi = "CEDI Cali",
        fechaRecoleccion = "2026-11-09",
        nitCliente = "900.412.889-1",
        razonSocialCliente = "Autocentro del Valle Ltda.",
        direccionRecogida = "Cra. 15 # 22-40",
        departamento = "Valle del Cauca",
        ciudad = "Palmira",
        vendedor = "Laura Gómez",
        tipoDevolucion = "INS-Institucional",
        observacionesIniciales = "Entrega con encargado de almacén",
        estado = OrdenStatus.RECOLECCION_REALIZADA.name,
        numeroActaGenerada = "INS-0048",
        articulos = listOf(
            ArticuloBupasDto(
                codigoArticulo = "BUP-AUTO-34",
                descripcion = "Batería Usada Auto 34",
                cantidadInicial = 20,
                cantidadActa = 20,
                pesoUnitarioKg = 16.0,
                valorCompraUnitario = 62000.0
            )
        )
    ),
    OrderDto(
        numeroSolicitud = "SOL-2026-00133",
        identificadorOm = "OM-889344",
        cedi = "CEDI Cali",
        fechaRecoleccion = "2026-11-04",
        nitCliente = "890.901.552-3",
        razonSocialCliente = "Transportes Carga Segura S.A.",
        direccionRecogida = "Km 4 Vía Yumbo - Menga",
        departamento = "Valle del Cauca",
        ciudad = "Yumbo",
        vendedor = "Laura Gómez",
        tipoDevolucion = "POS-Posconsumo",
        observacionesIniciales = "Novedad: bodega cerrada al momento de la visita.",
        estado = OrdenStatus.NO_RECOLECTADA.name,
        articulos = listOf(
            ArticuloBupasDto(
                codigoArticulo = "BUP-SCRAP-01",
                descripcion = "Chatarra Plomo-Ácido Scrap",
                cantidadInicial = 12,
                cantidadActa = 0,
                pesoUnitarioKg = 18.0,
                valorCompraUnitario = 48000.0
            )
        )
    ),
    OrderDto(
        numeroSolicitud = "SOL-2026-00118",
        identificadorOm = "OM-889205",
        cedi = "CEDI Cali",
        fechaRecoleccion = "2026-10-08",
        nitCliente = "800.221.904-5",
        razonSocialCliente = "Distribuidora Eléctrica Pacífico",
        direccionRecogida = "Calle 13 # 34-10",
        departamento = "Valle del Cauca",
        ciudad = "Cali",
        vendedor = "Carlos Pérez",
        tipoDevolucion = "POS-Posconsumo",
        observacionesIniciales = "Recolección de cierre mensual.",
        estado = OrdenStatus.RECOLECCION_REALIZADA.name,
        numeroActaGenerada = "POS-0112",
        articulos = listOf(
            ArticuloBupasDto(
                codigoArticulo = "BUP-AUTO-42",
                descripcion = "Batería Usada Auto 42R",
                cantidadInicial = 25,
                cantidadActa = 25,
                pesoUnitarioKg = 14.5,
                valorCompraUnitario = 55000.0
            )
        )
    )
)
