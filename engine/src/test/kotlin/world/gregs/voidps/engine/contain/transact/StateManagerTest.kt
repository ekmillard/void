package world.gregs.voidps.engine.contain.transact

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import world.gregs.voidps.cache.definition.data.ItemDefinition
import world.gregs.voidps.engine.contain.Container
import world.gregs.voidps.engine.entity.item.Item

internal class StateManagerTest {

    private lateinit var state: StateManager
    private lateinit var container: Container

    @BeforeEach
    fun setup() {
        container = Container(arrayOf())
        state = StateManager(container)
    }

    @Test
    fun `Revert saved state`() {
        container.data = arrayOf(Item("item", 1, def = ItemDefinition.EMPTY))
        assertFalse(state.hasSaved())
        state.save()
        assertTrue(state.hasSaved())
        container.data = arrayOf(Item("item2", 4, def = ItemDefinition.EMPTY))
        assertTrue(state.revert())
        assertFalse(state.hasSaved())

        assertEquals("item", container.items[0].id)
        assertEquals(1, container.items[0].amount)
    }

    @Test
    fun `Revert with no saved state`() {
        container.data = arrayOf(Item("item", 1, def = ItemDefinition.EMPTY))
        assertFalse(state.hasSaved())
        assertFalse(state.revert())

        assertEquals("item", container.items[0].id)
        assertEquals(1, container.items[0].amount)
    }
}