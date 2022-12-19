package co.demo.whisky;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import io.reactiverse.junit5.web.WebClientOptionsInject;

import static io.reactiverse.junit5.web.TestRequest.testRequest;
import static io.reactiverse.junit5.web.TestRequest.*;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestVerticleRestApi {

  String deploymentId;

  @WebClientOptionsInject
  public WebClientOptions opts = new WebClientOptions().setDefaultPort(8080).setDefaultHost("localhost");

  @BeforeAll
  void setUp(Vertx vertx, VertxTestContext testContext) {
    testContext
      .assertComplete(vertx.deployVerticle(new MainVerticle()))
      .onComplete(ar -> {
        deploymentId = ar.result();
        testContext.completeNow();
      });
  }

  @AfterAll
  void tearDown(Vertx vertx, VertxTestContext testContext) {
    testContext
      .assertComplete(vertx.undeploy(deploymentId))
      .onComplete(ar -> testContext.completeNow());
  }

  @Test
  @Order(1)
  @DisplayName("Test Root")
  public void testRoot(WebClient client, VertxTestContext testContext) {
    testRequest(client.get("/"))
      .expect(
        statusCode(200),
        statusMessage("OK")
      )
      .send(testContext);
  }

  @Test
  @Order(2)
  @DisplayName("Test Get All")
  public void testAll(WebClient client, VertxTestContext testContext) {
    testRequest(client.get("/api/whiskies"))
      .expect(
        statusCode(200)
      )
      .send(testContext);
  }

  @Test
  @Order(3)
  @DisplayName("Test Get One No Found")
  public void testGetOneNotFound(WebClient client, VertxTestContext testContext) {
    testRequest(client.get("/api/whiskies/11"))
      .expect(
        statusCode(404),
        emptyResponse()
      )
      .send(testContext);
  }

  @Test
  @Order(4)
  @DisplayName("Test Get One Success")
  public void testGetOne(WebClient client, VertxTestContext testContext) {
    testRequest(client.get("/api/whiskies/1"))
      .expect(
        statusCode(200),
        jsonBodyResponse(new JsonObject().put("id", 1).put("name", "Talisker 57° North").put("origin", "Scotland, Island"))
      )
      .send(testContext);
  }

  @Test
  @Order(5)
  @DisplayName("Test Delete One")
  public void testDeleteOne(WebClient client, VertxTestContext testContext) {
    testRequest(client.delete("/api/whiskies/1"))
      .expect(
        statusCode(204),
        emptyResponse()
      )
      .send(testContext);
  }

  @Test
  @Order(6)
  @DisplayName("Test New One & Update")
  public void testAddUpdate(WebClient client, VertxTestContext testContext) {
    final JsonObject newWhisky = new JsonObject().put("id", 2).put("name", "Name").put("origin", "Origin");
    final JsonObject updateWhisky = new JsonObject().put("id", 2).put("name", "Whisky").put("origin", "Colombia");

    final Checkpoint checkpoint = testContext.checkpoint(2);

    testRequest(client.post("/api/whiskies"))
      .expect(
        statusCode(201),
        jsonBodyResponse(newWhisky)
      )
      .sendJson(newWhisky, testContext, checkpoint)
      .onComplete(ar -> // Executed after the first response is completed
        testRequest(client.put("/api/whiskies/2"))
          .expect(
            statusCode(200),
            jsonBodyResponse(updateWhisky)
          )
          .sendJson(updateWhisky, testContext, checkpoint)
      );
  }

  /*
  @Test
  void http_server_check_response(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> {
      HttpClient client = vertx.createHttpClient();
      client.request(HttpMethod.GET, 8080, "localhost", "/")
        .compose(req -> req.send().compose(HttpClientResponse::body))
        .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {

          testContext.completeNow();
        })));
    }));
  }
  */
}
