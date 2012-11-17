package org.eranmedan;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eranmedan.mixpanel.MixpanelAPI;

public class MixpanelAPITest {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testTrack() {
    String uniqueId = "50479b24671bf";
    String nameTag = "Test Name";
    String ip = "123.123.123.123";
    Date time = new Date();
    String token = "e3bc4100330c35722740fb8c6f5abddc";
    Map<String, String> props = new HashMap<String, String>();
    props.put("action", "play");

    MixpanelAPI mixpanelAPI = new MixpanelAPI(token);

    checkResult(mixpanelAPI.track("test1", uniqueId, props));
    checkResult(mixpanelAPI.track("test2", uniqueId, nameTag, ip, time, props));
    checkResult(mixpanelAPI.track("test3", uniqueId, nameTag, ip, time));
    checkResult(mixpanelAPI.track("test4", uniqueId, nameTag, ip));
    checkResult(mixpanelAPI.track("test5", uniqueId, nameTag));
    checkResult(mixpanelAPI.track("test6", uniqueId));

    Future<Boolean> track = mixpanelAPI.track("test7", uniqueId);
    mixpanelAPI.close();
    mixpanelAPI.awaitTermiation(10, TimeUnit.SECONDS);
    checkResult(track);

  }

  @Test
  public void testFail() {
    String uniqueId = "50479b24671bf";
    String nameTag = "Test Name";
    String ip = "123.123.123.123";
    Date time = new Date();
    String token = "e3bc4100330c35722740fb8c6f5abddc";
    Map<String, String> props = new HashMap<String, String>();
    props.put("action", "play");

    MixpanelAPI mixpanelAPI = new MixpanelAPI(token);

    checkFailedResult(mixpanelAPI.track("test1", null, props));
    checkFailedResult(mixpanelAPI.track(null, uniqueId, nameTag, ip, time, props));

    mixpanelAPI.close();
    mixpanelAPI.awaitTermiation(10, TimeUnit.SECONDS);

  }

  private void checkFailedResult(Future<Boolean> track) {
    try {
      track.get();
      fail("Exception expected");
    } catch (InterruptedException e) {

    } catch (ExecutionException e) {

    }
  }

  private void checkResult(Future<Boolean> track) {
    try {
      Boolean result = track.get();
      assertTrue(result);
    } catch (InterruptedException e) {
      fail(e.getMessage());
    } catch (ExecutionException e) {
      fail(e.getMessage());
    }
  }

}
