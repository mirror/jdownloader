package org.jdownloader.extensions.streaming;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
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
                return URLEncoder.encode(JSonStorage.serializeToJson(id), "UTF-8");
            } else {
                return URLEncoder.encode(JSonStorage.serializeToJson(id + "/" + path), "UTF-8");

            }

        } catch (UnsupportedEncodingException e) {
            throw new WTFException(e);
        } catch (StorageException e) {

            throw new WTFException(e);
        }
    }

}
