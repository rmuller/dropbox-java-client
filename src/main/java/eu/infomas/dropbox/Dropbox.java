/* DropBox.java
 * 
 * Created: Oct 01, 2012
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
package eu.infomas.dropbox;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import java.io.InputStream;
import java.io.OutputStream;

import static eu.infomas.dropbox.Utils.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import net.minidev.json.JSONValue;

/**
 * {@code DropBox} offers an easy to use interface for working with the 
 * <a href="https://www.dropbox.com/developers/reference/api">Dropbox REST API v1</a>.
 * All operations (REST Requests) are done via this API.
 * <br/>
 * Dropbox does use <a href="http://tools.ietf.org/html/rfc5849">the OAuth 1.0
 * Protocol</a> which is also supported by this class.
 * <br/>
 * Note that this class is <b>not</b> thread-safe!
 * {@code Dropbox} instances are cheap to create, so do not cache these instances,
 * just create a new instance when needed, use it and throw it away!
 * <br/>
 * The actual HTTP communication is delegated to {@link RestClient}, which can easily
 * use a different implementation than the default provided by using the
 * Java {@link ServiceLoader service-provider loading facility}.
 * 
 * @author <a href="mailto:rmuller@xiam.nl">Ronald K. Muller</a>
 * @since infomas-asl 3.0.2
 */
public final class Dropbox {

    /**
     * The Dropbox REST API version number.
     */
    public static final String API_VERSION = "1";
    
    /**
     * The Dropbox Server address for Authentication and meta data.
     */
    public static final String API_SERVER = "api.dropbox.com";
    
    /**
     * The Dropbox Server address for file operations (operations on content).
     */
    public static final String CONTENT_SERVER = "api-content.dropbox.com";
    
    // keys used in Properties file
    
    /**
     * The name (key) of the property, stored in the {@code Properties} file, for
     * the application key (or identifier).
     */    
    public static final String CLIENT_CREDENTIALS_KEY = "dropbox.app.key";
    
    /**
     * The name (key) of the property, stored in the {@code Properties} file, for
     * the application secret token.
     */        
    public static final String CLIENT_CREDENTIALS_SECRET = "dropbox.app.secret";
    
    /**
     * The name (key) of the property, stored in the {@code Properties} file, for
     * the request key.
     */  
    public static final String TOKEN_CREDENTIALS_KEY = "dropbox.access.key";
    
    /**
     * The name (key) of the property, stored in the {@code Properties} file, for
     * the request secret token.
     */      
    public static final String TOKEN_CREDENTIALS_SECRET = "dropbox.access.secret";
    
    /**
     * The name (key) of the property, stored in the {@code Properties} file, for
     * the language used by the request.
     * If not specified, the default language "en" (English) is used.
     */        
    public static final String LANGUAGE_KEY = "dropbox.language";
    
    private static final Logger LOG = Logger.getLogger(Dropbox.class.getName());

    private final RestClient restClient = RestClient.newInstance();
    private Credentials clientCredentials;
    private String authorization;
    private Locale locale = Locale.ENGLISH;
        
    // Dropbox instance creation & configuration ========================================
    
    /**
     * Create a new {@code Dropbox} instance.
     * This is a low level interface, in practice you mostly will use
     * {@link #Dropbox(String)} or {@link #Dropbox(Properties)}.
     * 
     * @param clientCredentials The client (application) credentials. Mandatory.
     * @param tokenCredentials The token (access or request) credentials, may be 
     * {@code null} in which case the token credentials must be requested by using
     * {@link #requestTemporaryCredentials() }
     */
    public Dropbox(final Credentials clientCredentials, final Credentials tokenCredentials) {
        this.clientCredentials = clientCredentials;
        if (tokenCredentials != null) {
            setClientCredentials(tokenCredentials);
        }
    }
    
