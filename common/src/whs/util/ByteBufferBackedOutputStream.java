package whs.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by misson20000 on 11/12/16.
 */
public class ByteBufferBackedOutputStream extends OutputStream {
    private ByteBuffer buf;

    public ByteBufferBackedOutputStream(ByteBuffer buf) {
        this.buf = buf;
    }

    public void write(int b) throws IOException {
        buf.put((byte) b);
    }

    public void write(byte[] bytes, int off, int len) throws IOException {
        buf.put(bytes, off, len);
    }
}