package org.jdownloader.extensions.streaming.dataprovider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public abstract class AbstractStreamFactory implements StreamFactoryInterface, StreamListener {

    private HashSet<ListenerInputstreamWrapper>   activeStreams = new HashSet<ListenerInputstreamWrapper>();

    private static final ScheduledExecutorService EXECUTER      = Executors.newSingleThreadScheduledExecutor();
    private DelayedRunnable                       closer;

    private LogSource                             logger;

    protected AbstractStreamFactory() {
        logger = LogController.getInstance().getLogger(getClass().getName());
        exceptions = new ArrayList<Throwable>();
        this.closer = new DelayedRunnable(EXECUTER, 5 * 1000l) {
            @Override
            public void delayedrun() {
                synchronized (activeStreams) {

                    if (activeStreams.size() > 0) return;
                    System.out.println("CLOSED " + AbstractStreamFactory.this);

                    close();
                }

            }
        };
    }

    @Override
    public void onStreamTimeout(ListenerInputstreamWrapper timeoutInputStream, InputStream delegate) {
        try {
            timeoutInputStream.close();
        } catch (IOException e) {
            logger.log(e);
        }
    }

    @Override
    public void onStreamClosed(ListenerInputstreamWrapper timeoutInputStream, InputStream delegate) {
        synchronized (activeStreams) {
            activeStreams.remove(timeoutInputStream);
            if (activeStreams.size() == 0) {

                closer.resetAndStart();
            }
        }

    }

    @Override
    public void onStreamException(ListenerInputstreamWrapper timeoutInputStream, InputStream delegate, Throwable e) {
        logger.info("Stream Exception: " + e);
        // TODO... remove active streams
        try {
            timeoutInputStream.close();
        } catch (IOException e1) {
            logger.log(e);
        }
    }

    private List<Throwable> exceptions;

    public List<Throwable> getExceptions() {
        return exceptions;
    }

    public boolean hasException() {
        return exceptions.size() > 0;
    }

    protected void addException(Exception e) {
        logger.log(e);
        exceptions.add(e);
        close();
    }

    @Override
    public final InputStream getInputStream(long start, long end) throws IOException {

        ListenerInputstreamWrapper stream = new ListenerInputstreamWrapper(createInputStream(start, end), -1, this);
        synchronized (activeStreams) {
            activeStreams.add(stream);
        }
        closer.stop();
        return stream;

    }

    abstract protected InputStream createInputStream(long start, long end) throws IOException;
}
