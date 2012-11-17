package com.eranmedan.mixpanel;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A dead simple Mixpanel track API for Java
 * <p>
 * <b>Example Usage:</b>
 * </p>
 * 
 * <blockquote>
 * 
 * <pre>
 * 
 * String uniqueId = &quot;50479b24671bf&quot;;
 * String nameTag = &quot;Test Name&quot;;
 * String ip = &quot;123.123.123.123&quot;;
 * Date time = new Date();
 * String token = &quot;e3bc4100330c35722740fb8c6f5abddc&quot;;
 * Map&lt;String, String&gt; props = new HashMap&lt;String, String&gt;();
 * props.put(&quot;action&quot;, &quot;play&quot;);
 * Logger logger = LoggerFactory.getLogger(&quot;MixpanelAPI Test Logger&quot;);
 * 
 * MixpanelAPI mixpanelAPI = new MixpanelAPI(token, logger);
 * 
 * mixpanelAPI.track(&quot;test1&quot;, uniqueId, props);
 * mixpanelAPI.track(&quot;test2&quot;, uniqueId, nameTag, ip, time, props);
 * mixpanelAPI.track(&quot;test3&quot;, uniqueId, nameTag, ip, time);
 * mixpanelAPI.track(&quot;test4&quot;, uniqueId, nameTag, ip);
 * mixpanelAPI.track(&quot;test5&quot;, uniqueId, nameTag);
 * mixpanelAPI.track(&quot;test6&quot;, uniqueId);
 * 
 * 
 *  
 * </pre>
 * </blockquote>
 * 
 * <b>For example, <code>test2</code> will send a message of this format:</b>
 * 
 * <pre>
 * {   "event": "test2", 
 *     "properties": {
 *         "distinct_id": "50479b24671bf", 
 *         "ip": "123.123.123.123", 
 *         "token": "e3bc4100330c35722740fb8c6f5abddc", 
 *         "time": 1245613885,
 *         "mp_name_tag": "Test Name",  
 *         "action": "play"
 *         
 *     }
 * }
 * </pre>
 * 
 * <b>Note:</b> In most use cases you can ignore the return value of the <code>Future</code> returned for performance. The Future is mostly for testing purposes
 * 
 * @version 0.1
 * @author Eran Medan
 * @see <a
 *      href="https://mixpanel.com/docs/api-documentation/http-specification-insert-data">https://mixpanel.com/docs/api-documentation/http-specification-insert-data</a>
 */
public class MixpanelAPI {
  private static final String MIXPANEL_API_ENDPOINT = "http://api.mixpanel.com/track/?data=";
  private final String token;
  private final Logger logger;
  private final ExecutorService threadPool;

  /**
   * @see #MixpanelAPI(String token, Logger logger, ExecutorService threadPool)
   * 
   */
  public MixpanelAPI(String token) {
    this(token, null);
  }
  
  /**
   * @see #MixpanelAPI(String token, Logger logger, ExecutorService threadPool)
   * 
   */
  public MixpanelAPI(String token, Logger logger) {
    // TODO: isn't a fixed threadpool based on
    // Runtime.getRuntime().availableProcessors() better?
    this(token, logger, Executors.newCachedThreadPool());
  }

  /**
   * Create a new MixpanelAPI object (usually, there is no need for more than one)
   * 
   * @param token the MixPanel API token
   * @param logger an optional Logger, if none provided a {@link NOPLogger} is provided
   * @param threadPool an optional custom ExecutorService to queue the asynchronous HTTP calls to Mixpanel's API, if none provided a <code>Executors.newCachedThreadPool()</code> is used
   */
  
  public MixpanelAPI(String token, Logger logger, ExecutorService threadPool) {
    this.token = token;
    this.logger = (logger == null) ? NOPLogger.NOP_LOGGER : logger;
    this.threadPool = threadPool;
  }

/**
 * @see #track(String event, String nameTag, HttpServletRequest request, String cookieName, Map additionalProperties)
 */
  public void track(String event, String nameTag, HttpServletRequest request, String cookieName) {
    track(event, nameTag, request, cookieName, null);
  }

