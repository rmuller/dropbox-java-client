/* package-info.java
 * 
 * Created: Oct 09, 2012
 * Character encoding: UTF-8
 * 
 ********************************* LICENSE **********************************************
 * 
 * Copyright (c) 2012 - XIAM Solutions B.V. (http://www.xiam.nl)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * An easy to use, OSGi based Java client for the Dropbox REST API v1,  
 * runs on Google App Engine.
 * <br/><br/>
 * The main class in this package is {@link eu.infomas.dropbox.Dropbox}. 
 * It offers the primary interface (facade) to the 
 * <a href="https://www.dropbox.com/developers/reference/api">Dropbox REST API v1</a>.
 *
 * <h4>Why use this library?</h4>
 * Advantages of using this library instead of the 
 * <a href="https://www.dropbox.com/static/developers/dropbox-java-sdk-1.5.1-docs/index.html">
 * official implementation</a>:
 * <ul>
 * <li>Works (and tested) on Google App Engine (this was the primary reason for creating 
 * this library)</li>
 * <li>Swappable HTTP Client implementation</li>
 * <li>Smaller library, works default with {@code java.net} package</li>
 * <li>More clear (fluent) API</li>
 * <li>Artifact is OSGi compliant bundle</li>
 * <li>Faster because <a href="http://code.google.com/p/json-smart/">json-smart</a> is 
 * used instead of {@code json-simple}</li>
 * </ul>
 * 
 * <h4>Example. Get the first 8 bytes of a file.</h4>
 * <pre>
 * /*
 * Credentials are stored in a Properties File, format:
 * # Properties for Dropbox application
 * # Application credentials are mandatory
 * dropbox.app.key={key}
 * dropbox.app.secret={secret token}
 * # Access credentials are optional. If not present the API offers operations to 
 * # query Access credentials / tokens
 * dropbox.access.key={key}
 * dropbox.access.secret={secret token}
 * # Language is optional, default is "en"
 * dropbox.language=nl
 * *&#47;
 * final Dropbox dropbox = new Dropbox("~/.dropbox.config");
 * final ByteArrayOutputStream baos = new ByteArrayOutputStream(8);
 * dropbox.filesGet("dropbox file path")
 *     .withRange(0, 7) // from (inclusive) - to (inclusive)
 *     .toOutputStream(baos);
 * </pre>
 * <h4>Example. Request metadata for a file or directory.</h4>
 * <pre>
 * Dropbox dropbox = new Dropbox("path-to-your-dropbox-configuration-file");
 * Entry entry = dropbox.metadata("/path-to-file-or-directory")
 *     .withList()
 *     .asEntry();
 * // do something with the Entry
 * if (entry.getMimeType.startsWith("image/")) {
 *     ....
 * }
 * </pre>
 * 
 * @author <a href="mailto:rmuller@xiam.nl">Ronald K. Muller</a>
 * @since infomas-asl 3.0.2
 */
package eu.infomas.dropbox;
