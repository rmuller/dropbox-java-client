package eu.infomas.dropbox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.*;

public final class ChunkedInputStreamTest {

    @Test
    public void testByte() throws IOException {
        final InputStream in = new ByteArrayInputStream(new byte[]{1,2,3});
        final ChunkedInputStream chunked = new ChunkedInputStream(in, 2);

        assertTrue(chunked.nextChunk());
        assertEquals(1, chunked.read());
        assertEquals(2, chunked.read());
        assertEquals(-1, chunked.read());
        assertEquals(-1, chunked.read()); // can be called more than once
        assertTrue(chunked.nextChunk());
        assertEquals(3, chunked.read());
        assertEquals(-1, chunked.read());
        assertFalse(chunked.nextChunk());
        assertFalse(chunked.nextChunk()); // can be called more than once
    }

    @Test
    public void testReadyArray() throws IOException {
        final InputStream in = new ByteArrayInputStream(new byte[]{1,2,3,4,5,6,7,8,9});
        final ChunkedInputStream chunked = new ChunkedInputStream(in, 4);
        final byte[] buffer = new byte[2];
        
        assertTrue(chunked.nextChunk());
        assertEquals(2, chunked.read(buffer));
        assertEquals(2, chunked.read(buffer));
        assertEquals(-1, chunked.read(buffer));
        assertEquals(-1, chunked.read(buffer)); // can be called more than once
        assertTrue(chunked.nextChunk());
        assertEquals(2, chunked.read(buffer));
        assertEquals(2, chunked.read(buffer));
        assertEquals(-1, chunked.read(buffer));
        assertTrue(chunked.nextChunk());
        assertEquals(1, chunked.read(buffer));
        assertEquals(-1, chunked.read(buffer));
        assertFalse(chunked.nextChunk());
        assertFalse(chunked.nextChunk()); // can be called more than once
    }

}