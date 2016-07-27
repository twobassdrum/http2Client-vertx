package com.twobassdrum;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;

public class Http2Client extends AbstractVerticle {
  @Override
  public void start() {
      HttpServer server = vertx.createHttpServer(new HttpServerOptions());
      server.requestHandler(req -> {
          final LatencyTimer myTimer = new LatencyTimer();

          String path = req.path();
          HttpServerResponse resp = req.response();
          HttpVersion httpVersion = HttpVersion.HTTP_2;

          resp.setChunked(true);
          if ("/".equals(path)) {

              HttpClientOptions options = new HttpClientOptions()
                      .setSsl(false)
                      .setUseAlpn(false)
                      .setProtocolVersion(httpVersion)
                      .setTrustAll(true);
              options.setLogActivity(true);
              options.setHttp2MaxPoolSize(50);
              options.setHttp2MultiplexingLimit(50);
              //options.setMaxWaitQueueSize(100);
              //options.setKeepAlive(true);
              //System.out.println(options.getInitialSettings().toString());

              HttpClient client = vertx.createHttpClient(options);
              HttpClientRequest request = client.get(config().getInteger("target.port", 8888), config().getString("target.hostname", "localhost"), "/", respo -> {
                  System.out.println("Got response " + respo.statusCode() + " with protocol " + respo.version() + " " + myTimer.latency() + "ms");
                  respo.bodyHandler(body -> {
                      System.out.println("Got data " + body.toString("UTF-8"));
                      resp.write("<h1>main " + myTimer.latency() + "ms</h1><textarea>" + body + "</textarea>");
                      resp.write("</body></html>");
                      resp.end();
                  });
              });
              request.exceptionHandler(a -> {
                  System.out.println("Connection Error" + a.getMessage());
              });

              // Set handler for server side push
              request.pushHandler(pushedReq -> {
                  System.out.println("Receiving pushed content " + myTimer.latency() + "ms");
                  pushedReq.handler(pushedResp -> {
                      System.out.println("Server pushed " + pushedReq.path() + " " + myTimer.latency() + "ms");
                      pushedResp.bodyHandler(body -> {
                          System.out.println("Got pushed data " + body.toString("UTF-8"));
                          resp.write("<h1>pushed " + myTimer.latency() + "ms</h1><textarea>" + body + "</textarea>");
                      });
                  });
              });
              request.end();
              System.out.println("\n\nrequest sent\n\n\n");
              resp.putHeader("content-type", "text/html");
              resp.write("<html><body>");
          } else {
              System.out.println("Not found " + path);
              resp.setStatusCode(404).end();
          }
      });

      server.listen(config().getInteger("http.port", 8080), ar -> {
          if (ar.succeeded()) {
              System.out.println("Server started");
          } else {
              ar.cause().printStackTrace();
          }
      });

  }
}
class LatencyTimer {
    long start;
    public LatencyTimer() {
        this.start = System.currentTimeMillis();
    }
    public long latency() {
        return System.currentTimeMillis() - this.start;
    }
}