    /**
     * Create a new {@code Dropbox} instance, using the configuration data supplied
     * by the {@code Properties} object.
     * The following property names are recognized:
     * <ul>
     * <li>{@link #CLIENT_CREDENTIALS_KEY}</li>
     * <li>{@link #CLIENT_CREDENTIALS_SECRET}</li>
     * <li>{@link #TOKEN_CREDENTIALS_KEY}</li>
     * <li>{@link #TOKEN_CREDENTIALS_SECRET}</li>
     * <li>{@link #LANGUAGE_KEY}</li>
     * </ul>
     */
    public Dropbox(final Properties configuration) {
        this(
            Credentials.of(
                configuration.getProperty(CLIENT_CREDENTIALS_KEY),
                configuration.getProperty(CLIENT_CREDENTIALS_SECRET)),
            Credentials.of(
                configuration.getProperty(TOKEN_CREDENTIALS_KEY),
                configuration.getProperty(TOKEN_CREDENTIALS_SECRET))
            );
        if (configuration.containsKey(LANGUAGE_KEY)) {
            this.locale = new Locale(configuration.getProperty(LANGUAGE_KEY));
        }
    }
    
    /**
     * Create a new {@code Dropbox} instance, using the configuration data supplied
     * by the {@code Properties} file found at the specified location.
     * 
     * @param configurationPath The path where the configuration {@code Properties} file
     * can be found.
     * If a relative 'configurationPath' is specified, the configuration is loaded 
     * relative to the current user ({@code user.dir}) directory. If the path starts 
     * with "~/" the configuration file is loaded relative to the current user home 
     * ({@code user.home}) directory.
     * If the prefix {@code classpath:} is used, the 'configurationPath' is loaded from 
     * the class path. 
     * May not be empty or {@code null}.
     * 
     * @see #Dropbox(java.util.Properties) 
     */
    public Dropbox(final String configurationPath) throws IOException {
        this(loadProperties(configurationPath, Dropbox.class));
    }
    
    /**
     * Configure the language to use when the service endpoints are called.
     * Default value is {@link Locale#ENGLISH}.
     */
    public Dropbox withLocale(final Locale locale) {
        this.locale = locale;
        return this;
    }

    /**
     * Return the configured {@link Locale}.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * After a {@code Dropbox} client is created, you are able to set the request or
     * token {@code Credentials} once. These {@code Credentials} are returned by 
     * {@link #requestTokenCredentials(eu.infomas.dropbox.Credentials) }.
     */
    public void setClientCredentials(final Credentials tokenCredentials) {
        if (tokenCredentials == null) {
            throw new IllegalArgumentException("'tokenCredentials' is null");
        }
        if (this.authorization != null) {
            throw new IllegalStateException("Token Credentials already set.");
        }
        authorization = getAuthorization(clientCredentials, tokenCredentials);
    }
    
    // Authentication ===================================================================
    
    /**
     * First step in the OAuth 1.0 protocol: Temporary Credentials Request. In this
     * request the client credentials are used which must be provided by Dropbox to this
     * application in advance.
     *
     * @return Temporary Credentials
     */
    public Credentials requestTemporaryCredentials() throws IOException {
        String response = request("GET", API_SERVER, "/oauth/request_token")
            .withHeader("Authorization", String.format(
                "OAuth oauth_version=\"1.0\", " +
                "oauth_signature_method=\"PLAINTEXT\", " +
                "oauth_consumer_key=\"%s\", " +
                "oauth_signature=\"%s&\"", 
                clientCredentials.getKey(), 
                clientCredentials.getSecret()))
            .asString(restClient);
        return parseCredentials(response);
    }

    /**
     * Second step in the OAuth 1.0 protocol: Resource Owner Authorization Request.
     * Redirect the Resource Owner (User) to the endpoint returned by this method. If the
     * Resource Owner approved the access to the resource(s), the Token Credentials can be
     * requested by
     * {@link #requestTokenCredentials(eu.infomas.dropbox.Credentials) }.
     *
     * @param temporaryCredentials The Temporary Credentials as returned by
     * {@link #requestTemporaryCredentials() }
     *
     * @return The endpoint URI for the Resource Owner Authorization Request
     */
    public URL getResourceOwnerAuthorizationEndpoint(final Credentials temporaryCredentials)
        throws IOException {

        return request("GET", API_SERVER, "/oauth/authorize")
            .withParameter("oauth_token", temporaryCredentials.getKey())
            .toURL();
    }

