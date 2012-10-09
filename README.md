# dropbox-java-client

A OSGi based Java Client for the Dropbox REST Service without dependencies, 
runs on Google App Engine.

## Quick Facts
+ Easy to use, "fluent" API
+ Runs on Google App Engine (GAE)
+ Language: Java 6 SE or better
+ Tested with both Oracle JDK 6 and 7 and OpenJDK 6 and 7 (Last three VM's using [Travis CI Server](https://travis-ci.org/))
+ Build System: Maven 3
+ Apache License, Version 2.0

#### Example Usage:
First put `dropbox-java-client-{version}.jar` and the only dependency, `json-smart-1.1.1.jar`, 
in the class path.

``` java
/* 
Credentials are stored in a Properties File, format:
# properties for Dropbox application
# Application credentials are mandatory
dropbox.app.key={key}
dropbox.app.secret={secret token}
# Access credentials are optional. If not present the API offers operations the 
# get the Access credentials / tokens
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

Copyright (c) 2012 XIAM Solutions B.V.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0

[![Ohloh profile for ronaldmuller](https://www.ohloh.net/accounts/224392/widgets/account_tiny.gif)](https://www.ohloh.net/accounts/224392?ref=Tiny)
