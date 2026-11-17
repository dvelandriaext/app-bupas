package co.com.coexito.bupas.domain.model

import androidx.compose.runtime.Immutable

/**
 * Estados de generación del documento PDF del Acta Respel (HU-007 CA 1, CA 2 y CA 3).
 */
enum class EstadoGeneracionActa(val displayName: String) {
    GENERADA("Generada"),
    ERROR_GENERACION("Error de generación");

    companion object {
        fun fromRaw(raw: String?): EstadoGeneracionActa =
            entries.firstOrNull {
                it.name.equals(raw, ignoreCase = true) ||
                    it.displayName.equals(raw, ignoreCase = true)
            } ?: GENERADA
    }
}

/**
 * Estados de envío por correo electrónico del Acta Respel (HU-007 CA 2 y CA 3).
 */
enum class EstadoEnvioActa(val displayName: String) {
    ENVIADA("Enviada"),
    ERROR_ENVIO("Error de envío");

    companion object {
        fun fromRaw(raw: String?): EstadoEnvioActa =
            entries.firstOrNull {
                it.name.equals(raw, ignoreCase = true) ||
                    it.displayName.equals(raw, ignoreCase = true)
            } ?: ENVIADA
    }
}

/**
 * Canal de devolución que determina el prefijo del consecutivo único del Acta Respel (`INS` o `POS`).
 */
enum class CanalActaRespel(val prefix: String, val label: String) {
    INSTITUCIONAL(prefix = "INS", label = "Institucional"),
    POSCONSUMO(prefix = "POS", label = "Posconsumo");

    companion object {
        fun fromTipoDevolucion(tipoDevolucion: String): CanalActaRespel =
            if (tipoDevolucion.trim().startsWith("POS", ignoreCase = true)) {
                POSCONSUMO
            } else {
                INSTITUCIONAL
            }
    }
}

/**
 * Sección 2 del Modelo Oficial del Acta Respel: Datos del Generador del Residuo.
 */
@Immutable
data class DatosGeneradorActa(
    val departamento: String,
    val municipio: String,
    val razonSocial: String,
    val nit: String,
    val entregadoPorNombre: String,
    val entregadoPorIdentificacion: String,
    val telefono: String,
    val direccion: String,
    val correoElectronico: String
) {
    val entregadoPorResumen: String
        get() = if (entregadoPorIdentificacion.isNotBlank()) {
            "$entregadoPorNombre (CC/NIT: $entregadoPorIdentificacion)"
        } else {
            entregadoPorNombre
        }
}

/**
 * Sección 4 del Modelo Oficial del Acta Respel: Renglón de la grilla de detalle de residuos.
 * Solo incluye referencias cuya cantidad real recolectada ([cantidad]) sea mayor a 0.
 */
@Immutable
data class ItemResiduoActa(
    val descripcion: String,
    val referencia: String,
    val cantidad: Int,
    val pesoUnitarioKg: Double,
    val pesoTotalKg: Double
)

/**
 * Sección 5 del Modelo Oficial del Acta Respel: Desglose independiente de tercero Proveniente.
 */
@Immutable
data class ProvenienteActa(
    val id: String,
    val nit: String,
    val razonSocial: String,
    val tipoBateria: String,
    val cantidad: Int,
    val telefono: String,
    val correoElectronico: String,
    val departamento: String,
    val municipio: String,
    val direccion: String
)

/**
 * Sección 8 del Modelo Oficial del Acta Respel: Datos de transporte de OTM.
 */
@Immutable
data class DatosTransporteOtmActa(
    val conductorResponsable: String,
    val cedulaCiudadania: String,
    val firmaDigitalBase64: String?,
    val placaVehiculo: String
)

/**
 * Plan de reintentos automáticos ante fallo en el envío de correo (HU-007 CA 2:
 * hasta 3 reintentos con intervalo de 2 horas: a las 2h, 4h y 6h).
 */
