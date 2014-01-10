package org.jdownloader.plugins.config;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.WeakHashMap;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.JsonKeyValueStorage;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.InterfaceParseException;
import org.appwork.storage.config.annotations.CryptedStorage;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;

public class PluginJsonConfig {
    private static class Cache {
        private Cache() {

        }

        public ConfigInterface     instance;
        public JsonKeyValueStorage storage;

    }

    private static final WeakHashMap<ClassLoader, Cache> CACHE         = new WeakHashMap<ClassLoader, Cache>();
    private static final WeakHashMap<String, Cache>      STORAGE_CACHE = new WeakHashMap<String, Cache>();

    public static <T extends ConfigInterface> T get(Class<T> configInterface) {
        System.out.println(CACHE);
        Cache cache = CACHE.get(configInterface.getClassLoader());
        if (cache != null) { return (T) cache.instance; }

        File storage = Application.getResource("cfg/plugins/" + configInterface.getName());
        storage.getParentFile().mkdirs();
        StorageHandler<T> sh;

        cache = new Cache();
        final Cache storageCache = STORAGE_CACHE.get(configInterface.getName());
        if (storageCache == null) {
            sh = new StorageHandler<T>(storage, configInterface);
        } else {
            sh = new StorageHandler<T>(storage, configInterface) {
                @Override
                protected void dupeCheck(File name, Class<T> configInterface) {

                }

                @Override
                protected JsonKeyValueStorage createPrimitiveStorage(File file, boolean plain, byte[] key) {
                    return storageCache.storage;
                }

                @Override
                protected void validateKeys(CryptedStorage crypted) {
                    if (storageCache.storage != null) {
                        if (!Arrays.equals(storageCache.storage.getKey(), crypted == null ? JSonStorage.KEY : crypted.key())) { throw new InterfaceParseException("Key Mismatch!"); }
                    }
                }
            };
        }
        // disabled object cache
        sh.setObjectCacheEnabled(false);

        cache.storage = sh.getPrimitiveStorage();
        cache.storage.setEnumCacheEnabled(false);
        cache.instance = (T) Proxy.newProxyInstance(configInterface.getClassLoader(), new Class<?>[] { configInterface }, sh);

        STORAGE_CACHE.put(configInterface.getName(), cache);
        CACHE.put(configInterface.getClassLoader(), cache);

        return (T) cache.instance;
    }
}
