package co.com.coexito.bupas.data.remote

import co.com.coexito.bupas.data.remote.api.OrderApiService
import co.com.coexito.bupas.data.remote.dto.ArticuloBupasDto
import co.com.coexito.bupas.data.remote.dto.ArticuloCantidadDto
import co.com.coexito.bupas.data.remote.dto.AssignedOrdersResponseDto
import co.com.coexito.bupas.data.remote.dto.OrderDto
import co.com.coexito.bupas.data.remote.dto.PrecioArticuloBupasDto
import co.com.coexito.bupas.data.remote.dto.SaveFieldCollectionRequestDto
import co.com.coexito.bupas.data.remote.dto.TerceroProvenienteDto
import co.com.coexito.bupas.data.remote.mapper.parseOrdenStatus
import co.com.coexito.bupas.domain.model.EstadoEnvioActa
import co.com.coexito.bupas.domain.model.EstadoGeneracionActa
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
    private val clientPriceCatalog: List<PrecioArticuloBupasDto> = buildClientPriceCatalog()

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

    override suspend fun getClientPriceList(nitCliente: String): List<PrecioArticuloBupasDto> {
        if (networkDelayMillis > 0L) {
            delay(networkDelayMillis)
        }
        return clientPriceCatalog
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
        if (parseOrdenStatus(current.estado).isReadOnlyLocked) {
            throw IllegalStateException(
                "La orden ${current.numeroSolicitud} ya fue cerrada y se encuentra bloqueada en modo solo lectura."
            )
        }

        val finalStatus = resolveFinalStatus(request)
        val updatedOrder = if (finalStatus == OrdenStatus.NO_RECOLECTADA.name) {
            buildNoRecolectadaOrder(current, request)
        } else {
            buildPhysicalCollectionOrder(current, request, finalStatus)
        }
        ordersCatalog[index] = updatedOrder
        return updatedOrder
    }

    override suspend fun retryActaContingency(solicitudId: String): OrderDto {
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
        val recovered = current.copy(
            numeroActaGenerada = current.numeroActaGenerada
                ?: buildActaNumber(current.tipoDevolucion, current.numeroSolicitud),
            estadoGeneracionActa = EstadoGeneracionActa.GENERADA.name,
            estadoEnvioActa = EstadoEnvioActa.ENVIADA.name,
            detalleErrorActa = null
        )
        ordersCatalog[index] = recovered
        return recovered
    }

    fun simulateActaContingencyState(
        solicitudId: String,
        estadoGeneracion: EstadoGeneracionActa,
        estadoEnvio: EstadoEnvioActa,
        detalleError: String
    ) {
        val index = ordersCatalog.indexOfFirst {
            it.numeroSolicitud.equals(solicitudId, ignoreCase = true)
        }
        if (index != -1) {
            ordersCatalog[index] = ordersCatalog[index].copy(
                estadoGeneracionActa = estadoGeneracion.name,
                estadoEnvioActa = estadoEnvio.name,
                detalleErrorActa = detalleError
            )
        }
    }

    private fun resolveFinalStatus(request: SaveFieldCollectionRequestDto): String {
        return if (request.finalizeCollection && request.estado != OrdenStatus.NO_RECOLECTADA.name) {
            OrdenStatus.RECOLECCION_REALIZADA.name
        } else {
            request.estado
        }
    }

    private fun buildNoRecolectadaOrder(
        current: OrderDto,
        request: SaveFieldCollectionRequestDto
    ): OrderDto = current.copy(
        estado = OrdenStatus.NO_RECOLECTADA.name,
        articulos = current.articulos.map { it.copy(cantidadActa = 0) },
        aplicaProvenientes = false,
        provenientes = emptyList(),
        numeroActaGenerada = null,
        detalleNovedad = request.detalleNovedad,
        observacionesCampo = request.observacionesCampo,
        nombreResponsableEntrega = "",
        identificacionResponsableEntrega = "",
        fechaHoraInicioAtencion = current.fechaHoraInicioAtencion ?: request.fechaHoraInicioAtencion,
        fechaHoraFinalizacion = request.fechaHoraFinalizacion ?: current.fechaHoraFinalizacion,
        firmaDigitalBase64 = null
    )

    private fun buildPhysicalCollectionOrder(
        current: OrderDto,
        request: SaveFieldCollectionRequestDto,
        finalStatus: String
    ): OrderDto {
        val updatedArticulos = upsertSubmittedArticulos(current.articulos, request.articulos)
        val totalUnidades = updatedArticulos.sumOf { it.cantidadActa ?: 0 }
        val generatedActa = when {
            finalStatus == OrdenStatus.RECOLECCION_REALIZADA.name && totalUnidades > 0 ->
                current.numeroActaGenerada ?: buildActaNumber(current.tipoDevolucion, current.numeroSolicitud)
            finalStatus == OrdenStatus.RECOLECCION_REALIZADA.name -> null
            else -> current.numeroActaGenerada
        }
        val estadoGen = if (generatedActa != null) EstadoGeneracionActa.GENERADA.name else null
        val estadoEnv = if (generatedActa != null) EstadoEnvioActa.ENVIADA.name else null

        return current.copy(
            estado = finalStatus,
            articulos = updatedArticulos,
            aplicaProvenientes = request.aplicaProvenientes,
            provenientes = if (request.aplicaProvenientes) request.provenientes else emptyList(),
            detalleNovedad = null,
            observacionesCampo = request.observacionesCampo,
            nombreResponsableEntrega = request.nombreResponsableEntrega,
            identificacionResponsableEntrega = request.identificacionResponsableEntrega,
            numeroActaGenerada = generatedActa,
            fechaHoraInicioAtencion = current.fechaHoraInicioAtencion ?: request.fechaHoraInicioAtencion,
            fechaHoraFinalizacion = request.fechaHoraFinalizacion ?: current.fechaHoraFinalizacion,
            firmaDigitalBase64 = request.firmaDigitalBase64 ?: current.firmaDigitalBase64,
            estadoGeneracionActa = estadoGen,
            estadoEnvioActa = estadoEnv,
            detalleErrorActa = null
        )
    }

    private fun upsertSubmittedArticulos(
        existingArticulos: List<ArticuloBupasDto>,
        submittedArticulos: List<ArticuloCantidadDto>
    ): List<ArticuloBupasDto> {
        val submittedByCode = submittedArticulos.associateBy { it.codigoArticulo.uppercase() }
        val existingCodes = existingArticulos.map { it.codigoArticulo.uppercase() }.toSet()

        val updatedExisting = existingArticulos.map { articulo ->
            val key = articulo.codigoArticulo.uppercase()
            if (key in submittedByCode) {
                articulo.copy(cantidadActa = submittedByCode.getValue(key).cantidadActa)
            } else {
                articulo
            }
        }

        val addedFromCatalog = submittedArticulos
            .filter { it.codigoArticulo.uppercase() !in existingCodes }
            .map { submitted ->
                val meta = findCatalogMetadata(submitted.codigoArticulo)
                ArticuloBupasDto(
                    codigoArticulo = submitted.codigoArticulo,
                    descripcion = meta?.descripcion ?: submitted.codigoArticulo,
                    cantidadInicial = 0,
                    cantidadActa = submitted.cantidadActa,
                    pesoUnitarioKg = meta?.pesoUnitarioKg ?: 0.0,
                    valorCompraUnitario = meta?.valorCompraUnitario ?: 0.0
                )
            }

        return updatedExisting + addedFromCatalog
    }

    private fun findCatalogMetadata(codigoArticulo: String): PrecioArticuloBupasDto? {
        return (clientPriceCatalog + EXTRA_REFERENCE_CATALOG).firstOrNull {
            it.codigoArticulo.equals(codigoArticulo, ignoreCase = true)
        }
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

        private val EXTRA_REFERENCE_CATALOG = listOf(
            PrecioArticuloBupasDto(
                codigoArticulo = "BUP-SCRAP-01",
                descripcion = "Chatarra Plomo-Ácido Scrap",
                pesoUnitarioKg = 18.0,
                valorCompraUnitario = 48000.0
            )
        )
    }
}

