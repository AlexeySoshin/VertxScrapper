package codecs

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import messages.ImageMessage

class ImageCodec : MessageCodec<ImageMessage, ImageMessage> {
    override fun decodeFromWire(pos: Int, buffer: Buffer?): ImageMessage {
        throw RuntimeException()
    }

    override fun encodeToWire(buffer: Buffer?, s: ImageMessage) {
    }

    override fun name(): String {
        return this.javaClass.simpleName
    }

    override fun systemCodecID(): Byte {
        return -1
    }

    // Local transformation
    override fun transform(d: ImageMessage): ImageMessage {
        return d
    }
}