    /**
     * Third step in the OAuth 1.0 protocol: Token Credentials Request.
     *
     * @param temporaryCredentials The Temporary Credentials as returned by
     * {@link #requestTemporaryCredentials() }
     *
     * @return The Token Credentials which can be used to access the restricted resources
     * of the Resource Owner
     */
    public Credentials requestTokenCredentials(final Credentials temporaryCredentials)
        throws IOException {

        String response = request("GET", API_SERVER, "/oauth/access_token")
            .withHeader("Authorization", 
                getAuthorization(clientCredentials, temporaryCredentials))
            .asString(restClient);
        // we ignore the user id (uid) because we do not need it and it can be requested
        // by accountInfo()
        return parseCredentials(response);
    }
    
    // Dropbox accounts =================================================================

    /**
     * Retrieves information about the user's account.
     * <br/>
     * This method wraps the 
     * <a href="https://www.dropbox.com/developers/reference/api#account-info">/account/info</a> 
     * service. See the official API Documentation for more information.
     */ 
    public Account accountInfo() 
        throws IOException {

        final String response = request("GET", API_SERVER, "/account/info")
            .withHeader("Authorization", authorization)
            .asString(restClient);
        return Account.valueOf(parseJson(response, Map.class));
    }
    
    // Files and metadata ===============================================================
    
    /**
     * Builder for the {@link #filesGet(java.lang.String)} service.
     */
    public static final class FilesGet extends AbstractBuilder {
        private String rev;
        private int rangeFirst;
        private int rangeLast;
        
        private FilesGet(final Dropbox dropbox, final String path) {
            super(dropbox, path);
        }
        
        /**
         * Specify the revision of the file to retrieve ({@code rev}). 
         * If not specified, the most recent revision is used.
         */
        public FilesGet withRev(final String rev) {
            this.rev = rev;
            return this;
        }
        
        /**
         * Specify the byte range to retrieve.
         * If not specified, the complete file is retrieved.
         * 
         * @param first Index position of first byte to retrieve (zero based, inclusive)
         * @param last Index position of last byte to retrieve (zero based, inclusive)
         */
        public FilesGet withRange(final int first, final int last) {
            this.rangeFirst = first;
            this.rangeLast = last;
            return this;
        }
        
        /**
         * Call the service and write the requested file to the specified {@code File}.
         */
        public FilesGet toFile(final File file) throws IOException {
            return toOutputStream(new FileOutputStream(file));
        }
        
        /**
         * Call the service and write the requested file to the specified output
         * stream.
         */        
        public FilesGet toOutputStream(final OutputStream out) throws IOException {
            Request.Builder builder = dropbox
                .request("GET", CONTENT_SERVER, "/files/sandbox" + path)
                .withHeader("Authorization", dropbox.authorization)
                .withParameter("rev", rev);
            if (rangeLast > 0) {
                builder.withHeader("Range", "bytes=" + rangeFirst + '-' + rangeLast);
            }
            builder.toOutputStream(dropbox.restClient, out);
            return this;
        }
    }
    
    /**
     * Download a file.
     * <br/>
     * This method wraps the 
     * <a href="https://www.dropbox.com/developers/reference/api#files-GET">/files (GET)</a> 
     * service. See the official API Documentation for more information.
     * <br/>
     * Example Usage:
     * <pre>
     * dropbox.filesGet("path")
     *     .withRange(0, 64)
     *     .toFile("local path to file");
     * </pre>
     * 
     * @return A builder object to customize the request
     */
    public FilesGet filesGet(final String path) throws IOException {
        
        return new FilesGet(this, path);
    }
    
    /**
     * Builder for the {@link #filesPut(java.lang.String)} service.
     */
    public static final class FilesPut extends AbstractBuilder {

        private boolean overwrite;
        private String parentRev = "";

        private FilesPut(final Dropbox dropbox, final String path) {
            super(dropbox, path);
        }
                
        /**
         * Specify the revision of the file you're editing (parent_rev).
         * The revision may be empty, but not {@code null}.
         */
        public FilesPut withParentRev(final String parentRev) {
            Utils.notNull("parentRev", parentRev);
            this.parentRev = parentRev;
            return this;
        }
        
        /**
         * Specify that an already existing file can be overwritten (overwrite).
         * Default is that the file is not overwritten, but a new file name is created.
         */
        public FilesPut withOverwrite() {
            this.overwrite = true;
            return this;
        }
        
