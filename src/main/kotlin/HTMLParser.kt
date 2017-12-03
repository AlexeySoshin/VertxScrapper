import codecs.DocumentCodec
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.DeliveryOptions
import org.jsoup.Jsoup

class HTMLParser : AbstractVerticle() {
    companion object {
        // Note that this is a coupling between verticles, although not very tight one
        // It helps avoiding naming mistakes though
        val CONSUMES = HTMLParser.PRODUCES
        val PRODUCES = "findAttribute"
    }

    /**
     * To send custom objects over EventBus, you need both to register codecs and to specify it during send
     */
    override fun start() {
        val deliveryOptions = DeliveryOptions().setCodecName(DocumentCodec().name())
        this.vertx.eventBus().consumer<String>(CONSUMES, { event ->
            event.body().let { body ->
                if (body == null) {
                    return@let
                }

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
}

