package co.com.coexito.bupas.domain.usecase.collection

import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.PrecioArticuloBupas
import co.com.coexito.bupas.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Carga la orden de recolección para HU-006 e integra todas las referencias BUPAS
 * de la lista de precios del cliente con `cantidadInicial = 0` cuando no fueron
 * incluidas en la solicitud original (CA 4).
 */
class PrepareFieldCollectionOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {

    suspend operator fun invoke(numeroSolicitud: String): Result<OrdenRecoleccion> {
        return orderRepository.getOrderBySolicitud(numeroSolicitud).map { order ->
            val priceList = orderRepository.getClientPriceList(order.nitCliente)
                .getOrDefault(emptyList())
            mergePriceListItems(order, priceList)
        }
    }

    suspend fun hasPendingOfflineDraft(numeroSolicitud: String): Boolean =
        orderRepository.hasPendingOfflineDraft(numeroSolicitud)

    private fun mergePriceListItems(
        order: OrdenRecoleccion,
        priceList: List<PrecioArticuloBupas>
    ): OrdenRecoleccion {
        if (priceList.isEmpty()) return order

        val existingCodigos = order.articulos
            .map { it.codigoArticulo.uppercase() }
            .toSet()

        val defaultCantidadActa = if (order.estado.isReadOnlyLocked) 0 else null
        val extraItems = priceList
            .filter { it.codigoArticulo.uppercase() !in existingCodigos }
            .map { precio ->
                ArticuloBupas(
                    codigoArticulo = precio.codigoArticulo,
                    descripcion = precio.descripcion,
                    cantidadInicial = 0,
                    cantidadActa = defaultCantidadActa,
                    pesoUnitarioKg = precio.pesoUnitarioKg,
                    valorCompraUnitario = precio.valorCompraUnitario
                )
            }

        return if (extraItems.isEmpty()) {
            order
        } else {
            order.copy(articulos = order.articulos + extraItems)
        }
    }
}
