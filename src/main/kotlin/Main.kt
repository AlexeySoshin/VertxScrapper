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

    val deploymentOptions = DeploymentOptions().setWorker(true)

    vertx.eventBus().registerCodec(DocumentCodec()).registerCodec(ImageCodec())

    vertx.deployVerticle(ContentWriter("../manga/"),
            deploymentOptions)
    vertx.deployVerticle("ContentFetcher",
            deploymentOptions.setInstances(2))
    vertx.deployVerticle(AttributeFinder("#vungdoc img", "src"),
            deploymentOptions.setInstances(1))
    vertx.deployVerticle("HTMLParser",
            deploymentOptions.setInstances(2))
    vertx.deployVerticle("HTMLFetcher",
            deploymentOptions.setInstances(4), { _ ->

        println("Open UI by using this link: http://localhost:8080/")
        // One Piece manga is notoriously long, 886 chapters
        repeat(886, { i ->
            vertx.eventBus().send(HTMLFetcher.CONSUMES,
                    "http://manganeli.com/chapter/read_one_piece_manga_online_free4/chapter_${i + 1}")
        })
    })

    val writtenBytes = AtomicLong(0)
    val writtenFiles = AtomicInteger(0)

    // Each time you get new stats, publish them to the client
    vertx.eventBus().consumer<Int>(ContentWriter.PRODUCES, { r ->
        writtenBytes.addAndGet(r.body().toLong())
        writtenFiles.incrementAndGet()

        vertx.eventBus().publish("stats", statsAsJson(writtenBytes, writtenFiles))
    })

    val router = Router.router(vertx)

    router.get("/stats").handler { req ->
        req.response().end(statsAsJson(writtenBytes, writtenFiles))
    }
    router.route("/eventbus/*").handler(sockJsHandler(vertx))
    router.route("/*").handler(StaticHandler.create("web"))

    vertx.createHttpServer().requestHandler(router::accept).listen(8080)
}

fun statsAsJson(writtenBytes: AtomicLong, writtenFiles: AtomicInteger): String {
    return JsonObject(Stats(writtenBytes.toLong(), writtenFiles.toInt()).toMap()).toString()
}

fun sockJsHandler(vertx: Vertx): Handler<RoutingContext>? {
    return SockJSHandler.create(vertx).bridge(BridgeOptions().addOutboundPermitted(PermittedOptions().setAddressRegex("stats")))
}

data class Stats(private val bytes: Long, private val images: Int) {
    fun toMap(): Map<String, Any> {
        return mapOf("bytes" to bytes, "images" to images)
    }
}