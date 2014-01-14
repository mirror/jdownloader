package org.jdownloader.plugins.config;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
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

    private static final WeakHashMap<ClassLoader, HashMap<String, Cache>> CACHE         = new WeakHashMap<ClassLoader, HashMap<String, Cache>>();
    private static final WeakHashMap<String, Cache>                       STORAGE_CACHE = new WeakHashMap<String, Cache>();

    public static <T extends ConfigInterface> T get(Class<T> configInterface) {
        System.out.println(CACHE);
        ClassLoader cl = configInterface.getClassLoader();
        synchronized (cl) {

            HashMap<String, Cache> cacheMap = CACHE.get(cl);
            Cache cache;
            if (cacheMap != null) {
                cache = cacheMap.get(configInterface.getName());
                if (cache != null) return (T) cache.instance;
            }

            File storage = Application.getResource("cfg/plugins/" + configInterface.getName());
            storage.getParentFile().mkdirs();
            StorageHandler<T> sh;

            cache = new Cache();
            final Cache storageCache = STORAGE_CACHE.get(configInterface.getName());
            if (storageCache == null) {
                sh = new StorageHandler<T>(storage, configInterface) {
                    @Override
                    protected void dupeCheck(File name, Class<T> configInterface) {

                    }

                    @Override
                    protected void initShutdownHook(Class<T> configInterface) {

                    }

                    @Override
                    protected JsonKeyValueStorage createPrimitiveStorage(File file, boolean plain, byte[] key) {
                        final JsonKeyValueStorage ret = super.createPrimitiveStorage(file, plain, key);
                        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

                            @Override
                            public int getHookPriority() {
                                return 0;
                            }

                            @Override
                            public void onShutdown(final ShutdownRequest shutdownRequest) {

                                ret.save();

                            }

                            @Override
                            public String toString() {
                                return "Save " + ret;
                            }
                        });
                        return ret;
                    }

                };

            } else {
                sh = new StorageHandler<T>(storage, configInterface) {
                    @Override
                    protected void dupeCheck(File name, Class<T> configInterface) {

                    }

                    @Override
                    protected void initShutdownHook(Class<T> configInterface) {

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
            if (cacheMap == null) {
                cacheMap = new HashMap<String, PluginJsonConfig.Cache>();
                CACHE.put(cl, cacheMap);
            }
            cacheMap.put(configInterface.getName(), cache);
            STORAGE_CACHE.put(configInterface.getName(), cache);
            return (T) cache.instance;
        }

    }
}
