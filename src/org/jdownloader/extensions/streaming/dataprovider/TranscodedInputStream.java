package org.jdownloader.extensions.streaming.dataprovider;

import java.io.InputStream;

public abstract class TranscodedInputStream extends InputStream {
    public String toString() {
        return "TranscodedInputStream->";
    }
}
