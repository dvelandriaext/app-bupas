package co.com.coexito.bupas.domain.usecase.certificate

import co.com.coexito.bupas.domain.model.ActaRespel
import co.com.coexito.bupas.domain.model.CanalActaRespel
import co.com.coexito.bupas.domain.model.DatosGeneradorActa
import co.com.coexito.bupas.domain.model.DatosTransporteOtmActa
import co.com.coexito.bupas.domain.model.EstadoEnvioActa
import co.com.coexito.bupas.domain.model.EstadoGeneracionActa
import co.com.coexito.bupas.domain.model.ItemResiduoActa
import co.com.coexito.bupas.domain.model.NotificacionCorreoActa
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.PlanReintentoActa
import co.com.coexito.bupas.domain.model.ProvenienteActa
import javax.inject.Inject

/**
 * Caso de uso de dominio que construye el modelo oficial del Acta de Recolección y Transporte
 * de Residuos Peligrosos (`[NumeroActa].pdf`) y su notificación por correo electrónico
 * conforme a HU-007 (CA 1, CA 2 y CA 3).
 */
class BuildActaRespelUseCase @Inject constructor() {

    operator fun invoke(
        order: OrdenRecoleccion,
        reintentosEjecutados: Int = 0
    ): ActaRespel? {
        val itemsRecolectados = extractCollectedWasteItems(order)
        val totalUnidades = itemsRecolectados.sumOf { it.cantidad }

        if (order.estado != OrdenStatus.RECOLECCION_REALIZADA || totalUnidades <= 0) {
            return null
        }

        val canal = CanalActaRespel.fromTipoDevolucion(order.tipoDevolucion)
        val numeroActa = order.numeroActaGenerada?.takeIf { it.isNotBlank() }
            ?: buildConsecutivoActa(canal, order.numeroSolicitud)
        val nombreArchivoPdf = "$numeroActa.pdf"
        val fechaHoraGen = order.fechaHoraFinalizacion?.takeIf { it.isNotBlank() }
            ?: "${order.fechaRecoleccion} 10:00:00"
        val fechaDdMmYyyy = formatIsoToDdMmYyyy(fechaHoraGen.substringBefore(" "))

        val estadoGeneracion = order.estadoGeneracionActa ?: EstadoGeneracionActa.GENERADA
        val estadoEnvio = when {
            estadoGeneracion == EstadoGeneracionActa.ERROR_GENERACION -> EstadoEnvioActa.ERROR_ENVIO
            else -> order.estadoEnvioActa ?: EstadoEnvioActa.ENVIADA
        }

        return ActaRespel(
            numeroActa = numeroActa,
            canal = canal,
            nombreArchivoPdf = nombreArchivoPdf,
            numeroSolicitud = order.numeroSolicitud,
            identificadorOm = order.identificadorOm,
            cedi = order.cedi,
            tituloEncabezado = ActaRespel.buildOfficialTitle(numeroActa),
            fechaGeneracionDdMmYyyy = fechaDdMmYyyy,
            fechaHoraGeneracion = fechaHoraGen,
            estadoGeneracion = estadoGeneracion,
            detalleErrorGeneracion = order.detalleErrorActa.takeIf {
                estadoGeneracion == EstadoGeneracionActa.ERROR_GENERACION
            },
            datosGenerador = buildDatosGenerador(order),
            itemsResiduos = itemsRecolectados,
            totalCantidadRecolectada = totalUnidades,
            totalPesoKg = itemsRecolectados.sumOf { it.pesoTotalKg },
            aplicaProvenientes = order.aplicaProvenientes,
            provenientes = buildProvenientes(order),
            observacionesRecoleccion = order.observacionesCampo.ifBlank { order.observacionesIniciales },
            datosTransporteOtm = DatosTransporteOtmActa(
                conductorResponsable = order.conductorNombre,
                cedulaCiudadania = order.conductorCedula,
                firmaDigitalBase64 = order.firmaDigitalBase64,
                placaVehiculo = order.placaVehiculo
            ),
            notificacionCorreo = buildNotificacionCorreo(
                order = order,
                numeroActa = numeroActa,
                nombreArchivoPdf = nombreArchivoPdf,
                fechaDdMmYyyy = fechaDdMmYyyy,
                fechaHoraGen = fechaHoraGen,
                estadoEnvio = estadoEnvio,
                reintentosEjecutados = reintentosEjecutados
            )
        )
    }

