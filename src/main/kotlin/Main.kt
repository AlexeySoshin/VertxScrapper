import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx

fun main(args: Array<String>) {

    val vertx = Vertx.vertx()

    val deploymentOptions = DeploymentOptions().setWorker(true)

    vertx.deployVerticle(ContentWriter("./cats/"), deploymentOptions)
    vertx.deployVerticle("ContentFetcher", deploymentOptions.setInstances(2))
    vertx.deployVerticle(AttributeFinder("img", "src"), deploymentOptions.setInstances(1))
    vertx.deployVerticle("HTMLParser", deploymentOptions.setInstances(2))
    vertx.deployVerticle("HTMLFetcher", deploymentOptions.setInstances(4), { _ ->
        vertx.eventBus().send(HTMLFetcher.CONSUMES, "http://thumbpress.com/lol-cats-50-awesomely-funny-cat-photos-to-crack-you-up")
    })


}