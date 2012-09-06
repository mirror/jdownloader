package org.jdownloader.extensions.streaming.dataprovider;

import java.io.InputStream;

import org.appwork.utils.net.meteredconnection.MeteredInputStream;
import org.appwork.utils.speedmeter.AverageSpeedMeter;

public class PrintSpeedInputStream extends MeteredInputStream implements StreamListener {

    private Thread thread = null;

    public PrintSpeedInputStream(ListenerInputstreamWrapper inputStream, final String name) {
        super(inputStream, new AverageSpeedMeter(10));
        inputStream.setListener(this);
        thread = new Thread("PrintSpeedInputStream " + name) {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    System.out.println(name + " " + (getSpeedMeter() / 1024) + " kb/s");
                }
            }
        };

        thread.start();
    }

    @Override
    public void onStreamTimeout(ListenerInputstreamWrapper timeoutInputStream, InputStream delegate) {
        thread.interrupt();
    }

    @Override
    public void onStreamClosed(ListenerInputstreamWrapper timeoutInputStream, InputStream delegate) {
        thread.interrupt();
    }

    @Override
    public void onStreamException(ListenerInputstreamWrapper timeoutInputStream, InputStream delegate, Throwable e) {
        thread.interrupt();
    }

}