private fun buildClientPriceCatalog(): List<PrecioArticuloBupasDto> = listOf(
    PrecioArticuloBupasDto(
        codigoArticulo = "BUP-AUTO-42",
        descripcion = "Batería Usada Auto 42R",
        pesoUnitarioKg = 14.5,
        valorCompraUnitario = 55000.0
    ),
    PrecioArticuloBupasDto(
        codigoArticulo = "BUP-AUTO-34",
        descripcion = "Batería Usada Auto 34",
        pesoUnitarioKg = 16.0,
        valorCompraUnitario = 62000.0
    ),
    PrecioArticuloBupasDto(
        codigoArticulo = "BUP-MOTO-12",
        descripcion = "Batería Usada Moto 12V",
        pesoUnitarioKg = 4.0,
        valorCompraUnitario = 18000.0
    ),
    PrecioArticuloBupasDto(
        codigoArticulo = "BUP-PESADA-31",
        descripcion = "Batería Usada Grupo 31H",
        pesoUnitarioKg = 26.0,
        valorCompraUnitario = 95000.0
    )
)

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
        fechaHoraInicioAtencion = "2026-11-14 08:35:00",
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
        fechaHoraInicioAtencion = "2026-11-11 10:15:00",
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
        observacionesCampo = "Recolección completa verificada en almacén.",
        nombreResponsableEntrega = "Andrés Felipe Mora",
        identificacionResponsableEntrega = "1113645890",
        fechaHoraInicioAtencion = "2026-11-09 09:10:00",
        fechaHoraFinalizacion = "2026-11-09 09:52:00",
        firmaDigitalBase64 = "U0lHTkFUVVJFX0lOU18wMDQ4",
        telefonoCliente = "3157401122",
        correoPrincipalCliente = "ambiental@autocentrovalle.com.co",
        correoAlternoCliente = "gerencia@autocentrovalle.com.co",
        estadoGeneracionActa = EstadoGeneracionActa.GENERADA.name,
        estadoEnvioActa = EstadoEnvioActa.ENVIADA.name,
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
        detalleNovedad = "Establecimiento cerrado al momento de la visita; vigilancia solicita reprogramar.",
        fechaHoraFinalizacion = "2026-11-04 11:20:00",
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
        observacionesCampo = "Entrega mensual completa sin novedades.",
        nombreResponsableEntrega = "Diana Marcela Ríos",
        identificacionResponsableEntrega = "66982341",
        fechaHoraInicioAtencion = "2026-10-08 14:05:00",
        fechaHoraFinalizacion = "2026-10-08 14:50:00",
        firmaDigitalBase64 = "U0lHTkFUVVJFX1BPU18wMTEy",
        telefonoCliente = "3189023344",
        correoPrincipalCliente = "logistica@electricapacifico.com.co",
        correoAlternoCliente = null,
        estadoGeneracionActa = EstadoGeneracionActa.GENERADA.name,
        estadoEnvioActa = EstadoEnvioActa.ENVIADA.name,
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
