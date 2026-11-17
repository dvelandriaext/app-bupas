package co.com.coexito.bupas.domain.usecase.certificate

import co.com.coexito.bupas.domain.model.ActaRespel
import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.CanalActaRespel
import co.com.coexito.bupas.domain.model.EstadoEnvioActa
import co.com.coexito.bupas.domain.model.EstadoGeneracionActa
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.TerceroProveniente
import co.com.coexito.bupas.domain.model.TipoBateriaProveniente
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de dominio trazadas estrictamente a los Criterios de Aceptación
 * CA 1, CA 2 y CA 3 de HU-007 y al Modelo Oficial del Acta Respel.
 */
class BuildActaRespelUseCaseTest {

    private val buildActaRespelUseCase = BuildActaRespelUseCase()

    // HU-007 CA 1: Estructura oficial de 9 secciones del Acta PDF, consecutivo INS/POS, fecha DD/MM/YYYY y filtro cantidadActa > 0
    @Test
    fun `CA1 genera acta oficial con las 9 secciones del modelo y excluye referencias con cantidadActa igual a cero`() {
        val order = sampleClosedOrder(
            tipoDevolucion = "POS-Posconsumo",
            numeroActaGenerada = "POS-0148",
            articulos = listOf(
                ArticuloBupas(
                    codigoArticulo = "BUP-AUTO-42",
                    descripcion = "Batería Usada Auto 42R",
                    cantidadInicial = 15,
                    cantidadActa = 12,
                    pesoUnitarioKg = 14.5,
                    valorCompraUnitario = 55000.0
                ),
                ArticuloBupas(
                    codigoArticulo = "BUP-MOTO-12",
                    descripcion = "Batería Usada Moto 12V",
                    cantidadInicial = 8,
                    cantidadActa = 0,
                    pesoUnitarioKg = 4.0,
                    valorCompraUnitario = 18000.0
                )
            )
        )

        val acta = checkNotNull(buildActaRespelUseCase(order))

        // 1. Encabezado formal, fecha DD/MM/YYYY, consecutivo POS-XXXX y archivo [NumeroActa].pdf
        assertEquals("POS-0148", acta.numeroActa)
        assertEquals(CanalActaRespel.POSCONSUMO, acta.canal)
        assertEquals("POS-0148.pdf", acta.nombreArchivoPdf)
        assertEquals("15/11/2026", acta.fechaGeneracionDdMmYyyy)
        assertEquals(
            "Sistema de Recolección Logística Reversiva Coéxito S.A.S. - " +
                "Comprobante de Recolección y Transporte de Residuos Peligrosos N° POS-0148",
            acta.tituloEncabezado
        )

        // 2. Datos del Generador del Residuo
        assertEquals("Valle del Cauca", acta.datosGenerador.departamento)
        assertEquals("Cali", acta.datosGenerador.municipio)
        assertEquals("Serviteca El Dorado S.A.S.", acta.datosGenerador.razonSocial)
        assertEquals("890.300.211-4", acta.datosGenerador.nit)
        assertEquals("Andrés Felipe Mora", acta.datosGenerador.entregadoPorNombre)
        assertEquals("1113645890", acta.datosGenerador.entregadoPorIdentificacion)
        assertEquals("3157401122", acta.datosGenerador.telefono)
        assertEquals("Av. 3N # 45-12", acta.datosGenerador.direccion)
        assertEquals("ambiental@eldorado.com.co", acta.datosGenerador.correoElectronico)

        // 3. Texto legal de responsabilidad de Coéxito
        assertEquals(ActaRespel.TEXTO_RESPONSABILIDAD_LEGAL_OFICIAL, acta.textoResponsabilidadLegal)

        // 4. Grilla de detalle de residuos (únicamente referencias con cantidadActa > 0) y totales
        assertEquals(1, acta.itemsResiduos.size)
        assertEquals("BUP-AUTO-42", acta.itemsResiduos.first().referencia)
        assertEquals(12, acta.itemsResiduos.first().cantidad)
        assertEquals(174.0, acta.itemsResiduos.first().pesoTotalKg, 0.001)
        assertEquals(12, acta.totalCantidadRecolectada)
        assertEquals(174.0, acta.totalPesoKg, 0.001)

        // 6, 7, 8, 9. Observaciones, Clasificación Y31 fija, Transporte OTM y Nota preliminar CEDI
        assertEquals("Entrega verificada en bodega.", acta.observacionesRecoleccion)
        assertEquals(
            "Y31 baterías plomoacido usadas recibido y transportado por coexito s.a.s",
            acta.clasificacionRespel
        )
        assertEquals("Juan Carlos Ramírez", acta.datosTransporteOtm.conductorResponsable)
        assertEquals("1144089231", acta.datosTransporteOtm.cedulaCiudadania)
        assertEquals("WXY-482", acta.datosTransporteOtm.placaVehiculo)
        assertEquals("U0lHTkFUVVJFX0JBU0U2NA==", acta.datosTransporteOtm.firmaDigitalBase64)
        assertEquals(ActaRespel.NOTA_ACLARATORIA_PRELIMINAR_OFICIAL, acta.notaAclaratoriaPreliminar)
    }

