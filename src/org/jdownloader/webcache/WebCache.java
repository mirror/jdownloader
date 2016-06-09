package org.jdownloader.webcache;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.StorageException;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.extmanager.LoggerFactory;

public class WebCache {
    private File         folder;
    private LogInterface logger;

    public WebCache() {
        this("default");
    }

    protected final HashMap<String, MinTimeWeakReference<CachedRequest>> cache;

    public WebCache(String cacheID) {
        folder = Application.getResource("tmp/webcache/" + cacheID);
        folder.mkdirs();
        logger = LoggerFactory.getDefaultLogger();
        cache = new HashMap<String, MinTimeWeakReference<CachedRequest>>();
    }

    public LogInterface getLogger() {
        return logger;
    }

    public void setLogger(LogInterface logger) {
        this.logger = logger;
    }

    public synchronized CachedRequest get(String lookUpID) {
        CachedRequest cachedRequest = null;
        MinTimeWeakReference<CachedRequest> ret = cache.get(lookUpID);
        if (ret != null) {
            cachedRequest = ret.get();
            if (cachedRequest == null || cachedRequest._isExpired()) {
                cache.remove(lookUpID);
            }

        }
        if (cachedRequest == null) {
            cachedRequest = readFormDisk(lookUpID);

        }

        if (cachedRequest != null) {
            putToRAM(cachedRequest);
        }
        return cachedRequest;
    }

    private CachedRequest readFormDisk(String lookUpID) {
        File file = new File(folder, Hash.getMD5(lookUpID) + ".json");

        try {
            if (file.exists()) {

                CachedRequest ret = JSonStorage.restoreFromString(IO.readFileToString(file), CachedRequest.TYPE_REF);

                if (ret == null | ret._isExpired()) {
                    file.delete();
                    return null;
                }
                return ret;
            }
        } catch (Throwable e) {
            logger.log(e);
            file.delete();
        }
        return null;
    }

    private long minLifeTimeForRAMCache = 5 * 60 * 1000l;

    public long getMinLifeTimeForRAMCache() {
        return minLifeTimeForRAMCache;
    }

    public void setMinLifeTimeForRAMCache(long minLifeTimeForRAMCache) {
        this.minLifeTimeForRAMCache = minLifeTimeForRAMCache;
    }

    public synchronized void put(CachedRequest cachedRequest) {
        if (cachedRequest._isExpired()) {
            logger.info("Do not cache request. ALready expired");
            return;
        }
        System.out.println(JSonStorage.serializeToJson(cachedRequest));
        putToRAM(cachedRequest);
        putToDisk(cachedRequest);

    }

    protected void putToRAM(CachedRequest cachedRequest) {
        cache.put(cachedRequest.getKey(), new MinTimeWeakReference<CachedRequest>(cachedRequest, getMinLifeTimeForRAMCache(), cachedRequest.getKey()));
    }

    protected void putToDisk(CachedRequest cachedRequest) {
        File file = new File(folder, Hash.getMD5(cachedRequest.getKey()) + ".json");
        file.delete();
        try {
            IO.writeStringToFile(file, JSonStorage.serializeToJson(cachedRequest), false);
        } catch (StorageException e) {
            logger.log(e);
        } catch (IOException e) {
            logger.log(e);
        }
    }

}
