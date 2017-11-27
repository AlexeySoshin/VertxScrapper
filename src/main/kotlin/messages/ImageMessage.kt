package messages

import io.vertx.core.buffer.Buffer

data class ImageMessage(val name: String, val imageBuffer: Buffer)