    // HU-007 CA 1: No generar Acta cuando la cantidad total recolectada es 0 o el estado es NO_RECOLECTADA
    @Test
    fun `CA1 no genera acta cuando la orden cierra con cantidad total cero o en estado NO_RECOLECTADA`() {
        val zeroQuantityOrder = sampleClosedOrder(
            articulos = listOf(
                ArticuloBupas(
                    codigoArticulo = "BUP-AUTO-42",
                    descripcion = "Batería Usada Auto 42R",
                    cantidadInicial = 10,
                    cantidadActa = 0,
                    pesoUnitarioKg = 14.5,
                    valorCompraUnitario = 55000.0
                )
            )
        )
        val noRecolectadaOrder = sampleClosedOrder(estado = OrdenStatus.NO_RECOLECTADA)

        assertNull(buildActaRespelUseCase(zeroQuantityOrder))
        assertNull(buildActaRespelUseCase(noRecolectadaOrder))
    }

    // HU-007 CA 1 (Sección 5 del modelo): Desglose independiente de terceros Provenientes
    @Test
    fun `CA1 desglosa informacion independiente por cada tercero proveniente cuando aplicaProvenientes es verdadero`() {
        val order = sampleClosedOrder(
            tipoDevolucion = "INS-Institucional",
            numeroActaGenerada = null,
            aplicaProvenientes = true,
            provenientes = listOf(
                TerceroProveniente(
                    id = "PROV-1",
                    nit = "805.019.223-1",
                    razonSocial = "Taller Automotriz El Cruce",
                    tipoBateria = TipoBateriaProveniente.AUTO,
                    cantidadBaterias = 6,
                    telefono = "3154829011",
                    correoElectronico = "contacto@tallerelcruce.com"
                ),
                TerceroProveniente(
                    id = "PROV-2",
                    nit = "900.112.445-9",
                    razonSocial = "Motos del Valle S.A.S.",
                    tipoBateria = TipoBateriaProveniente.MOTO,
                    cantidadBaterias = 4,
                    telefono = "3169981122",
                    correoElectronico = "logistica@motosdelvalle.com"
                )
            )
        )

        val acta = checkNotNull(buildActaRespelUseCase(order))

        assertEquals(CanalActaRespel.INSTITUCIONAL, acta.canal)
        assertEquals("INS-0148", acta.numeroActa)
        assertTrue(acta.aplicaProvenientes)
        assertEquals(2, acta.provenientes.size)
        assertEquals("Taller Automotriz El Cruce", acta.provenientes[0].razonSocial)
        assertEquals("Auto", acta.provenientes[0].tipoBateria)
        assertEquals(6, acta.provenientes[0].cantidad)
        assertEquals("Motos del Valle S.A.S.", acta.provenientes[1].razonSocial)
        assertEquals("Moto", acta.provenientes[1].tipoBateria)
        assertEquals(4, acta.provenientes[1].cantidad)
    }

    // HU-007 CA 2: Asunto oficial de correo, destinatarios (Principal + Alterno opcional) y caso sin correo alterno
    @Test
    fun `CA2 construye correo con asunto oficial y envia a correo principal y alterno o solo al principal si no existe alterno`() {
        val orderWithAlterno = sampleClosedOrder(
            numeroActaGenerada = "INS-0048",
            correoPrincipal = "ambiental@eldorado.com.co",
            correoAlterno = "gerencia@eldorado.com.co"
        )
        val orderWithoutAlterno = sampleClosedOrder(
            numeroActaGenerada = "POS-0112",
            correoPrincipal = "ambiental@eldorado.com.co",
            correoAlterno = null
        )

        val actaBoth = checkNotNull(buildActaRespelUseCase(orderWithAlterno))
        val actaSingle = checkNotNull(buildActaRespelUseCase(orderWithoutAlterno))

        assertEquals(
            "Acta de Recolección de Residuos Peligrosos – Solicitud INS-0048",
            actaBoth.notificacionCorreo.asunto
        )
        assertEquals(
            listOf("ambiental@eldorado.com.co", "gerencia@eldorado.com.co"),
            actaBoth.notificacionCorreo.correosDestino
        )
        assertEquals("INS-0048.pdf", actaBoth.notificacionCorreo.nombreArchivoAdjunto)
        assertTrue(actaBoth.notificacionCorreo.cuerpoMensaje.contains("Serviteca El Dorado S.A.S."))
        assertTrue(actaBoth.notificacionCorreo.cuerpoMensaje.contains("OM-889450"))

        assertEquals(
            listOf("ambiental@eldorado.com.co"),
            actaSingle.notificacionCorreo.correosDestino
        )
        assertNull(actaSingle.notificacionCorreo.correoAlterno)
    }

