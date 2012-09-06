package org.jdownloader.extensions.streaming.dataprovider;

import java.io.InputStream;

public interface StreamListener {

    void onStreamTimeout(ListenerInputstreamWrapper timeoutInputStream, InputStream delegate);

    void onStreamClosed(ListenerInputstreamWrapper timeoutInputStream, InputStream delegate);

    void onStreamException(ListenerInputstreamWrapper timeoutInputStream, InputStream delegate, Throwable e);

}
