/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.commons.notification.impl.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.platform.Verticle;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 24, 2014  
 */
public class VertXServerEndpoint extends Verticle implements Startable {

  private static final Logger logger = LoggerFactory.getLogger(VertXServerEndpoint.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private final JsonFactory jsonFactory = mapper.getFactory();
  private static final Map<String, Set<ServerWebSocket>> subscriptions = new ConcurrentHashMap<String, Set<ServerWebSocket>>();
  
  public VertXServerEndpoint() {
  }
  @Override
  public void start() {
    Vertx vertx = VertxFactory.newVertx();
    vertx.createHttpServer().setAcceptBacklog(10000).websocketHandler(new Handler<ServerWebSocket>() {
           public void handle(final ServerWebSocket ws) {
             if (ws.path().equals("/notify")) {
               ws.dataHandler(new Handler<Buffer>() {
                 public void handle(Buffer data) {
                   try {
                     logger.info(data.toString());
                     JsonParser jp = jsonFactory.createParser(data.toString());
                     JsonNode jsonObj = mapper.readTree(jp);
                     String action = jsonObj.get("action").asText();
                     String identifier = jsonObj.get("identifier").asText();
                     if (action == null || identifier == null) {
                       return;
                     }
                     if ("subscribe".equals(action)) {
                       Set<ServerWebSocket> sockets = subscriptions.get(identifier);
                       if (sockets == null) {
                         sockets = new HashSet<ServerWebSocket>();
                       }
                       sockets.add(ws);
                       subscriptions.put(identifier, sockets);
                     } else if ("unsubscribe".equals(action)) {
                       Set<ServerWebSocket> sockets = subscriptions.get(identifier);
                       if (sockets == null) {
                         return;
                       }
                       sockets.remove(ws);
                       if (sockets.size() == 0) {
                         subscriptions.remove(identifier);
                       }
                       logger.info(ws.textHandlerID() + " is closed");
                     }
                   } catch (IOException e) {
                     logger.error("Failed to handle " + data, e);
                   }
                 }
               });
               ws.closeHandler(new Handler<Void>() {

                 @Override
                 public void handle(Void event) {
                   ws.close();
                   logger.info("textHandlerID:" + ws.textHandlerID() + " is closed");
                 }
               });
             } else {
               ws.reject();
             }
           }
      }).requestHandler(new Handler<HttpServerRequest>() {
          public void handle(HttpServerRequest req) {
              //if (req.path.equals("/")) req.response.sendFile("websockets/ws.html"); // Serve the html
          }
      }).listen(8181, "localhost");
  }

  /**
   * 
   */
  public void sendMessageForRandomIdentifier(String message, String identifier) {
    if (subscriptions.size() == 0) {
      return;
    }
    if (identifier == null) {
      return;
    }
    Set<ServerWebSocket> sockets = subscriptions.get(identifier);
    if (sockets == null || sockets.size() == 0) {
      return;
    }
    for (ServerWebSocket ws : sockets) {
      try {
        ws.writeTextFrame(message);
      } catch (Exception e) {
        subscriptions.remove(ws);
        sockets.remove(ws);
        if (sockets.size() == 0) {
          subscriptions.remove(identifier);
        }
        logger.warn(e.getMessage());
      }
    }
  }
}
