import io.vertx.core.AbstractVerticle
import org.jsoup.nodes.Document

/**
 * This class looks for attribute values
 */
class AttributeFinder(private val selector: String,
                      private val attribute: String) : AbstractVerticle() {

    companion object {
        const val CONSUMES = HTMLParser.PRODUCES
        const val PRODUCES = "content.fetch"
    }

    override fun start() {
        this.vertx.eventBus().consumer<Document>(CONSUMES, { event ->
            val start = System.currentTimeMillis()

            val elements = event.body().body().select(selector)
            val attributes = elements.eachAttr(attribute)
            println("Took ${System.currentTimeMillis() - start}ms to find ${attributes.size} attributes")

            attributes.forEach { a ->
                vertx.eventBus().send(PRODUCES, a)
            }
        })
    }

    override fun stop() {
        this.vertx.eventBus().consumer<String>(CONSUMES).unregister()
    }
}