    // HU-007 CA 2: Fallo en envío registra ERROR_ENVIO sin revertir RECOLECCION_REALIZADA y programa 3 reintentos (2h, 4h, 6h)
    @Test
    fun `CA2 ante fallo de envio mantiene acta generada y orden cerrada programando 3 reintentos a las 2h 4h y 6h`() {
        val orderWithEmailError = sampleClosedOrder(
            numeroActaGenerada = "INS-0048",
            estadoGeneracion = EstadoGeneracionActa.GENERADA,
            estadoEnvio = EstadoEnvioActa.ERROR_ENVIO,
            detalleError = "Timeout SMTP al conectar con servidor de correo."
        )

        val acta = checkNotNull(buildActaRespelUseCase(orderWithEmailError))

        assertEquals(OrdenStatus.RECOLECCION_REALIZADA, orderWithEmailError.estado)
        assertEquals(EstadoGeneracionActa.GENERADA, acta.estadoGeneracion)
        assertTrue(acta.canViewPdf)
        assertEquals(EstadoEnvioActa.ERROR_ENVIO, acta.notificacionCorreo.estadoEnvio)
        assertNotNull(acta.notificacionCorreo.detalleErrorEnvio)
        assertEquals(3, acta.notificacionCorreo.planReintento.maxReintentos)
        assertEquals(2, acta.notificacionCorreo.planReintento.intervaloHoras)
        assertEquals(listOf(2, 4, 6), acta.notificacionCorreo.planReintento.horasProgramadas)
        assertTrue(acta.notificacionCorreo.planReintento.habilitadoEnvioManualContingencia)
    }

    // HU-007 CA 3: Habilitación de "Ver Acta" exclusivamente cuando estadoGeneracion == GENERADA
    @Test
    fun `CA3 habilita Ver Acta unicamente cuando estadoGeneracion es Generada y lo deshabilita en Error de generacion`() {
        val generatedOrder = sampleClosedOrder(
            estadoGeneracion = EstadoGeneracionActa.GENERADA
        )
        val failedGenerationOrder = sampleClosedOrder(
            estadoGeneracion = EstadoGeneracionActa.ERROR_GENERACION,
            estadoEnvio = EstadoEnvioActa.ERROR_ENVIO,
            detalleError = "Error renderizando plantilla PDF en motor documental."
        )

        val generatedActa = checkNotNull(buildActaRespelUseCase(generatedOrder))
        val failedActa = checkNotNull(buildActaRespelUseCase(failedGenerationOrder))

        assertTrue(generatedOrder.canViewActaPdf)
        assertTrue(generatedActa.canViewPdf)

        assertFalse(failedGenerationOrder.canViewActaPdf)
        assertFalse(failedActa.canViewPdf)
        assertEquals(EstadoGeneracionActa.ERROR_GENERACION, failedActa.estadoGeneracion)
    }

    private fun sampleClosedOrder(
        estado: OrdenStatus = OrdenStatus.RECOLECCION_REALIZADA,
        tipoDevolucion: String = "POS-Posconsumo",
        numeroActaGenerada: String? = "POS-0148",
        aplicaProvenientes: Boolean = false,
        provenientes: List<TerceroProveniente> = emptyList(),
        correoPrincipal: String = "ambiental@eldorado.com.co",
        correoAlterno: String? = "gerencia@eldorado.com.co",
        estadoGeneracion: EstadoGeneracionActa? = EstadoGeneracionActa.GENERADA,
        estadoEnvio: EstadoEnvioActa? = EstadoEnvioActa.ENVIADA,
        detalleError: String? = null,
        articulos: List<ArticuloBupas> = listOf(
            ArticuloBupas(
                codigoArticulo = "BUP-AUTO-42",
                descripcion = "Batería Usada Auto 42R",
                cantidadInicial = 15,
                cantidadActa = 12,
                pesoUnitarioKg = 14.5,
                valorCompraUnitario = 55000.0
            )
        )
    ): OrdenRecoleccion = OrdenRecoleccion(
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
        tipoDevolucion = tipoDevolucion,
        observacionesIniciales = "Recoger en bodega posterior.",
        estado = estado,
        articulos = articulos,
        aplicaProvenientes = aplicaProvenientes,
        provenientes = provenientes,
        numeroActaGenerada = numeroActaGenerada,
        observacionesCampo = "Entrega verificada en bodega.",
        nombreResponsableEntrega = "Andrés Felipe Mora",
        identificacionResponsableEntrega = "1113645890",
        fechaHoraInicioAtencion = "2026-11-15 09:00:00",
        fechaHoraFinalizacion = "2026-11-15 09:40:00",
        firmaDigitalBase64 = "U0lHTkFUVVJFX0JBU0U2NA==",
        telefonoCliente = "3157401122",
        correoPrincipalCliente = correoPrincipal,
        correoAlternoCliente = correoAlterno,
        conductorNombre = "Juan Carlos Ramírez",
        conductorCedula = "1144089231",
        placaVehiculo = "WXY-482",
        estadoGeneracionActa = estadoGeneracion,
        estadoEnvioActa = estadoEnvio,
        detalleErrorActa = detalleError
    )
}
