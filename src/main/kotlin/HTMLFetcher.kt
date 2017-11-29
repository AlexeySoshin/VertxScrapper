import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.web.client.WebClient
import java.util.concurrent.TimeoutException
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
                        if (res.succeeded()) {
                            println("Took ${System.currentTimeMillis() - start}ms to fetch document $url")
                            vertx.eventBus().send(PRODUCES, res.result())
                        } else {
                            println("Couldn't fetch document $url")
                        }
                    }
                }
            })
        }

        println("Took ${time}ms until HTMLFetcher initialized")

        future.complete()
    }

    fun fetchHTML(client: WebClient, url: String, retries: Int = 3): Future<String> {

        val result = Future.future<String>()

        client.getAbs(url).timeout(10000).send { response ->
            if (response.succeeded()) {
                result.complete(response.result().bodyAsString())
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