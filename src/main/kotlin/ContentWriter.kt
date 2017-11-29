import io.vertx.core.AbstractVerticle
import messages.ImageMessage
import java.io.File

class ContentWriter(private val targetDirectory: String) : AbstractVerticle() {

    companion object {
        val CONSUMES = ContentFetcher.PRODUCES
        val PRODUCES = "done"
    }

    override fun start() {

        createDirectory(targetDirectory)

        this.vertx.eventBus().consumer<ImageMessage>(CONSUMES, { event ->
            event.body().let { imageMessage ->

                if (imageMessage.name.isEmpty()) {
                    return@let
                }

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