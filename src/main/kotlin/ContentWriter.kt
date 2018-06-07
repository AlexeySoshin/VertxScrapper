import io.vertx.core.AbstractVerticle
import messages.ImageMessage
import java.io.File

class ContentWriter(private val targetDirectory: String) : AbstractVerticle() {

    companion object {
        const val CONSUMES = ContentFetcher.PRODUCES
        const val PRODUCES = "done"
    }

    override fun start() {

        createDirectory(targetDirectory)

        this.vertx.eventBus().consumer<ImageMessage>(CONSUMES, { event ->
            val imageMessage = event.body()

            if (!imageMessage.name.isEmpty()) {
                val start = System.currentTimeMillis()

                vertx.fileSystem().writeFile("$targetDirectory/${imageMessage.name}",
                        imageMessage.buffer, { res ->
                    if (res.succeeded()) {
                        println("Took ${System.currentTimeMillis() - start}ms to write image ${imageMessage.name}")
                        vertx.eventBus().publish(PRODUCES, imageMessage.buffer.length())
                    } else {
                        println(res.cause())
                    }
                })
            }
        })
    }

    private fun createDirectory(targetDirectory: String) {
        File(targetDirectory).mkdirs()
    }

    override fun stop() {
        this.vertx.eventBus().consumer<String>(CONSUMES).unregister()
    }
}