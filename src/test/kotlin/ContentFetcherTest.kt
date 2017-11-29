import codecs.ImageCodec
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.WebClient
import messages.ImageMessage
import org.testng.Assert.assertEquals
import org.testng.annotations.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ContentFetcherTest {

    @Test
    fun testFetchHTML() {
        val contentFetcher = ContentFetcher()

        val latch = CountDownLatch(1)
        val vertx = Vertx.vertx()
        val holder = mutableListOf<Buffer>()
        val client = WebClient.create(vertx)

        contentFetcher.fetchImage(client, "http://vertx.io/assets/logo-sm.png")
                .setHandler { result ->
                    if (result.succeeded()) {
                        holder += result.result()
                    }
                    latch.countDown()
                }

        latch.await(10, TimeUnit.SECONDS)
        assertEquals(holder.size, 1)
        assertEquals(holder[0].length(), 8832)
    }

    @Test
    fun testEventBus() {
        val vertx = Vertx.vertx()

        val resultHolder = mutableListOf<ImageMessage>()

        var latch = CountDownLatch(1)

        // Deploy verticle and wait for success
        vertx.deployVerticle(ContentFetcher(), DeploymentOptions().setWorker(true), {
            latch.countDown()
        })

        latch.await()
        latch = CountDownLatch(1)

        // Listen to events verticle produces
        vertx.eventBus().consumer<ImageMessage>(ContentFetcher.PRODUCES, { result ->
            resultHolder += result.body()
            latch.countDown()
        })

        vertx.eventBus().registerCodec(ImageCodec())
        // Send new event
        vertx.eventBus().send(ContentFetcher.CONSUMES, "http://vertx.io/assets/logo-sm.png")

        latch.await(10, TimeUnit.SECONDS)

        assertEquals(1, resultHolder.size)
        assertEquals(resultHolder[0].name, "logo-sm.png")
        assertEquals(resultHolder[0].imageBuffer.length(), 8832)
    }
}
