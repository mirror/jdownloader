package org.jdownloader.extensions.streaming;

import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.StorageException;
import org.appwork.utils.StringUtils;

public class IDFactory {

    public static String create(DownloadLink link, String path) {
        return create(link.getUniqueID().toString(), path);
    }

    // "&uid="+getUniqueDeviceID()
    public static String create(String id, String path) {
        try {
            if (StringUtils.isEmpty(path)) {
                return id;
            } else {
                return id + "/" + path;

            }

        } catch (StorageException e) {

            throw new WTFException(e);
        }
    }

}
