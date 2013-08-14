package org.jdownloader.jdserv;

import org.appwork.remotecall.RemoteCallInterface;
import org.jdownloader.remotecall.MultiForm;

public interface UploadInterface extends RemoteCallInterface {
    @MultiForm
    public String upload(byte[] data, String name, String id);

    @Deprecated
    public LogCollection get(String id, String name);

    public String setKey(String key);

    public LogCollection getLogByID(String id);
}