    private fun extractCollectedWasteItems(order: OrdenRecoleccion): List<ItemResiduoActa> {
        return order.articulos
            .filter { (it.cantidadActa ?: 0) > 0 }
            .map { articulo ->
                val cantidad = articulo.cantidadActa ?: 0
                ItemResiduoActa(
                    descripcion = articulo.descripcion,
                    referencia = articulo.codigoArticulo,
                    cantidad = cantidad,
                    pesoUnitarioKg = articulo.pesoUnitarioKg,
                    pesoTotalKg = cantidad * articulo.pesoUnitarioKg
                )
            }
    }

    private fun buildDatosGenerador(order: OrdenRecoleccion): DatosGeneradorActa =
        DatosGeneradorActa(
            departamento = order.departamento,
            municipio = order.ciudad,
            razonSocial = order.razonSocialCliente,
            nit = order.nitCliente,
            entregadoPorNombre = order.nombreResponsableEntrega,
            entregadoPorIdentificacion = order.identificacionResponsableEntrega,
            telefono = order.telefonoCliente,
            direccion = order.direccionRecogida,
            correoElectronico = order.correoPrincipalCliente
        )

    private fun buildProvenientes(order: OrdenRecoleccion): List<ProvenienteActa> {
        if (!order.aplicaProvenientes) return emptyList()
        return order.provenientes.map { tercero ->
            ProvenienteActa(
                id = tercero.id,
                nit = tercero.nit,
                razonSocial = tercero.razonSocial,
                tipoBateria = tercero.tipoBateria.label,
                cantidad = tercero.cantidadBaterias,
                telefono = tercero.telefono,
                correoElectronico = tercero.correoElectronico,
                departamento = order.departamento,
                municipio = order.ciudad,
                direccion = order.direccionRecogida
            )
        }
    }

    private fun buildNotificacionCorreo(
        order: OrdenRecoleccion,
        numeroActa: String,
        nombreArchivoPdf: String,
        fechaDdMmYyyy: String,
        fechaHoraGen: String,
        estadoEnvio: EstadoEnvioActa,
        reintentosEjecutados: Int
    ): NotificacionCorreoActa {
        val principal = order.correoPrincipalCliente.trim()
        val alterno = order.correoAlternoCliente?.trim()?.takeIf { it.isNotEmpty() }
        val destinatarios = listOfNotNull(principal.takeIf { it.isNotEmpty() }, alterno).distinct()
        val isErrorEnvio = estadoEnvio == EstadoEnvioActa.ERROR_ENVIO

        return NotificacionCorreoActa(
            asunto = ActaRespel.buildOfficialEmailSubject(numeroActa),
            correoPrincipal = principal,
            correoAlterno = alterno,
            correosDestino = destinatarios,
            cuerpoMensaje = buildEmailBody(order, numeroActa, fechaDdMmYyyy),
            nombreArchivoAdjunto = nombreArchivoPdf,
            estadoEnvio = estadoEnvio,
            fechaHoraEnvio = if (isErrorEnvio) null else fechaHoraGen,
            detalleErrorEnvio = if (isErrorEnvio) {
                order.detalleErrorActa
                    ?: "Fallo en servidor SMTP al enviar notificación. Reintentos automáticos programados cada 2 horas."
            } else {
                null
            },
            planReintento = PlanReintentoActa(
                reintentosEjecutados = reintentosEjecutados,
                habilitadoEnvioManualContingencia = isErrorEnvio
            )
        )
    }

    private fun buildEmailBody(
        order: OrdenRecoleccion,
        numeroActa: String,
        fechaDdMmYyyy: String
    ): String = buildString {
        appendLine("Estimado(a) ${order.razonSocialCliente} (NIT: ${order.nitCliente}),")
        appendLine()
        appendLine(
            "Adjunto encontrará el Comprobante de Recolección y Transporte de Residuos Peligrosos " +
                "N° $numeroActa ($numeroActa.pdf), correspondiente a la solicitud ${order.numeroSolicitud} " +
                "(OM: ${order.identificadorOm}) atendida el $fechaDdMmYyyy."
        )
        appendLine()
        append(ActaRespel.NOTA_ACLARATORIA_PRELIMINAR_OFICIAL)
    }

    companion object {
        fun buildConsecutivoActa(canal: CanalActaRespel, numeroSolicitud: String): String {
            val digits = numeroSolicitud.filter { it.isDigit() }.takeLast(4).padStart(4, '0')
            return "${canal.prefix}-$digits"
        }

        fun formatIsoToDdMmYyyy(rawDate: String): String {
            val parts = rawDate.trim().split("-")
            return if (parts.size == 3 && parts[0].length == 4) {
                "${parts[2].padStart(2, '0')}/${parts[1].padStart(2, '0')}/${parts[0]}"
            } else {
                rawDate
            }
        }
    }
}
