import codecs.DocumentCodec
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import org.jsoup.nodes.Document
import org.testng.Assert.assertEquals
import org.testng.annotations.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HTMLParserTest {

    @Test
    fun testEventBus() {
        val vertx = Vertx.vertx()

        val resultHolder = mutableListOf<Document>()

        var latch = CountDownLatch(1)

        vertx.eventBus().registerCodec(DocumentCodec())
        vertx.deployVerticle(HTMLParser(), DeploymentOptions().setWorker(true), { _ ->
            latch.countDown()
        })

        latch.await()
        latch = CountDownLatch(1)

        vertx.eventBus().consumer<Document>(HTMLParser.PRODUCES, { result ->
            resultHolder += result.body()
            latch.countDown()
        })

        vertx.eventBus().send(HTMLParser.CONSUMES, getHTML("1.html"))

        latch.await(20, TimeUnit.SECONDS)

        assertEquals(resultHolder.size, 1)
        assertEquals(resultHolder[0].title(), "Vert.x Web Client - Vert.x")
    }
}


fun getHTML(textResource: String): String {
    return HTMLParserTest::javaClass.javaClass.getResource(textResource).readText()
}