package org.jdownloader.extensions.streaming.dataprovider;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.appwork.scheduler.DelayedRunnable;

public class ListenerInputstreamWrapper extends InputStream {
    private static final ScheduledExecutorService EXECUTER = Executors.newSingleThreadScheduledExecutor();
    private InputStream                           delegate;
    private DelayedRunnable                       closer;
    private StreamListener                        listener;

    public ListenerInputstreamWrapper(InputStream inputStream, long timeout, StreamListener downloadStreamFactory) {
        this.delegate = inputStream;
        listener = downloadStreamFactory;
        if (timeout > 0) {
            this.closer = new DelayedRunnable(EXECUTER, timeout) {
                @Override
                public void delayedrun() {
                    listener.onStreamTimeout(ListenerInputstreamWrapper.this, delegate);
                }
            };

        }
    }

    @Override
    public int read() throws IOException {
        try {
            return delegate.read();
        } catch (IOException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (RuntimeException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (Error e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } finally {
            if (closer != null) closer.resetAndStart();
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        try {
            return delegate.read(b);
        } catch (IOException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (RuntimeException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (Error e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } finally {
            if (closer != null) closer.resetAndStart();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return delegate.read(b, off, len);
        } catch (IOException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (RuntimeException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (Error e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } finally {
            if (closer != null) closer.resetAndStart();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        try {
            return delegate.skip(n);
        } catch (IOException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (RuntimeException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (Error e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } finally {
            if (closer != null) closer.resetAndStart();
        }
    }

    @Override
    public int available() throws IOException {
        try {
            return delegate.available();
        } catch (IOException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (RuntimeException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (Error e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } finally {
            if (closer != null) closer.resetAndStart();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } catch (IOException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (RuntimeException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (Error e) {
            listener.onStreamException(this, delegate, e);
            throw e;

        } finally {
            if (closer != null) closer.stop();
            listener.onStreamClosed(this, delegate);
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        try {
            delegate.reset();
        } catch (IOException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (RuntimeException e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } catch (Error e) {
            listener.onStreamException(this, delegate, e);
            throw e;
        } finally {
            if (closer != null) closer.resetAndStart();
        }
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

}
