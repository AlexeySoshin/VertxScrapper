import codecs.ImageCodec
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.DeliveryOptions
import messages.ImageMessage
import org.testng.Assert.assertTrue
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ContentWriterTest {

    @Test
    fun testEventBus() {
        val vertx = Vertx.vertx()

        val latch = CountDownLatch(1)

        vertx.deployVerticle(ContentWriter("./"),
                DeploymentOptions().setWorker(true))

        vertx.eventBus().consumer<Boolean>(ContentWriter.PRODUCES, { result ->
            latch.countDown()
        })

        val deliveryOptions = DeliveryOptions().setCodecName(ImageCodec().name())

        val image = Buffer.buffer(getImage("logo-sm.png"))

        vertx.eventBus().registerCodec(ImageCodec())
        vertx.eventBus().send(ContentWriter.CONSUMES, ImageMessage("test.png", image), deliveryOptions)

        latch.await(5, TimeUnit.SECONDS)

        assertTrue(File("./test.png").exists())
    }

    @AfterMethod
    fun removeImage() {
        File("./test.png").delete()
    }
}


fun getImage(imageResource: String): ByteArray {
    return ContentWriterTest::javaClass.javaClass.getResource(imageResource).readBytes()
}