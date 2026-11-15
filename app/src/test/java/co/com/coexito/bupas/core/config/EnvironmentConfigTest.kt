package co.com.coexito.bupas.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentConfigTest {

    @Test
    fun `ambientes dev y test exponen etiqueta sutil para UI`() {
        assertEquals("DEV", AppEnvironment.fromRaw("DEV").badgeLabel)
        assertEquals("TEST", AppEnvironment.fromRaw("TEST").badgeLabel)
        assertFalse(AppEnvironment.DEV.isProduction)
        assertFalse(AppEnvironment.TEST.isProduction)
    }

    @Test
    fun `ambiente productivo no expone etiqueta para mantener UI limpia`() {
        val prod = AppEnvironment.fromRaw("PROD")
        assertTrue(prod.isProduction)
        assertNull(prod.badgeLabel)
    }
}
