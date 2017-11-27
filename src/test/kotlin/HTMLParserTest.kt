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

        val latch = CountDownLatch(1)

        vertx.deployVerticle(HTMLParser(), DeploymentOptions().setWorker(true))

        vertx.eventBus().consumer<Document>(HTMLParser.PRODUCES, { result ->
            resultHolder += result.body()
            latch.countDown()
        })

        vertx.eventBus().send(HTMLParser.CONSUMES, getHTML("1.html"))

        latch.await(10, TimeUnit.SECONDS)

        assertEquals(1, resultHolder.size)
        assertEquals(resultHolder[0].title(), "Vert.x Web Client - Vert.x")
    }
}


fun getHTML(textResource: String): String {
    return HTMLParserTest::javaClass.javaClass.getResource(textResource).readText()
}