  /**
   * Track an event 
   * 
   * @param request the request object, will be used to deduce the IP address and Mixpanel cookie for the unique ID 
   * @param cookieName the mixpanel cookie name, e.g. if this is your setup: <pre>
   * mixpanel.init(token, {
        cookie_expiration: 365,
        cookie_name: "foobar"
     }
   * </pre>
   * then the cookie name is actually <code>mp_foobar</code>
   * 
   * @see #track(String event, String distinctId, String nameTag, String ip, Date time, Map additionalProperties)
   */
  public Future<Boolean> track(String event, String nameTag, HttpServletRequest request, String cookieName, Map<String, String> additionalProperties) {
    Cookie mixpanelCookie = findCookieByName(request, cookieName);
    String uniqueId = null;
    String ip = getClientIpAddr(request);
    if (mixpanelCookie != null) {
      String cookieValue = mixpanelCookie.getValue();
      String result;
      try {
        result = URLDecoder.decode(cookieValue, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
      JsonParser jp = new JsonParser();

      JsonElement mixJason = jp.parse(result);
      uniqueId = mixJason.getAsJsonObject().get("distinct_id").getAsString();
    } else {
      logger.warn("Unique ID for mixpanel cookie name: " + cookieName + " was not found, using IP instead");
      uniqueId = ip;
    }

    return track(event, uniqueId, nameTag, ip, null, null);
  }
  
  /**
   * @see #track(String event, String distinctId, String nameTag, String ip, Date time, Map additionalProperties)
   */
  public Future<Boolean> track(String event, String distinctId) {
    return track(event, distinctId, null, null, null, null);
  }

  /**
   * @return 
   * @see #track(String event, String distinctId, String nameTag, String ip, Date time, Map additionalProperties)
   */
  public Future<Boolean> track(String event, String distinctId, Map<String, String> additionalProperties) {
    return track(event, distinctId, null, null, null, additionalProperties);
  }

  /**
   * @see #track(String event, String distinctId, String nameTag, String ip, Date time, Map additionalProperties)
   */
  public Future<Boolean> track(String event, String distinctId, String nameTag) {
    return track(event, distinctId, nameTag, null, null, null);
  }

  /**
   * @see #track(String event, String distinctId, String nameTag, String ip, Date time, Map additionalProperties)
   */
  public Future<Boolean> track(String event, String distinctId, String nameTag, String ip) {
    return track(event, distinctId, nameTag, ip, null, null);
  }

  /**
   * @see #track(String event, String distinctId, String nameTag, String ip, Date time, Map additionalProperties)
   */
  public Future<Boolean> track(String event, String distinctId, String nameTag, String ip, Date time) {
    return track(event, distinctId, nameTag, ip, time, null);
  }
  
/**
 * Tracks an event 
 * 
 * @param event the (required) event name 
 * @param distinctId (required) the user's distinct mixpanel ID (usually stored in a cookie) or any string that uniquely can identify a user. e.g. the user id.
 * @param nameTag (optional) is the way to set a name for a given user for our streams feature. You can set this to any string value like an email, first and last name, or username.
 * @param ip (optional) is a raw string IP Address (e.g. "127.0.0.1") that you pass to our API. This is largely useful if you're making requests from your backend and would like geolocation processing done on your requests otherwise it's safe to use the &ip=1 parameter described in the docs that is outside of the encoded data string. 
 * @param time is the time at which the event occured, it must be a unix timestamp, requests will be rejected that are 5 days older than codesent time - this is done for security reasons as your token is public generally. Format is seconds since 1970, GMT time zone. If you'd like to import data, you can through a special API for any event.
 * @param additionalProperties additional custom properties in a name-value map
 * @return a {@link Future} object returning a Boolean when calling it's <code>get()</code> method, true means a successful call (at the moment, true is the only possible return value, any error will cause an {@link ExecutionException} to be thrown when calling the future's get method). <b>Note:</b> In most use cases you can ignore the return value of the <code>Future</code> returned for performance. The Future is mostly for testing purposes   
 */

  public Future<Boolean> track(final String event, final String distinctId, final String nameTag, final String ip, final Date time, final Map<String, String> additionalProperties) {

    return threadPool.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        logger.warn("test the test");
        if (event == null) {
          throw new RuntimeException("event field is mandatory");
        }
        if (distinctId == null) {
          throw new RuntimeException("distinctId field is mandatory");
        }

        JsonObject jo = new JsonObject();
        jo.addProperty("event", event);
        JsonObject properties = new JsonObject();
        jo.add("properties", properties);
        properties.addProperty("distinct_id", distinctId);
        if (ip != null) {
          properties.addProperty("ip", ip);
        }
        properties.addProperty("token", token);
        if (time != null) {
          properties.addProperty("time", time.getTime() / 1000L);
        }
        if (nameTag != null) {
          properties.addProperty("mp_name_tag", nameTag);
        }
        if (additionalProperties != null) {
          for (Entry<String, String> entry : additionalProperties.entrySet()) {
            properties.addProperty(entry.getKey(), entry.getValue());
          }
        }
        final String message = jo.toString();
        byte[] bytes = message.getBytes();
        String encode = DatatypeConverter.printBase64Binary(bytes);

        logger.debug("Mixpanel message to be sent: " + message);
        final String url = MIXPANEL_API_ENDPOINT + encode;
        logger.debug("Mixpanel URL to call: " + url);
        URL apiURL;

        try {
          apiURL = new URL(url);
          HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
          int statusCode = connection.getResponseCode();
          InputStream inputStream = connection.getInputStream();
          String contentEncoding = connection.getContentEncoding();
          String responseBody = IOUtils.toString(inputStream, contentEncoding);
          if (statusCode == 200) {
            if (responseBody.equals("1")) {
              logger.debug("Mixpanel event reported successfully");
              return true;
            } else {
              String warningMessage = "Mixpanel event not reported successfully. Response Body: " + responseBody + " message: \n" + message + ". url: " + url;
              logger.warn(warningMessage);
              throw new Exception(warningMessage);
            }
          } else {
            String warningMessage = "Mixpanel response not 200: " + statusCode;
            logger.warn(warningMessage);
            throw new Exception(warningMessage);
          }
        } catch (MalformedURLException e) {
          String warningMessage = "Mixpanel URL is malformed: " + e.getMessage();
          logger.warn(warningMessage, e);
          throw new Exception(warningMessage, e);
        } catch (IOException e) {
          String warningMessage = "Mixpanel IO Exception: " + e.getMessage();
          logger.warn(warningMessage, e);
          throw new Exception(warningMessage, e);
        }
      }
    });
  }

  public void close() {
    if (!threadPool.isShutdown()) {
      threadPool.shutdown();
    }
    // no need to threadPool.awaitTermination, let it end when it ends, just
    // stop accepting new tasks.

  }

  public void awaitTermiation(long timeout, TimeUnit unit) {
    try {
      threadPool.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      logger.warn("Didn't terminate after 10 seconds");
    }

    // no need to threadPool.awaitTermination, let it end when it ends, just
    // stop accepting new tasks.

  }

  @Override
  protected void finalize() throws Throwable {
    try {
      close();
    } finally {
      super.finalize();
    }
  }

  private static String getClientIpAddr(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    return ip;
  }

  private static Cookie findCookieByName(HttpServletRequest request, String cookieName) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(cookieName)) {
          return cookie;
        }
      }
    }
    return null;
  }
}
