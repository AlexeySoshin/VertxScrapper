import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.web.client.WebClient
import kotlin.system.measureTimeMillis

class HTMLFetcher : AbstractVerticle() {

    companion object {
        val CONSUMES = "html.fetch"
        val PRODUCES = "html.parse"
    }

    override fun start(future: Future<Void?>) {
        val time = measureTimeMillis {
            val client = WebClient.create(vertx)

            this.vertx.eventBus().consumer<String>(CONSUMES, { event ->
                // If null string was sent over, ignore it
                event.body().let { url ->
                    val start = System.currentTimeMillis()
                    fetchHTML(client, url).setHandler { res ->

                        println("Took ${System.currentTimeMillis() - start}ms to fetch document")
                        vertx.eventBus().send(PRODUCES, res.result())
                    }
                }
            })
        }

        println("Took ${time}ms until HTMLFetcher initialized")

        future.complete()
    }

    fun fetchHTML(client: WebClient, url: String): Future<String> {

        val result = Future.future<String>()

        client.getAbs(url).timeout(10000).send { response ->
            if (response.succeeded()) {
                result.complete(response.result().bodyAsString())
            } else {
                result.fail(response.cause())
            }
        }

        return result
    }
}