        /**
         * Call the service and upload the data from the specified input stream.
         * Note that a length must be specified!
         * Maximum file size limit is 150 MB.
         */
        public Entry fromInputStream(final InputStream is, final long length) 
            throws IOException {
            
            String response = dropbox
                .request("PUT", CONTENT_SERVER, 
                "/files_put/sandbox" + (path.startsWith("/") ? path : "/" + path))
                .withHeader("Authorization", dropbox.authorization)
                .withHeader("Content-Length", Long.toString(length))
                .withParameter("overwrite", overwrite)
                .withParameter("parent_rev", parentRev) // maybe empty, not null
                .withPayload(is)
                .asString(dropbox.restClient);
            return Entry.valueOf(parseJson(response, Map.class));
        }
        
        /**
         * Call the service and upload the data from the specified file.
         * Maximum file size limit is 150 MB.
         */
        public Entry fromFile(final File file) throws IOException {
            return fromInputStream(new FileInputStream(file), file.length());
        }

    }
    
    /**
     * Uploads a file using PUT semantics. 
     * <br/>
     * This method wraps the 
     * <a href="https://www.dropbox.com/developers/reference/api#files_put">/files_put</a> 
     * service. See the official API Documentation for more information.
     * <br/>
     * Note that this API does not support the {@code files-POST} service.
     * Maximum file size limit is 150 MB.
     * 
     * @return A builder object to customize the request
     */
    public FilesPut filesPut(final String path) throws IOException {

        return new FilesPut(this, path);
    }
    
    /**
     * Builder for the {@link #metadata(java.lang.String)} service.
     */
    public static final class Metadata extends AbstractBuilder {
        private int fileLimit = 25000; // default value
        private String hash;
        private boolean list;
        private String rev;

        private Metadata(final Dropbox dropbox, final String path) {
            super(dropbox, path);
        }
        
        /**
         * Specify the revision to use ({@code rev}). 
         * Optional.
         */
        public Metadata withRev(final String rev) {
            this.rev = rev;
            return this;
        }
        
        /**
         * Specify the file limit to use ({@code file_limit}). 
         * Optional.
         */
        public Metadata withFileLimit(int fileLimit) {
            this.fileLimit = fileLimit;
            return this;
        }
        
        /**
         * Specify the hash to use ({@code hash}). 
         * Optional.
         */
        public Metadata withHash(String hash) {
            this.hash = hash;
            return this;
        }
        
        /**
         * Specify the children (files directories) must be returned when the queried
         * path is a directory ({@code list}).
         * Optional, default {@code false}.
         */
        public Metadata withList() {
            this.list = true;
            return this;
        }
        
        /**
         * Call the service and return the response as a JSON String.
         * 
         * @see #asEntry() 
         */        
        public String asJson() throws IOException {
            final String response = dropbox
                .request("GET", API_SERVER, "/metadata/sandbox" + path)
                .withHeader("Authorization", dropbox.authorization)
                .withParameter("file_limit", fileLimit)
                .withParameter("hash", hash)
                .withParameter("list", list)
                .withParameter("rev", rev)
                .withParameter("file_limit", fileLimit)
                .asString(dropbox.restClient);
            if (response == null) {
                LOG.log(Level.WARNING, 
                    "Response is null for path ''/metadata/sandbox/{0}''", path);
            }
            return response;
        }
        
        /**
         * Call the service and return the response as an {@link Entry} object.
         * 
         * @see #asJson() 
         */
        public Entry asEntry() throws IOException {
            final String json = asJson();
            final Map<String, Object> map = parseJson(json, Map.class);
            if (map == null) {
                LOG.log(Level.WARNING, "Map is null for path ''{0}'', content=''{1}''", 
                    new Object[] {path, json});
            }
            return Entry.valueOf(map);
        }
    }
    
    /**
     * Retrieves file and folder metadata.
     * <br/>
     * This method wraps the 
     * <a href="https://www.dropbox.com/developers/reference/api#metadata">/metadata</a> 
     * service. See the official API Documentation for more information.
     * 
     * @return A builder object to customize the request
     */
    public Metadata metadata(final String path) throws IOException {
        
        assert path != null && path.startsWith("/");
        
        return new Metadata(this, path);
    }
    
