package org.jdownloader.extensions.streaming.dataprovider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class LocalFileProvider implements DataProvider<File> {

    private HashMap<File, InputStream> map;

    public LocalFileProvider() {
        map = new HashMap<File, InputStream>();
    }

    @Override
    public boolean canHandle(File link, DataProvider<?>... dataProviders) {
        return link instanceof File;
    }

    @Override
    public boolean isRangeRequestSupported(File link) {
        return true;
    }

    @Override
    public long getFinalFileSize(File link) {
        return link.length();
    }

    public String toString() {
        return getClass().getSimpleName() + "<< File on Harddisk";
    }

    @Override
    public InputStream getInputStream(File link, long startPosition, long stopPosition) throws IOException {

        InputStream st = map.get(link);

        if (st == null) {
            st = link.toURI().toURL().openStream();

        } else {
            st.reset();
            st.skip(startPosition);
        }
        return st;
    }

    @Override
    public void close() throws IOException {
        for (InputStream is : map.values()) {
            try {
                is.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}