@Immutable
data class PlanReintentoActa(
    val maxReintentos: Int = MAX_REINTENTOS_OFICIAL,
    val intervaloHoras: Int = INTERVALO_HORAS_OFICIAL,
    val horasProgramadas: List<Int> = HORAS_REINTENTO_OFICIALES,
    val reintentosEjecutados: Int = 0,
    val habilitadoEnvioManualContingencia: Boolean = false
) {
    companion object {
        const val MAX_REINTENTOS_OFICIAL = 3
        const val INTERVALO_HORAS_OFICIAL = 2
        val HORAS_REINTENTO_OFICIALES = listOf(2, 4, 6)
    }
}

/**
 * Trazabilidad y estructura del correo electrónico de notificación del Acta Respel (HU-007 CA 2 y CA 3).
 */
@Immutable
data class NotificacionCorreoActa(
    val asunto: String,
    val correoPrincipal: String,
    val correoAlterno: String?,
    val correosDestino: List<String>,
    val cuerpoMensaje: String,
    val nombreArchivoAdjunto: String,
    val estadoEnvio: EstadoEnvioActa,
    val fechaHoraEnvio: String?,
    val detalleErrorEnvio: String? = null,
    val planReintento: PlanReintentoActa = PlanReintentoActa()
)

/**
 * Modelo de dominio integral del Comprobante de Recolección y Transporte de Residuos Peligrosos
 * (`[NumeroActa].pdf`) y su trazabilidad de generación/envío (HU-007 CA 1, CA 2 y CA 3).
 */
@Immutable
data class ActaRespel(
    val numeroActa: String,
    val canal: CanalActaRespel,
    val nombreArchivoPdf: String,
    val numeroSolicitud: String,
    val identificadorOm: String,
    val cedi: String,
    val tituloEncabezado: String,
    val fechaGeneracionDdMmYyyy: String,
    val fechaHoraGeneracion: String,
    val estadoGeneracion: EstadoGeneracionActa,
    val detalleErrorGeneracion: String? = null,
    val datosGenerador: DatosGeneradorActa,
    val textoResponsabilidadLegal: String = TEXTO_RESPONSABILIDAD_LEGAL_OFICIAL,
    val itemsResiduos: List<ItemResiduoActa>,
    val totalCantidadRecolectada: Int,
    val totalPesoKg: Double,
    val aplicaProvenientes: Boolean,
    val provenientes: List<ProvenienteActa>,
    val observacionesRecoleccion: String,
    val clasificacionRespel: String = CLASIFICACION_RESPEL_Y31_OFICIAL,
    val datosTransporteOtm: DatosTransporteOtmActa,
    val notaAclaratoriaPreliminar: String = NOTA_ACLARATORIA_PRELIMINAR_OFICIAL,
    val notificacionCorreo: NotificacionCorreoActa
) {
    /**
     * Regla HU-007 CA 3: La acción "Ver Acta" para visualizar o descargar el PDF está habilitada
     * únicamente cuando el estado de generación del acta sea [EstadoGeneracionActa.GENERADA].
     */
    val canViewPdf: Boolean
        get() = estadoGeneracion == EstadoGeneracionActa.GENERADA

    companion object {
        const val TEXTO_RESPONSABILIDAD_LEGAL_OFICIAL =
            "COÉXITO realiza la recolección, transporte y almacenamiento de las baterías de plomo-ácido, " +
                "las cuales son posteriormente entregadas a los gestores autorizados para su disposición final."

        const val CLASIFICACION_RESPEL_Y31_OFICIAL =
            "Y31 baterías plomoacido usadas recibido y transportado por coexito s.a.s"

        const val NOTA_ACLARATORIA_PRELIMINAR_OFICIAL =
            "Nota aclaratoria: Las cantidades y pesos registrados en el presente comprobante corresponden a " +
                "datos preliminares capturados en campo y están sujetos a variación tras la validación " +
                "física y pesaje definitivo en el CEDI de Coéxito S.A.S."

        fun buildOfficialTitle(numeroActa: String): String =
            "Sistema de Recolección Logística Reversiva Coéxito S.A.S. - " +
                "Comprobante de Recolección y Transporte de Residuos Peligrosos N° $numeroActa"

        fun buildOfficialEmailSubject(numeroActa: String): String =
            "Acta de Recolección de Residuos Peligrosos – Solicitud $numeroActa"
    }
}
