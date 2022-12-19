package co.demo.whisky;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Main Class
 */
public class MainVerticle extends AbstractVerticle {

  /** Product List **/
  private Map<Integer, Whisky> products = new LinkedHashMap<>(0);

  private Integer port;

  /**
   * Start Vertx
   * @param promise The Promise
   */
  @Override
  public void start(Promise<Void> promise) {
    createInitialData();

    // Create a Router object.
    Router router = Router.router(vertx);

    // Bind "/"
    router.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response
        .putHeader("content-type", "text/html")
        .end("<h1>Welcome Application</h1>");
    });

    // Assets
    router.route("/assets/*").handler(StaticHandler.create("assets"));

    // BodyHandler
    router.route("/api/whiskies*").handler(BodyHandler.create());

    // Routes
    router.get("/api/whiskies").handler(this::getAll);
    router.post("/api/whiskies").handler(this::addOne);
    router.get("/api/whiskies/:id").handler(this::getOne);
    router.put("/api/whiskies/:id").handler(this::updateOne);
    router.delete("/api/whiskies/:id").handler(this::deleteOne);

    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(
        // Retrieve the port from the configuration, default to 8080.
        getPort(),
        result -> {
          if (result.succeeded()) {
            promise.complete();
          } else {
            promise.fail(result.cause());
          }
        }
      );
  }

  /**
   * Add Product
   * @param routingContext Routing Context
   */
  private void addOne(RoutingContext routingContext) {
    final Whisky whisky = Json.decodeValue(routingContext.body().asString(), Whisky.class);
    products.put(whisky.getId(), whisky);

    // Return the created whisky as JSON
    routingContext.response()
      .setStatusCode(201)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(whisky));
  }

  /**
   * Search Product by ID
   * @param routingContext  Routing Context
   */
  private void getOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      final Integer idAsInteger = Integer.valueOf(id);
      Whisky whisky = products.get(idAsInteger);
      if (whisky == null) {
        routingContext.response().setStatusCode(404).end();
      } else {
        routingContext.response()
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(whisky));
      }
    }
  }

  /**
   * Update Product
   * @param routingContext Routing Context
   */
  private void updateOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    JsonObject json = routingContext.body().asJsonObject();
    if (id == null || json == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      final Integer idAsInteger = Integer.valueOf(id);
      Whisky whisky = products.get(idAsInteger);
      if (whisky == null) {
        routingContext.response().setStatusCode(404).end();
      } else {
        whisky.setName(json.getString("name"));
        whisky.setOrigin(json.getString("origin"));
        routingContext.response()
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(whisky));
      }
    }
  }

  /**
   * Delete Product
   * @param routingContext Routing Context
   */
  private void deleteOne(RoutingContext routingContext) {
    String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      Integer idAsInteger = Integer.valueOf(id);
      products.remove(idAsInteger);
    }
    routingContext.response().setStatusCode(204).end();
  }

  /**
   * Get All Products
   * @param routingContext Routing Context
   */
  private void getAll(RoutingContext routingContext) {
    routingContext.response()
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(products.values()));
  }

  /**
   * Initial Data
   */
  private void createInitialData() {
    Whisky bowmore = new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay");
    products.put(bowmore.getId(), bowmore);
    Whisky talisker = new Whisky("Talisker 57° North", "Scotland, Island");
    products.put(talisker.getId(), talisker);
  }

  /**
   * Get Port
   * @return
   */
  public int getPort() {
    if(Objects.isNull(port)){
        port = config().getInteger("http.port", 8080);
    }
    return port;
  }
}

