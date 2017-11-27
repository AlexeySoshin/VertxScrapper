import codecs.DocumentCodec
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import org.jsoup.Jsoup
import org.testng.Assert.assertEquals
import org.testng.annotations.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AttributeFinderTest {

    @Test
    fun testEventBus() {
        val vertx = Vertx.vertx()
        val deliveryOptions = DeliveryOptions().setCodecName(DocumentCodec().name())
        val resultHolder = mutableListOf<String>()

        val latch = CountDownLatch(1)

        vertx.deployVerticle(AttributeFinder("a.navbar-brand img", "src"),
                DeploymentOptions().setWorker(true))

        vertx.eventBus().consumer<String>(AttributeFinder.PRODUCES, { result ->
            resultHolder += result.body()
            latch.countDown()
        })

        vertx.eventBus().send(AttributeFinder.CONSUMES, Jsoup.parse(getHTML("1.html")), deliveryOptions)

        latch.await(1, TimeUnit.SECONDS)

        assertEquals(resultHolder.size, 1)
        assertEquals(resultHolder[0], "http://vertx.io/assets/logo-sm.png")
    }

    @Test
    fun testMultipleResults() {
        val vertx = Vertx.vertx()
        val deliveryOptions = DeliveryOptions().setCodecName(DocumentCodec().name())
        val resultHolder = mutableListOf<String>()

        val latch = CountDownLatch(17)

        vertx.deployVerticle(AttributeFinder("img.lazyload", "data-src"),
                DeploymentOptions().setWorker(true))

        vertx.eventBus().consumer<String>(AttributeFinder.PRODUCES, { result ->
            resultHolder += result.body()
            latch.countDown()
        })

        vertx.eventBus().send(AttributeFinder.CONSUMES, Jsoup.parse(getHTML("2.html")), deliveryOptions)

        latch.await(2, TimeUnit.SECONDS)

        assertEquals(resultHolder.size, 17)
        assertEquals(resultHolder[2], "https://i.chzbgr.com/full/9098449920/h5B138E26/")
    }
}