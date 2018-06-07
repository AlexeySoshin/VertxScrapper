import codecs.ImageCodec
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.ext.web.client.WebClient
import messages.ImageMessage
import java.util.concurrent.TimeoutException
import kotlin.system.measureTimeMillis

class ContentFetcher : AbstractVerticle() {

    companion object {
        const val CONSUMES = AttributeFinder.PRODUCES
        const val PRODUCES = "content.write"
    }

    override fun start(future: Future<Void?>) {

        val time = measureTimeMillis {
            val client = WebClient.create(vertx)
            val deliveryOptions = DeliveryOptions().setCodecName(ImageCodec().name())

            this.vertx.eventBus().consumer<String>(CONSUMES, { event ->
                val url = event.body()
                val start = System.currentTimeMillis()
                fetchImage(client, url).setHandler { res ->
                    val imageName = url.split("/").last()
                    if (res.succeeded()) {
                        println("Took ${System.currentTimeMillis() - start}ms to fetch image $imageName")
                        vertx.eventBus().send(PRODUCES,
                                ImageMessage(imageName, res.result()), deliveryOptions)
                    }
                    // Ignore failures
                }
            })
        }

        println("Took ${time}ms until ContentFetcher initialized")

        future.complete()
    }

    private val defaultTimeout: Long = 5000

    /**
     * This method is almost the same as HTMLFetcher.fetchHTML
     * Not DRYied for clarity of presentation
     */
    fun fetchImage(client: WebClient, url: String): Future<Buffer> {

        val result = Future.future<Buffer>()

        client.getAbs(url).timeout(defaultTimeout).send { response ->
            if (response.succeeded()) {
                result.complete(response.result().bodyAsBuffer())
            } else {
                if (response.cause() is TimeoutException) {
                    println("Retrying $url")
                    vertx.eventBus().send(CONSUMES, url)
                }
                result.fail(response.cause())
            }
        }

        return result
    }

    override fun stop() {
        this.vertx.eventBus().consumer<String>(CONSUMES).unregister()
    }
}