    /**
     * Return a list of "delta entries", which are instructions on how to update your 
     * local state to match the server's state.
     * <br/>
     * This method wraps the 
     * <a href="https://www.dropbox.com/developers/reference/api#delta">/delta</a> 
     * service. See the official API Documentation for more information.
     * 
     * @param cursor On the first call, you should pass in {@code null}.  
     *               On subsequent calls, pass in the {@link DeltaPage#cursor cursor} 
     *               returned by the previous call
     */
    public DeltaPage delta(final String cursor) throws IOException {
        final String response = request("POST", API_SERVER, "/delta")
            .withHeader("Authorization", authorization)
            .withParameter("cursor", cursor)
            .asString(restClient);
        
        final DeltaPage page = new DeltaPage(parseJson(response, Map.class));
        //writeContentToFile(response, new File("./delta." + page.getCursor() + ".json"));
        return page;
    }
    
    /**
     * Obtains metadata for the previous revisions of a file.
     * <br/>
     * This method wraps the 
     * <a href="https://www.dropbox.com/developers/reference/api#revisions">/revisions</a> 
     * service. See the official API Documentation for more information.
     * 
     * @param path On the first call, you should pass in {@code null}.  
     *               On subsequent calls, pass in the {@link DeltaPage#cursor cursor} 
     *               returned by the previous call
     * @param limit rev_limit Default is 10. Max is 1,000. When listing a file, the 
     * service will not report listings containing more than the amount specified and 
     * will instead respond with a 406 (Not Acceptable) status response.
     */
    public List<Entry> revisions(final String path, final int limit) 
        throws IOException {
        
        final String response = request("GET", API_SERVER, "/revisions/sandbox/" + path)
            .withHeader("Authorization", authorization)
            .withParameter("rev_limit", limit)
            .asString(restClient);
        
        final List<Map<String, Object>> jsonList = 
            (List<Map<String, Object>>)parseJson(response, List.class);
        final List<Entry> entries = new ArrayList<Entry>(jsonList.size());
        for (final Map<String, Object> jsonMap : jsonList) {
            final Entry entry = Entry.valueOf(jsonMap);
            if (entry != null) {
                entries.add(entry);
            }
        }
        //writeContentToFile(response, new File("./revisions.json"));
        return entries;
    }   
    
    /**
     * Returns a public link directly to a file.
     * Similar to {@code /shares}. The difference is that this bypasses the Dropbox 
     * webserver, used to provide a preview of the file, so that you can effectively 
     * stream the contents of your media.
     * <br/>
     * Sample of returned message (JSON):
     * <pre>
     * {
     *    "url": "https://dl.dropbox.com/0/view/2j3mng7pmdqinf9/Apps/INFOMAS/digipub/499.038.jpg", 
     *    "expires": "Sat, 29 Sep 2012 19:16:20 +0000"
     * }
     * </pre>
     * <br/>
     * This method wraps the 
     * <a href="https://www.dropbox.com/developers/reference/api#media">/media</a> 
     * service. See the official API Documentation for more information.
     * 
     * @return The public link (URL)
     */
    public String media(final String path) throws IOException {
        final String response = request("POST", API_SERVER, "/media/sandbox/" + path)
            .withHeader("Authorization", authorization)
            .asString(restClient);
        return (String)parseJson(response, Map.class).get("url");
    }

    /**
     * Gets a thumbnail for an image.
     * Default is a JPEG image of 64x64 pixels.
     * <br/>
     * This method currently supports files with the following file extensions: 
     * "jpg", "jpeg", "png", "tiff", "tif", "gif", and "bmp" (case insensitive). 
     * Magic numbers are not used, so use proper file names!
     * Photos that are larger than 20MB in size will not be converted to a thumbnail.
     * <br/>
     * This method wraps the 
     * <a href="https://www.dropbox.com/developers/reference/api#thumbnails">/thumbnails</a> 
     * service. See the official API Documentation for more information.
     */
    public void getThumbnail(final String path, final ThumbSize size, 
        final ThumbFormat format, final OutputStream out) throws IOException {
        
        request("GET", CONTENT_SERVER, "/thumbnails/sandbox" + path)
            .withHeader("Authorization", authorization)
            .withParameter("size", size.toAPISize())
            .withParameter("format", format)
            .toOutputStream(restClient, out);
    }
    
