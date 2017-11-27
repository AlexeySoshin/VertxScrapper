import codecs.ImageCodec
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.ext.web.client.WebClient
import messages.ImageMessage
import kotlin.system.measureTimeMillis

class ContentFetcher : AbstractVerticle() {

    companion object {
        val CONSUMES = AttributeFinder.PRODUCES
        val PRODUCES = "content.write"
    }

    override fun start(future: Future<Void?>) {

        val time = measureTimeMillis {
            val client = WebClient.create(vertx)
            val deliveryOptions = DeliveryOptions().setCodecName(ImageCodec().name())

            // You could chain those if you want to
            registerCodec()
            this.vertx.eventBus().consumer<String>(CONSUMES, { event ->
                // If null string was sent over, ignore it
                event.body().let { url ->
                    val start = System.currentTimeMillis()
                    fetchImage(client, url).setHandler { res ->

                        val imageName = url.split("/").last()

                        println("Took ${System.currentTimeMillis() - start}ms to fetch image $imageName")
                        vertx.eventBus().send(PRODUCES, ImageMessage(imageName, res.result()), deliveryOptions)
                    }
                }
            })
        }

        println("Took ${time}ms until ContentFetcher initialized")

        future.complete()
    }

    private fun registerCodec() {
        try {
            this.vertx.eventBus().registerCodec(ImageCodec())
        } catch (e: IllegalStateException) {
            println("WARN: $e")
        }
    }

    fun fetchImage(client: WebClient, url: String): Future<Buffer> {

        val result = Future.future<Buffer>()

        client.getAbs(url).timeout(10000).send { response ->
            if (response.succeeded()) {
                result.complete(response.result().bodyAsBuffer())
            } else {
                result.fail(response.cause())
            }
        }

        return result
    }
}