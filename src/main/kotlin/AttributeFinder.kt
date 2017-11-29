import io.vertx.core.AbstractVerticle
import org.jsoup.nodes.Document

class AttributeFinder(private val selector: String,
                      private val attribute: String) : AbstractVerticle() {

    companion object {
        val CONSUMES = HTMLParser.PRODUCES
        val PRODUCES = "content.fetch"
    }

    override fun start() {
        this.vertx.eventBus().consumer<Document>(CONSUMES, { event ->
            event.body().let { body ->
                val start = System.currentTimeMillis()

                val elements = body.body().select(selector)
                val attributes = elements.eachAttr(attribute)
                println("Took ${System.currentTimeMillis() - start}ms to find ${attributes.size} attributes")

                attributes.forEach { a ->
                    vertx.eventBus().send(PRODUCES, a)
                }
            }
        })
    }

    override fun stop() {
        this.vertx.eventBus().consumer<String>(CONSUMES).unregister()
    }
}