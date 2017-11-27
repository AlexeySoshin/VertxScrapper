import codecs.DocumentCodec
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.DeliveryOptions
import org.jsoup.Jsoup

class HTMLParser : AbstractVerticle() {
    companion object {
        val CONSUMES = HTMLFetcher.PRODUCES
        val PRODUCES = "findAttribute"
    }

    /**
     * To send custom objects over EventBus, you need both to register codecs and to specify it during send
     */
    override fun start() {
        val deliveryOptions = DeliveryOptions().setCodecName(DocumentCodec().name())
        registerCodec()
        this.vertx.eventBus().consumer<String>(CONSUMES, { event ->
            event.body().let { body ->
                val start = System.currentTimeMillis()
                val document = Jsoup.parse(body)
                println("Took ${System.currentTimeMillis() - start}ms to parse the document")

                vertx.eventBus().send(PRODUCES, document, deliveryOptions)
            }
        })
    }

    override fun stop() {
        this.vertx.eventBus().consumer<String>(CONSUMES).unregister()
    }

    private fun registerCodec() {
        try {
            this.vertx.eventBus().registerCodec(DocumentCodec())
        } catch (e: IllegalStateException) {
            println("WARN: $e")
        }
    }
}

