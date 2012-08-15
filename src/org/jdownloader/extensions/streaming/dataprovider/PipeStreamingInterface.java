package org.jdownloader.extensions.streaming.dataprovider;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import jd.plugins.DownloadLink;

import org.appwork.scheduler.DelayedRunnable;
import org.jdownloader.extensions.streaming.StreamingInterface;

public class PipeStreamingInterface implements StreamingInterface {
    private static final ScheduledExecutorService EXECUTER = Executors.newSingleThreadScheduledExecutor();
    private DataProvider                          dataProvider;
    private DownloadLink                          link;
    private DelayedRunnable                       closer;
    private AtomicInteger                         counter  = new AtomicInteger();

    public PipeStreamingInterface(DownloadLink dlink, DataProvider rarProvider) {
        this.dataProvider = rarProvider;
        this.link = dlink;
        counter.set(0);
        // Close all streams if there is no read for 5 seconds
        this.closer = new DelayedRunnable(EXECUTER, 2 * 1000l) {
            @Override
            public void delayedrun() {
                if (counter.get() > 0) return;
                System.out.println("EXEC");
                try {
                    close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public boolean isRangeRequestSupported() {
        return dataProvider.isRangeRequestSupported(link);
    }

    @Override
    public long getFinalFileSize() {
        return dataProvider.getFinalFileSize(link);
    }

    @Override
    public InputStream getInputStream(long startPosition, long stopPosition) throws IOException {

        final InputStream str = dataProvider.getInputStream(link, startPosition, stopPosition);

        counter.addAndGet(1);
        closer.stop();
        return new InputStream() {

            @Override
            public int read() throws IOException {

                return str.read();

            }

            @Override
            public int read(byte[] b) throws IOException {

                return str.read(b);

            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {

                return str.read(b, off, len);

            }

            @Override
            public long skip(long n) throws IOException {
                return str.skip(n);
            }

            @Override
            public int available() throws IOException {
                return str.available();
            }

            @Override
            public void close() throws IOException {
                try {
                    str.close();

                } finally {
                    if (counter.decrementAndGet() <= 0) {

                        closer.resetAndStart();
                    }
                }
            }

            @Override
            public synchronized void mark(int readlimit) {
                str.mark(readlimit);
            }

            @Override
            public synchronized void reset() throws IOException {
                str.reset();
            }

            @Override
            public boolean markSupported() {
                return str.markSupported();
            }

        };
    }

    @Override
    public void close() throws IOException {
        dataProvider.close();
    }

}