    // File operations ==================================================================
    
    /**
     * Copies a file or folder to a new location.
     * <br/>
     * This method wraps the 
     * <a href="https://www.dropbox.com/developers/reference/api#fileops-copy">/fileops/copy</a> 
     * service. See the official API Documentation for more information.
     * 
     * @return The metadata of the file
     */    
    public Entry copy(final String fromPath, final String toPath) throws IOException {
        return fileops("copy", fromPath, toPath);    
    }
    
    /**
     * Creates a folder.
     * <br/>
     * This method wraps the 
     * <a href="https://www.dropbox.com/developers/reference/api#fileops-create-folder">/fileops/create_folder</a> 
     * service. See the official API Documentation for more information.
     * 
     * @return The metadata of the file
     */     
    public Entry createFolder(final String path) throws IOException {
        return fileops("create_folder", path, null); 
    }
    
    /**
     * Deletes a file or folder.
     * If the file or folder does not exists, an error is returned
     * <pre>
     * 404 Not Found
     * {"error": "Path '/testtest' not found"}
     * </pre>
     * This method wraps the 
     * <a href="https://www.dropbox.com/developers/reference/api#fileops-delete">/fileops/delete</a> 
     * service. See the official API Documentation for more information.
     * 
     * @return The metadata of the file
     */   
    public Entry delete(final String path) throws IOException {
        return fileops("delete", path, null); 
    }
    
    /**
     * Moves a file or folder to a new location.
     * <br/>
     * This method wraps the 
     * <a href="https://www.dropbox.com/developers/reference/api#fileops-move">/fileops/move</a> 
     * service. See the official API Documentation for more information.
     * 
     * @return The metadata of the file
     */ 
    public Entry move(final String fromPath, final String toPath) throws IOException {
        return fileops("move", fromPath, toPath); 
    }
    
    // Implementation / private =========================================================

    /**
     * Base class used by the "request builders".
     */
    private static abstract class AbstractBuilder {
        protected final Dropbox dropbox;
        protected final String path;

        private AbstractBuilder(final Dropbox dropbox, final String path) {
            this.dropbox = dropbox;
            this.path = path;
        }
    }
    
    private static String getAuthorization(final Credentials clientCredentials, 
        final Credentials signingCredentials) {
        // TODO: Do we need encodeRfc5849() here? identifiers and tokens are always 
        // alphanumeric Strings @ Dropbox?
        return String.format(
            "OAuth oauth_version=\"1.0\", " +
            "oauth_signature_method=\"PLAINTEXT\", " +
            "oauth_consumer_key=\"%s\", " +
            "oauth_token=\"%s\", " +
            "oauth_signature=\"%s&%s\"",
            encodeRfc5849(clientCredentials.getKey()), 
            encodeRfc5849(signingCredentials.getKey()),
            encodeRfc5849(clientCredentials.getSecret()), 
            encodeRfc5849(signingCredentials.getSecret()));
    }

    /**
     * Utility method, returning an {@link Request} preconfigured for the Dropbox API.
     */
    private Request.Builder request(final String method, final String host, final String path) {
        return Request.withMethod(method)
            .withHost(host)
            .withPath("/" + API_VERSION + path)
            .withParameter("locale", getLocale().getLanguage());
    }
    
    private Entry fileops(final String action, final String path, final String toPath) 
        throws IOException { 

        String response = request("POST", API_SERVER, "/fileops/" + action)
            .withParameter("root", "sandbox")
            .withParameter(toPath == null ? "path" : "from_path", path)
            .withParameter("to_path", toPath)
            .withHeader("Authorization", authorization)
            .asString(restClient);
        return Entry.valueOf(parseJson(response, Map.class));     
    }
    
    /**
     * All JSON parsing is delegated to this method.
     * Changing JSON parser must be easy :)
     */
    private static <T> T parseJson(final String json, final Class<T> type) {
        return type.cast(JSONValue.parse(json));
    }

}
