# dropbox-java-client

[![Build Status](https://secure.travis-ci.org/rmuller/dropbox-java-client.png)](http://travis-ci.org/rmuller/dropbox-java-client)

An easy to use, OSGi based Java client for the Dropbox REST API v1, runs on Google App Engine.

#### Current status: Beta
API stabilized. Almost all REST Services implemented. In production since March 2013.
We only need to do some cleanup and then this library will be available from 
The Central (Maven) Repository.

## Quick Facts
+ Simple easy to use, "fluent" API
+ Very fast
+ Lightweight, one dependency (noggit JSON parser), only 36 kb jar
+ Runs on Google App Engine (GAE)
+ Language: Java 6 SE or better
+ Tested with Oracle JDK 6, 7, 8 and OpenJDK 6 and 7 (Last four VM's using 
  [Travis CI Server](https://travis-ci.org/))
+ Standard JUL Logging (`java.util.logging`)
+ OSGi Bundle artifact
+ Apache License, Version 2.0
+ Build System: Maven 3

#### Example Usage:
First put `dropbox-java-client-{version}.jar` and `noggit-0.5.jar` in the class path.

For more information, see [Javadoc](http://rmuller.github.com/dropbox-java-client/)

``` java
/* 
Credentials are stored in a Properties File, format:
# properties for Dropbox application
# Application credentials are mandatory
dropbox.app.key={key}
dropbox.app.secret={secret token}
# Access credentials are optional. If not present the API offers operations to 
# query Access credentials / tokens
dropbox.access.key={key}
dropbox.access.secret={secret token}
# Language is optional, default is "en"
dropbox.language=nl
*/
final Dropbox dropbox = new Dropbox("~/.dropbox.config");
// Get the first 8 bytes of a file
final ByteArrayOutputStream baos = new ByteArrayOutputStream(8);
dropbox.filesGet("dropbox file path")
    .withRange(0, 7) // from (inclusive) - to (inclusive)
    .toOutputStream(baos);
```

## License

Copyright (c) 2012 - 2014 XIAM Solutions B.V.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0

[![Ohloh profile for ronaldmuller](https://www.ohloh.net/accounts/224392/widgets/account_tiny.gif)](https://www.ohloh.net/accounts/224392?ref=Tiny)
