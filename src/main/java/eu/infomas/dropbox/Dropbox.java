/* DropBox.java
 *
 * Created: 2012-10-01 (Year-Month-Day)
 * Character encoding: UTF-8
 *
 ****************************************** LICENSE *******************************************
 *
 * Copyright (c) 2012 - 2013 XIAM Solutions B.V. (http://www.xiam.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.infomas.dropbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static eu.infomas.dropbox.Utils.asLong;
import static eu.infomas.dropbox.Utils.asString;
import static eu.infomas.dropbox.Utils.encodeRfc5849;
import static eu.infomas.dropbox.Utils.loadProperties;
import static eu.infomas.dropbox.Utils.parseCredentials;
import static eu.infomas.dropbox.Utils.parseJson;

/**
 * {@code DropBox} offers an easy-to-use "fluent" interface for working with the
 * <a href="https://www.dropbox.com/developers/reference/api">Dropbox REST API v1</a>.
 * All operations (REST Requests) are done via this facade API.
 * <p>
 * Dropbox does use <a href="http://tools.ietf.org/html/rfc5849">the OAuth 1.0
 * Protocol</a> which is also supported by this class.
 * <p>
 * The actual HTTP communication is delegated to a {@link RestClient} implementation, which
 * can easily use a different implementation than the default provided by using the
 * Java {@link java.util.ServiceLoader service-provider loading facility}.
 * <p>
 * <b>NOTE</b> that this class is <b>not</b> thread-safe!
 * {@code Dropbox} instances are cheap to create, so do not cache these instances,
 * just create a new instance when needed, use it and throw it away!
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

    private static final String HEADER_AUTHORIZATION = "Authorization";

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
        final String response = request("GET", API_SERVER, "/oauth/request_token")
            .withHeader(HEADER_AUTHORIZATION, String.format(
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

        final String response = request("GET", API_SERVER, "/oauth/access_token")
            .withHeader(HEADER_AUTHORIZATION,
                getAuthorization(clientCredentials, temporaryCredentials))
            .asString(restClient);
        // we ignore the user id (uid) because we do not need it and it can be requested
        // by accountInfo()
        return parseCredentials(response);
    }

    // Dropbox accounts =================================================================

    /**
     * Retrieves information about the user's account.
     * <p>
     * This method wraps the <a
     * href="https://www.dropbox.com/developers/reference/api#account-info">
     * /account/info</a> service.
     * See the official API Documentation for more information.
     */
    public Account accountInfo()
        throws IOException {

        final String response = request("GET", API_SERVER, "/account/info")
            .withHeader(HEADER_AUTHORIZATION, authorization)
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
         *
         * @return The {@code FilesGet} Builder
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
         *
         * @return The {@code FilesGet} Builder
         */
        public FilesGet withRange(final int first, final int last) {
            if (first < 0) {
                throw new IllegalArgumentException("'first' < 0: " + first);
            }
            if (first >= last) {
                throw new IllegalArgumentException(
                    "'first' >= 'last': " + first + " >= " + last);
            }
            this.rangeFirst = first;
            this.rangeLast = last;
            return this;
        }

        /**
         * Call the service and write the requested file to the specified {@code File}.
         *
         * @return The bytes written to the file (file size)
         */
        public long toFile(final File file) throws IOException {
            return toOutputStream(new FileOutputStream(file));
        }

        /**
         * Call the service and write the requested file to the specified output
         * stream.
         *
         * @return The bytes written to the output stream
         */
        public long toOutputStream(final OutputStream out) throws IOException {
            final Request.Builder builder = dropbox
                .request("GET", CONTENT_SERVER, "/files/sandbox" + path)
                .withHeader(HEADER_AUTHORIZATION, dropbox.authorization)
                .withParameter("rev", rev);
            if (rangeLast > 0) {
                builder.withHeader("Range", "bytes=" + rangeFirst + '-' + rangeLast);
            }
            return builder.toOutputStream(dropbox.restClient, out);
        }
    }

    /**
     * Download a file.
     * <p>
     * This method wraps the
     * <a href="https://www.dropbox.com/developers/reference/api#files-GET">/files (GET)</a>
     * service. See the official API Documentation for more information.
     * <p>
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
    public static class FilesPut extends AbstractBuilder {

        private boolean overwrite;
        private String parentRev = "";

        FilesPut(final Dropbox dropbox, final String path) {
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
         * Used by subclasses.
         */
        String getParentRev() {
            return parentRev;
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
         * Used by subclasses.
         */
        boolean isOverwrite() {
            return overwrite;
        }

        /**
         * Call the service and upload the data from the specified input stream.
         * Note that a length must be specified!
         * Maximum file size limit is 150 MB.
         */
        public Entry fromInputStream(final InputStream is, final long length)
            throws IOException {

            final String response = dropbox
                .request("PUT", CONTENT_SERVER,
                "/files_put/sandbox" + (path.startsWith("/") ? path : "/" + path))
                .withHeader(HEADER_AUTHORIZATION, dropbox.authorization)
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
     * <p>
     * This method wraps the
     * <a href="https://www.dropbox.com/developers/reference/api#files_put">/files_put</a>
     * service. See the official API Documentation for more information.
     * <p>
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
         * Default value is 25000.
         */
        public Metadata withFileLimit(final int fileLimit) {
            this.fileLimit = fileLimit;
            return this;
        }

        /**
         * Specify the hash to use ({@code hash}).
         * Optional.
         */
        public Metadata withHash(final String hash) {
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
                .withHeader(HEADER_AUTHORIZATION, dropbox.authorization)
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
     * <p>
     * This method wraps the
     * <a href="https://www.dropbox.com/developers/reference/api#metadata">/metadata</a>
     * service. See the official API Documentation for more information.
     *
     * @return A builder object to customize the request
     */
    public Metadata metadata(final String path) throws IOException {

        return new Metadata(this, path);
    }

    /**
     * Return a list of "delta entries", which are instructions on how to update your
     * local state to match the server's state.
     * <p>
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
            .withHeader(HEADER_AUTHORIZATION, authorization)
            .withParameter("cursor", cursor)
            .asString(restClient);

        final DeltaPage page = new DeltaPage(parseJson(response, Map.class));
        //writeContentToFile(response, new File("./delta." + page.getCursor() + ".json"));
        return page;
    }

    /**
     * Obtains metadata for the previous revisions of a file.
     * <p>
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
            .withHeader(HEADER_AUTHORIZATION, authorization)
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
     * <p>
     * Sample of returned message (JSON):
     * <pre>
     * {
     *    "url": "https://dl.dropbox.com/0/view/2j3mng7pmdqinf9/Apps/INFOMAS/digipub.jpg",
     *    "expires": "Sat, 29 Sep 2012 19:16:20 +0000"
     * }
     * </pre>
     * <p>
     * This method wraps the
     * <a href="https://www.dropbox.com/developers/reference/api#media">/media</a>
     * service. See the official API Documentation for more information.
     *
     * @return The public link (URL)
     */
    public String media(final String path) throws IOException {
        final String response = request("POST", API_SERVER, "/media/sandbox/" + path)
            .withHeader(HEADER_AUTHORIZATION, authorization)
            .asString(restClient);
        return (String)parseJson(response, Map.class).get("url");
    }

    /**
     * Gets a thumbnail for an image.
     * Default is a JPEG image of 64x64 pixels.
     * <p>
     * This method currently supports files with the following file extensions:
     * "jpg", "jpeg", "png", "tiff", "tif", "gif", and "bmp" (case insensitive).
     * Magic numbers are not used, so use proper file names!
     * Photos that are larger than 20MB in size will not be converted to a thumbnail.
     * <p>
     * This method wraps the
     * <a href="https://www.dropbox.com/developers/reference/api#thumbnails">/thumbnails</a>
     * service. See the official API Documentation for more information.
     */
    public void getThumbnail(final String path, final ThumbSize size,
        final ThumbFormat format, final OutputStream out) throws IOException {

        request("GET", CONTENT_SERVER, "/thumbnails/sandbox" + path)
            .withHeader(HEADER_AUTHORIZATION, authorization)
            .withParameter("size", size.toAPISize())
            .withParameter("format", format)
            .toOutputStream(restClient, out);
    }

    /**
     * Builder for the {@link #chunkedUpload(java.lang.String)} service.
     */
    // http://stackoverflow.com/questions/5346726/java-inheritance-using-builder-pattern
    public static final class ChunkedFilesPut extends FilesPut {

        private int chunkSize;

        private ChunkedFilesPut(final Dropbox dropbox, final String path) {
            super(dropbox, path);
            withChunkSize(4);
        }

        /**
         * Specify the chuck size in MB.
         * If not specified, 4 MB is used as default.
         * The maximum chunk size is 150 MB.
         */
        public ChunkedFilesPut withChunkSize(final int chunkSize) {
            if (chunkSize <= 0 || chunkSize > 150) {
                throw new IllegalArgumentException("Invalid chunk size: " + chunkSize);
            }
            this.chunkSize = chunkSize * 1024 * 1024;
            return this;
        }

        /**
         * Call the service and upload the data from the specified input stream.
         * Note that a length must be specified!
         * Maximum file size limit is 150 MB.
         */
        @Override
        @SuppressWarnings("unchecked")
        public Entry fromInputStream(final InputStream is, final long length)
            throws IOException {

            // https://www.dropbox.com/developers/core/docs#chunked-upload
            final ChunkedInputStream chunked = new ChunkedInputStream(is, chunkSize);
            String uploadId = null; // at first PUT, uploadId should not be included
            long offset = 0;
            while (chunked.nextChunk()) {
                // response holds JSON String with "upload_id", "offset" and "expires"
                final String response = dropbox
                    .request("PUT", CONTENT_SERVER, "/chunked_upload")
                    .withHeader(HEADER_AUTHORIZATION, dropbox.authorization)
                    .withParameter("upload_id", uploadId)
                    .withParameter("offset", offset)
                    .withPayload(chunked)
                    .asString(dropbox.restClient);
                final Map<String, Object> json = parseJson(response, Map.class);
                uploadId = asString(json, "upload_id");
                offset = asLong(json, "offset");
            }
            // ChunkedInputStream handles close() itself
            // https://www.dropbox.com/developers/core/docs#commit-chunked-upload
            final String response = dropbox.request("POST", CONTENT_SERVER,
                "/commit_chunked_upload/sandbox" + (path.startsWith("/") ? path : "/" + path))
                .withHeader(HEADER_AUTHORIZATION, dropbox.authorization)
                .withParameter("upload_id", uploadId)
                .withParameter("parent_rev", getParentRev())
                .withParameter("overwrite", isOverwrite())
                .asString(dropbox.restClient);
            return Entry.valueOf(parseJson(response, Map.class));
        }

    }
    /**
     * Uploads large files to Dropbox in multiple chunks.
     * Also has the ability to resume if the upload is interrupted. This allows for
     * uploads larger than the {@code /files_put} maximum of 150 MB.
     * <p>
     * This method wraps the <a
     * href="https://www.dropbox.com/developers/reference/api#chunked-upload">
     * /chunked_upload</a> service.
     * See the official API Documentation for more information.
     */
    public ChunkedFilesPut chunkedUpload(final String path) throws IOException {

        return new ChunkedFilesPut(this, path);
    }

    // File operations ==================================================================

    /**
     * Copies a file or folder to a new location.
     * <p>
     * This method wraps the <a
     * href="https://www.dropbox.com/developers/reference/api#fileops-copy">
     * /fileops/copy</a> service.
     * See the official API Documentation for more information.
     *
     * @return The metadata of the file
     */
    public Entry copy(final String fromPath, final String toPath) throws IOException {
        return fileops("copy", fromPath, toPath);
    }

    /**
     * Creates a folder.
     * <p>
     * This method wraps the <a
     * href="https://www.dropbox.com/developers/reference/api#fileops-create-folder">
     * /fileops/create_folder</a> service.
     * See the official API Documentation for more information.
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
     * This method wraps the <a
     * href="https://www.dropbox.com/developers/reference/api#fileops-delete">
     * /fileops/delete</a> service.
     * See the official API Documentation for more information.
     *
     * @return The metadata of the file
     */
    public Entry delete(final String path) throws IOException {
        return fileops("delete", path, null);
    }

    /**
     * Moves a file or folder to a new location.
     * <p>
     * This method wraps the <a
     * href="https://www.dropbox.com/developers/reference/api#fileops-move">
     * /fileops/move</a> service.
     * See the official API Documentation for more information.
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
    private abstract static class AbstractBuilder {
        protected final Dropbox dropbox;
        protected final String path;

        private AbstractBuilder(final Dropbox dropbox, final String path) {
            assert dropbox != null;
            if (!path.isEmpty() && !path.startsWith("/")) {
                throw new IllegalArgumentException("'path' must be absolute: " + path);
            }
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
    private Request.Builder request(final String method, final String host,
        final String path) {

        return Request.withMethod(method)
            .withHost(host)
            .withPath("/" + API_VERSION + path)
            .withParameter("locale", getLocale().getLanguage());
    }

    private Entry fileops(final String action, final String path, final String toPath)
        throws IOException {

        final String response = request("POST", API_SERVER, "/fileops/" + action)
            .withParameter("root", "sandbox")
            .withParameter(toPath == null ? "path" : "from_path", path)
            .withParameter("to_path", toPath)
            .withHeader(HEADER_AUTHORIZATION, authorization)
            .asString(restClient);
        return Entry.valueOf(parseJson(response, Map.class));
    }

}
