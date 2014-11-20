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

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 17, 2014  
 */
public class Message {
  
  private String              notifId;
  
  private String              from;

  private String              to;

  private String              pluginId;

  private Map<String, String> ownerParameter;
  
  private String              message;
  
  public Message(final String to, final String pluginId) {
    this.to = to;
    this.pluginId = pluginId;
  }

  public Message() {
  }

  public String getNotifId() {
    return notifId;
  }

  public void setNotifId(String notifId) {
    this.notifId = notifId;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getPluginId() {
    return pluginId;
  }

  public void setPluginId(String pluginId) {
    this.pluginId = pluginId;
  }

  public Map<String, String> getOwnerParameter() {
    return ownerParameter;
  }

  public void setOwnerParameter(Map<String, String> ownerParameter) {
    this.ownerParameter = ownerParameter;
  }
  
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String buildStringFromMap(Map<String, String> ownerParameter) {
    StringBuilder sb = new StringBuilder();
    boolean hasNext = false;
    for (Entry<String, String> entry : ownerParameter.entrySet()) {
      if (hasNext) {
        sb.append(", ");
      }
      sb.append(entry.getKey()).append(" : ").append(entry.getValue());
      hasNext = ownerParameter.size() > 1;
    }
    return sb.toString();
  }
  
  public Map<String, String> buildMapFromString(String input) {
    Map<String, String> ownerParameter = new HashMap<String, String>();
    for (String element : input.split(",")) {
      String[] entry = element.split(":");
      ownerParameter.put(entry[0], entry.length > 1 ? entry[1] : "");
    }
    return ownerParameter;
  }
  
  public String toString() {
    return to + " : " + pluginId;
  }
  
  public static class MessageEncoder implements Encoder.Text< Message > {
    @Override
    public void init(final EndpointConfig config) {
    }

    @Override
    public String encode(final Message message) throws EncodeException {
        return Json.createObjectBuilder()
            .add("notifId", message.getNotifId())
            .add("to", message.getTo())
            .add("pluginId", message.getPluginId())
            .add("ownerParameter", message.buildStringFromMap(message.getOwnerParameter()))
            .add("message", message.getMessage())
            .build()
            .toString();
    }

    @Override
    public void destroy() {
    }
}
  
  public static class MessageDecoder implements Decoder.Text< Message > {
    private JsonReaderFactory factory = Json.createReaderFactory(Collections.< String, Object >emptyMap());

    @Override
    public void init(final EndpointConfig config) {
    }

    @Override
    public Message decode(final String str) throws DecodeException {
        final Message message = new Message();

        try(final JsonReader reader = factory.createReader(new StringReader(str))) {
            final JsonObject json = reader.readObject();
            message.setNotifId(json.getString("notifId"));
            message.setTo(json.getString("to"));
            message.setPluginId(json.getString("pluginId"));
            message.setOwnerParameter(message.buildMapFromString(json.getString("ownerParameter")));
            message.setMessage(json.getString("message"));
        }

        return message;
    }

    @Override
    public boolean willDecode(final String str) {
        return true;
    }

    @Override
    public void destroy() {
    }
  }

}
