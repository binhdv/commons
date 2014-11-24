package org.exoplatform.commons.notification.impl.service;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class Metrics {
   private static final Logger logger = LoggerFactory.getLogger(Metrics.class);
   private static final ThreadLocal<NumberFormat> FMT = new ThreadLocal<NumberFormat>() {
      @Override public NumberFormat initialValue() {
         return new DecimalFormat("#0.000");
      }
   };

   private static final String format(double d) {
      return FMT.get().format(d);
   }

   public class MetricResultSummary {
      public final double averageResponseTime;
      public final double averageSize;

      public MetricResultSummary() {
         this.averageResponseTime = messagesReceived.get() > 0 ? totalTime.get() / messagesReceived.get() : 0;
         this.averageSize = messagesReceived.get() > 0 ? totalPayloadSize.get() / messagesReceived.get() : 0;
      }
      @Override
      public String toString() {
         return format(this.averageResponseTime) + "," + format(this.averageSize) + "," + messagesReceived.get() + "," + format(tps()) + "," + format((System.currentTimeMillis()-started)/1000);
      }
   }
   //
   private static final ObjectMapper jsonMapper = new ObjectMapper();
   private static final JsonFactory jsonFactory = jsonMapper.getFactory();
   private final long started = System.currentTimeMillis();
   private AtomicLong totalTime = new AtomicLong();
   private AtomicLong totalPayloadSize = new AtomicLong();
   private AtomicLong messagesReceived = new AtomicLong();
   private AtomicLong[] transPerMinute = new AtomicLong[60];


   public Metrics() {
       for (int i=0; i<transPerMinute.length; i++) {
           transPerMinute[i] = new AtomicLong();
       }
   }

   public double tps() {
       AtomicLong[] topTps = new AtomicLong[3];
       for (int i=0; i<transPerMinute.length; i++) {
           for (int j=0; j<topTps.length; j++) {
               if (topTps[j] == null || topTps[j].get() < transPerMinute[i].get()) {
                   topTps[j] = transPerMinute[i];
               }
           }
       }
       int count = 0;
       double sum = 0;
       for (int i=0; i<topTps.length; i++) {
           if (topTps[i] != null) {
               sum += topTps[i].get();
               count++;
           }
       }
       return count > 0 ? sum / (60*count) : 0;
   }


   public void addTo(Metrics target) {
      target.totalTime.addAndGet(this.totalTime.get());
      target.totalPayloadSize.addAndGet(this.totalPayloadSize.get());
      target.messagesReceived.addAndGet(this.messagesReceived.get());
   }

   //
   public double getAverageResponseTime() {
      return this.totalTime.get() / this.messagesReceived.get();
   }


   public MetricResultSummary getSummary() {
      return new MetricResultSummary();
   }


   @Override
   public String toString() {
      return new MetricResultSummary().toString();
   }
  
   public void update(long sendTime, long payloadSize) {
      Calendar calendar = Calendar.getInstance();
      long elapsed = calendar.getTime().getTime() - sendTime;
      this.messagesReceived.incrementAndGet();
      this.totalTime.addAndGet(elapsed);
      this.totalPayloadSize.addAndGet(payloadSize);
      int ndx = calendar.get(Calendar.MINUTE);
      transPerMinute[ndx].incrementAndGet();
   }
   public String update(String jsonText) {
      try {
          if (!jsonText.contains("timestamp")) {
            logger.warn("Received unknown message " + jsonText);
            return null;
          }
          JsonParser jp = jsonFactory.createParser(jsonText);
          JsonNode actualObj = jsonMapper.readTree(jp);
          if (actualObj.get("timestamp") == null) {
              return null;
          }
          long timestamp = actualObj.get("timestamp").asLong();
          update(timestamp, jsonText.length());
          return actualObj.get("identifier").asText();
       } catch (IOException e) {
          throw new RuntimeException("Failed to parse [" + jsonText + "]", e);
       }
   }
   //
   public static String getHeading() {
      return "average response (ms), average size, messages, tps, walltime(secs)";
   }

}
