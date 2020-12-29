package rs.dusk.core.network.codec

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import rs.dusk.core.io.crypto.IsaacCipher
import rs.dusk.core.network.codec.Codec.Companion.CODEC_KEY
import rs.dusk.core.network.codec.Codec.Companion.IN_CIPHER_KEY
import rs.dusk.core.network.codec.Codec.Companion.OUT_CIPHER_KEY
import rs.dusk.core.network.codec.Codec.Companion.SIZED_KEY
import rs.dusk.core.network.codec.message.MessageDecoder
import rs.dusk.core.network.codec.message.MessageEncoder
import rs.dusk.core.network.codec.message.MessageHandler
import rs.dusk.core.network.model.message.Message
import kotlin.reflect.KClass

/**
 * @author Tyluur <contact@kiaira.tech>
 * @since February 18, 2020
 */
abstract class Codec {

    val decoders = HashMap<Int, MessageDecoder>()
    val encoders = HashMap<KClass<*>, MessageEncoder<*>>()

    abstract fun register()

    fun registerDecoder(opcode: Int, decoder: MessageDecoder) {
        if (decoders[opcode] != null) {
            throw IllegalArgumentException("Cannot have duplicate decoders $decoder $opcode")
        }
        decoders[opcode] = decoder
    }

    inline fun <reified T : Message> registerEncoder(encoder: MessageEncoder<T>) {
        if (encoders.contains(T::class)) {
            throw IllegalArgumentException("Cannot have duplicate encoders ${T::class} $encoder")
        }
        encoders[T::class] = encoder
    }

    fun registerHandler(opcode: Int, handler: MessageHandler) {
        val decoder = getDecoder(opcode) ?: throw IllegalArgumentException("Missing decoder $opcode $handler")
        if (decoder.handler != null) {
            throw IllegalArgumentException("Cannot have duplicate handlers $opcode $handler")
        }
        decoder.handler = handler
    }

    fun getDecoder(opcode: Int): MessageDecoder? {
        return decoders[opcode]
    }

    /**
     * Finds an [encoder][MessageEncoder] by class
     * @return MessageEncoder<M>?
     */
    @Suppress("UNCHECKED_CAST")
    fun <M : Message> getEncoder(clazz: KClass<M>): MessageEncoder<M>? {
        return encoders[clazz] as? MessageEncoder<M>
    }

    companion object {
        /**
         * The attribute in the [channel][Channel] that identifies the [codec][Codec]
         */
        val CODEC_KEY: AttributeKey<Codec> = AttributeKey.valueOf("codec.key")
        val IN_CIPHER_KEY: AttributeKey<IsaacCipher> = AttributeKey.valueOf("cipher.in.key")
        val OUT_CIPHER_KEY: AttributeKey<IsaacCipher> = AttributeKey.valueOf("cipher.out.key")
        val SIZED_KEY: AttributeKey<Boolean> = AttributeKey.valueOf("sized.key")
    }
}


fun Channel.getCipherOut(): IsaacCipher? {
    return attr(OUT_CIPHER_KEY).get()
}

fun Channel.setCipherOut(cipher: IsaacCipher?) {
    attr(OUT_CIPHER_KEY).set(cipher)
}

fun Channel.getCipherIn(): IsaacCipher? {
    return attr(IN_CIPHER_KEY).get()
}

fun Channel.setCipherIn(cipher: IsaacCipher?) {
    attr(IN_CIPHER_KEY).set(cipher)
}

fun Channel.getSized(): Boolean {
    return attr(SIZED_KEY).get() ?: false
}

fun Channel.setSized(sized: Boolean) {
    attr(SIZED_KEY).set(sized)
}
/**
 * Getting the codec of the channel
 * @receiver Channel
 */
fun Channel.getCodec(): Codec? {
    return attr(CODEC_KEY).get()
}

/**
 * Setting the codec of the channel
 * @receiver Channel
 */
fun Channel.setCodec(codec: Codec) {
    attr(CODEC_KEY).set(codec)
    setCipherIn(null)
    setCipherOut(null)
}