import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HTMLFetcherTest {

    @Test
    fun testFetchHTML() {
        val htmlFetcher = HTMLFetcher()

        val latch = CountDownLatch(1)
        val vertx = Vertx.vertx()
        val holder = mutableListOf<String>()
        val client = WebClient.create(vertx)

        htmlFetcher.fetchHTML(client, "http://vertx.io/docs/vertx-web-client/java/")
                .setHandler { result ->
                    if (result.succeeded()) {
                        holder += result.result()
                    }
                    latch.countDown()
                }

        latch.await(20, TimeUnit.SECONDS)
        assertEquals(1, holder.size)
        assertTrue(holder[0].contains("Vert.x Web Client is an asynchronous HTTP and HTTP/2 client"))
    }

    @Test
    fun testEventBus() {
        val vertx = Vertx.vertx()

        val resultHolder = mutableListOf<String>()

        var latch = CountDownLatch(1)

        // Deploy verticle and wait for success
        vertx.deployVerticle(HTMLFetcher(), DeploymentOptions().setWorker(true), {
            latch.countDown()
        })

        latch.await()
        latch = CountDownLatch(1)

        // Listen to events verticle produces
        vertx.eventBus().consumer<String>(HTMLFetcher.PRODUCES, { result ->
            resultHolder.add(result.body())
            latch.countDown()
        })

        // Send new event
        vertx.eventBus().send(HTMLFetcher.CONSUMES,
                "http://vertx.io/docs/vertx-web-client/java/")

        latch.await(10, TimeUnit.SECONDS)

        assertEquals(1, resultHolder.size)
        assertTrue(resultHolder[0].contains("Vert.x Web Client is an asynchronous HTTP and HTTP/2 client"))
    }
}
