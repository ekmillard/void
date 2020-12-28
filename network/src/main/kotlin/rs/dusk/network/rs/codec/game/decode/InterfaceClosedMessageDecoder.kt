package rs.dusk.network.rs.codec.game.decode

import rs.dusk.core.network.codec.message.MessageDecoder
import rs.dusk.core.network.codec.packet.access.PacketReader
import rs.dusk.network.rs.codec.game.decode.message.InterfaceClosedMessage

class InterfaceClosedMessageDecoder : MessageDecoder<InterfaceClosedMessage>(0) {

    override fun decode(packet: PacketReader) = InterfaceClosedMessage

}