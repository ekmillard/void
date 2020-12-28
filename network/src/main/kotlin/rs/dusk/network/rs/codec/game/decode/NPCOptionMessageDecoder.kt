package rs.dusk.network.rs.codec.game.decode

import rs.dusk.buffer.Modifier
import rs.dusk.core.network.codec.message.MessageDecoder
import rs.dusk.core.network.codec.packet.access.PacketReader
import rs.dusk.network.rs.codec.game.decode.message.NPCOptionMessage

class NPCOptionMessageDecoder(private val index: Int) : MessageDecoder<NPCOptionMessage>(3) {

    override fun decode(packet: PacketReader) = NPCOptionMessage(
        packet.readBoolean(Modifier.ADD),
        packet.readShort(Modifier.ADD),
        index + 1
    )

}