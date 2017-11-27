package codecs

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import org.jsoup.nodes.Document

class DocumentCodec : MessageCodec<Document, Document> {
    override fun decodeFromWire(pos: Int, buffer: Buffer?): Document {
        throw RuntimeException()
    }

    override fun encodeToWire(buffer: Buffer?, s: Document?) {
    }

    override fun name(): String {
        return this.javaClass.simpleName
    }

    override fun systemCodecID(): Byte {
        return -1
    }

    // Local transformation
    override fun transform(d: Document): Document {
        return d
    }
}