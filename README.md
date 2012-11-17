mixpanel-java
=============

A simple Mixpanel API for Java


Installation
------------

    <repositories>
      ...
      <repository>
        <id>github-repo</id>
        <url>http://raw.github.com/eranation/mixpanel-java/master/localrepo</url>
      </repository>
    </repositories>
    ...
    <dependencies>
      ...
      <dependency>
        <groupId>org.eranmedan</groupId>
        <artifactId>mixpanel-java</artifactId>
        <version>0.0.2-SNAPSHOT</version>
      </dependency>
    </dependencies>
    

Example usage
-------------

     String uniqueId = "50479b24671bf";
     String nameTag = "Test Name";
     String ip = "123.123.123.123";
     Date time = new Date();
     String token = "e3bc4100330c35722740fb8c6f5abddc";
     Map<String, String> props = new HashMap<String, String>();
     props.put("action", "play");
     Logger logger = LoggerFactory.getLogger("MixpanelAPI Test Logger");
     
     MixpanelAPI mixpanelAPI = new MixpanelAPI(token, logger);
     
     mixpanelAPI.track("test1", uniqueId, props);
     mixpanelAPI.track("test2", uniqueId, nameTag, ip, time, props);
     mixpanelAPI.track("test3", uniqueId, nameTag, ip, time);
     mixpanelAPI.track("test4", uniqueId, nameTag, ip);
     mixpanelAPI.track("test5", uniqueId, nameTag);
     mixpanelAPI.track("test6", uniqueId);


Tests
-----

    # in the src/test/java directory
    
    mvn test

Attribution/Credits
-------------------

- Mixpanel's documentation: https://mixpanel.com/docs/api-documentation/http-specification-insert-data
- Readme format copied from https://github.com/carlsverre/mixpanel-node  
- Inspired by the idea of the low priority filter from: https://github.com/scalascope/mixpanel-java/blob/master/src/main/java/com/mixpanel/java/mpmetrics/LowPriorityThreadFactory.java

Copyright (c) 2012 Eran Medan

License
-------------------

Released under the MIT license.  See file called LICENSE for more details.