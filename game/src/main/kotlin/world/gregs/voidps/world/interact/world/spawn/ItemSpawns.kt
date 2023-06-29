package world.gregs.voidps.world.interact.world.spawn

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import world.gregs.voidps.engine.entity.World
import world.gregs.voidps.engine.entity.item.floor.FloorItems
import world.gregs.voidps.engine.get
import world.gregs.voidps.engine.getProperty
import world.gregs.voidps.engine.map.Tile
import world.gregs.voidps.engine.timedLoad
import world.gregs.yaml.Yaml
import world.gregs.yaml.read.YamlReaderConfiguration

class ItemSpawns(
    private val chunks: MutableMap<Int, ItemSpawn> = Int2ObjectOpenHashMap()
) {
    val size: Int
        get() = chunks.size

    fun set(tile: Tile, spawn: ItemSpawn) {
        chunks[tile.id] = spawn
    }

    fun get(tile: Tile): ItemSpawn? = chunks[tile.id]

    fun clear() {
        chunks.clear()
    }
}

@Suppress("UNCHECKED_CAST")
fun loadItemSpawns(
    items: FloorItems,
    spawns: ItemSpawns,
    yaml: Yaml = get(),
    path: String = getProperty("itemSpawnsPath")
) {
    timedLoad("item spawn") {
        spawns.clear()
        val membersWorld = World.members
        val config = object : YamlReaderConfiguration() {
            override fun add(list: MutableList<Any>, value: Any, parentMap: String?) {
                value as Map<String, Any>
                val members = value["members"] as? Boolean ?: false
                if (!membersWorld && members) {
                    return
                }
                val id = value["id"] as String
                val tile = Tile.fromMap(value)
                val amount = value["amount"] as? Int ?: 1
                val delay = value["delay"] as? Int ?: 60
                spawns.set(tile, ItemSpawn(id, amount, delay))
                items.add(tile, id, amount)
            }
        }
        yaml.load<Any>(path, config)
        spawns.size
    }
}