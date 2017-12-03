import codecs.DocumentCodec
import codecs.ImageCodec
import io.vertx.core.DeploymentOptions
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

fun main(args: Array<String>) {

    val vertx = Vertx.vertx()

    // Register codecs to be able to send custom objects over EventBus
    vertx.eventBus().registerCodec(DocumentCodec()).registerCodec(ImageCodec())

    deployVerticles(vertx)

    serveStats(vertx)
}

private fun serveStats(vertx: Vertx) {
    // Gather some statistics about the progress
    val writtenBytes = AtomicLong(0)
    val writtenFiles = AtomicInteger(0)

    // Each time you get new stats, publish them to the client
    vertx.eventBus().consumer<Int>(ContentWriter.PRODUCES, { r ->
        writtenBytes.addAndGet(r.body().toLong())
        writtenFiles.incrementAndGet()

        vertx.eventBus().publish("stats", statsAsJson(writtenBytes, writtenFiles))
    })

    val router = Router.router(vertx)

    // Also provide statistics on http://localhost:8080/stats as JSON
    router.get("/stats").handler { req ->
        req.response().end(statsAsJson(writtenBytes, writtenFiles))
    }
    router.route("/eventbus/*").handler(sockJsHandler(vertx))
    router.route("/*").handler(StaticHandler.create("web"))

    vertx.createHttpServer().requestHandler(router::accept).listen(8080)
}

private fun deployVerticles(vertx: Vertx) {

    val deploymentOptions = DeploymentOptions().setWorker(true)

    vertx.deployVerticle(ContentWriter("../manga/"),
            deploymentOptions)
    vertx.deployVerticle("ContentFetcher",
            deploymentOptions.setInstances(2))
    vertx.deployVerticle(AttributeFinder("#vungdoc img", "src"),
            deploymentOptions.setInstances(1))
    vertx.deployVerticle("HTMLParser",
            deploymentOptions.setInstances(2))

    // Wait until first verticle in the pipeline is deployed to start sending the data
    vertx.deployVerticle("HTMLFetcher",
            deploymentOptions.setInstances(4), { _ ->

        println("Open UI by using this link: http://localhost:8080/")
        // One Piece manga is notoriously long, 886 chapters
        repeat(886, { i ->
            vertx.eventBus().send(HTMLFetcher.CONSUMES,
                    "http://manganeli.com/chapter/read_one_piece_manga_online_free4/chapter_${i + 1}")
        })
    })
}

fun statsAsJson(writtenBytes: AtomicLong, writtenFiles: AtomicInteger): String {
    return JsonObject(Stats(writtenBytes.toLong(), writtenFiles.toInt()).toMap()).toString()
}

/**
 * Create SockJS handler to be able to send data to clients over WebSockets
 */
fun sockJsHandler(vertx: Vertx): Handler<RoutingContext>? {
    return SockJSHandler.create(vertx).bridge(BridgeOptions().addOutboundPermitted(PermittedOptions().setAddressRegex("stats")))
}

data class Stats(private val bytes: Long, private val images: Int) {
    fun toMap(): Map<String, Any> {
        return mapOf("bytes" to bytes, "images" to images)
    }
}