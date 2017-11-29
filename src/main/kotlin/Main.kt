import codecs.DocumentCodec
import codecs.ImageCodec
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx

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

        // One Piece manga is notoriously long
        repeat(886, { i ->
            vertx.eventBus().send(HTMLFetcher.CONSUMES,
                    "http://manganeli.com/chapter/read_one_piece_manga_online_free4/chapter_${i + 1}")
        })
    })
}