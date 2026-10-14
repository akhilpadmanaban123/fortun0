package app.paisapilot.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryInfoTest {

    @Test
    fun `built-in categories keep their enum name`() {
        val info = CategoryInfo.of("FUEL")
        assertEquals(TxnCategory.FUEL, info.builtIn)
        assertEquals("Fuel", info.label)
        assertFalse(info.isCustom)
    }

    @Test
    fun `a custom category survives being stored and read back`() {
        val created = CategoryInfo.custom("  Tea: evening  ", "coffee")
        val read = CategoryInfo.of(created.stored)
        assertTrue(read.isCustom)
        assertEquals("Tea: evening", read.label)
        assertEquals("coffee", read.iconKey)
        assertEquals(created, read)
    }

    @Test
    fun `anything unreadable falls back to Other`() {
        assertEquals(TxnCategory.OTHER_EXPENSE, CategoryInfo.of("custom:coffee:").builtIn)
        assertEquals(TxnCategory.OTHER_EXPENSE, CategoryInfo.of("NOT_A_CATEGORY").builtIn)
        // An older app version reads a custom category the same way.
        assertEquals(TxnCategory.OTHER_EXPENSE, TxnCategory.fromName("custom:coffee:Tea"))
    }
}
