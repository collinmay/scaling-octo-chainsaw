package whs.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * Created by misson20000 on 9/24/16.
 */
public class ChannelPair {
    private final BidirectionalPipeChannel a;
    private final BidirectionalPipeChannel b;
    private boolean isClosed = false;

    public ChannelPair() throws IOException {
        Pipe aToB = Pipe.open();
        Pipe bToA = Pipe.open();

        a = new BidirectionalPipeChannel(bToA.source(), aToB.sink());
        b = new BidirectionalPipeChannel(aToB.source(), bToA.sink());
    }

    public void close() throws IOException {
        a.close();
        b.close();
    }

    public BidirectionalPipeChannel getChannelA() {
        return a;
    }
    public BidirectionalPipeChannel getChannelB() {
        return b;
    }

    public class BidirectionalPipeChannel extends AbstractSelectableChannel implements ByteChannel {
        private final Pipe.SourceChannel in;
        private final Pipe.SinkChannel out;

        private BidirectionalPipeChannel(Pipe.SourceChannel in, Pipe.SinkChannel out) {
            super(SelectorProvider.provider());
            this.in = in;
            this.out = out;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            return in.read(dst);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            return out.write(src);
        }

        @Override
        protected void implCloseSelectableChannel() throws IOException {
            in.close();
            out.close();
            isClosed = true;
        }

        @Override
        protected void implConfigureBlocking(boolean block) throws IOException {
            in.configureBlocking(block);
            out.configureBlocking(block);
        }

        @Override
        public int validOps() {
            return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        }
    }
}
