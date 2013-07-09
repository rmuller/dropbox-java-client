/* ChunkedInputStream.java
 *
 * Created: 2013-07-08 (Year-Month-Day)
 * Character encoding: UTF-8
 *
 ****************************************** LICENSE *******************************************
 *
 * Copyright (c) 2013 XIAM Solutions B.V. (http://www.xiam.nl)
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@code ChunkedInputStream}.
 * <p>
 * Usage:
 * <pre>
 * final ChunkedInputStream chunked = new ChunkedInputStream((in), chunkSize);
 * while (chunked.nextChunk()) {
 *     // read and process chunked input stream as a normal input stream, any close()
 *     // operation is ignored
 * }
 * // do not need to close the chunked input stream (closed by instance itself)
 * </pre>
 * 
 * @author <a href="mailto:rmuller@xiam.nl">Ronald K. Muller</a>
 * @since infomas-asl 3.0.2
 */
final class ChunkedInputStream extends FilterInputStream {

    private final int chunkSize;
    private int remaining;
    private boolean closed;
    
    /**
     * Create a new {@code ChunkedInputStream} instance.
     * 
     * @param in The underlying input stream
     * @param chunkSize The chunk size in bytes
     */
    ChunkedInputStream(final InputStream in, final int chunkSize) {
        super(in);
        this.chunkSize = chunkSize;
    }

    /**
     * Return {@code true} if the next chunk of data can be read. If {@code false}
     * the underlying input stream is read fully and automatically closed by this
     * {@code ChunkedInputStream}.
     */
    public boolean nextChunk() throws IOException {
        if (closed) {
            return false;
        } else {
            remaining = chunkSize;
            return true;
        }
    }
    
    @Override
    public int read() throws IOException {
        if (remaining == 0) {
            return -1;
        }
        final int c = super.read();
        if (c < 0) {
            closeSource();
            return -1;
        } else {
            --remaining;
            return c;
        }
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int length) 
        throws IOException {
        
        if (remaining == 0) {
            return -1;
        }
        final int max = Math.min(remaining, length);
        final int actual = super.read(buffer, offset, max);
        if (actual < 0) {
            closeSource();
        } else {
            remaining -= actual;
        }
        return actual;
    }

    @Override
    public void close() throws IOException {
        // do NOT close underlying stream! This is done by closeSource()
    }

    // private
    
    private void closeSource() throws IOException {
        try {
            in.close();
        } finally {
            closed = true;
        }
    }
    
}
