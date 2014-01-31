package org.jdownloader.plugins.config;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
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
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public class PluginJsonConfig {

    private static final WeakHashMap<ClassLoader, HashMap<String, WeakReference<ConfigInterface>>> CONFIG_CACHE  = new WeakHashMap<ClassLoader, HashMap<String, WeakReference<ConfigInterface>>>();
    private static final HashMap<String, JsonKeyValueStorage>                                      STORAGE_CACHE = new HashMap<String, JsonKeyValueStorage>();
    protected static final DelayedRunnable                                                         SAVEDELAYER   = new DelayedRunnable(5000, 30000) {

                                                                                                                     @Override
                                                                                                                     public void delayedrun() {
                                                                                                                         saveAll();
                                                                                                                         cleanup();
                                                                                                                     }
                                                                                                                 };
    private final static boolean                                                                   DEBUG         = true;

    static {
        File pluginsFolder = Application.getResource("cfg/plugins/");
        if (!pluginsFolder.exists()) pluginsFolder.mkdirs();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public int getHookPriority() {
                return 0;
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                saveAll();
            }

            @Override
            public String toString() {
                return "ShutdownEvent: SaveAllPluginJsonConfig";
            }
        });
    }

    private synchronized static void saveAll() {
        HashMap<String, JsonKeyValueStorage> storages = STORAGE_CACHE;
        Iterator<Entry<String, JsonKeyValueStorage>> it = storages.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().save();
        }
    }

    private synchronized static void cleanup() {
        CONFIG_CACHE.size();
    }

    public synchronized static <T extends ConfigInterface> T get(Class<T> configInterface) {
        final String ID = configInterface.getName();
        final ClassLoader cl = configInterface.getClassLoader();
        if (!(cl instanceof PluginClassLoaderChild)) throw new WTFException(configInterface + " got loaded by non PluginClassLoaderChild!");
        HashMap<String, WeakReference<ConfigInterface>> classLoaderMap = CONFIG_CACHE.get(cl);
        if (classLoaderMap == null) {
            classLoaderMap = new HashMap<String, WeakReference<ConfigInterface>>();
            CONFIG_CACHE.put(cl, classLoaderMap);
        }
        WeakReference<ConfigInterface> ret = classLoaderMap.get(ID);
        ConfigInterface intf = null;
        if (ret != null && (intf = ret.get()) != null) {
            if (DEBUG) System.out.println("Reuse cached ConfigInterface " + ID);
            return (T) intf;
        } else {
            if (DEBUG) System.out.println("Create new ConfigInterface " + ID);
        }

        JsonKeyValueStorage storage = STORAGE_CACHE.get(configInterface.getName());
        if (storage == null) {
            final File storageFile = Application.getResource("cfg/plugins/" + ID);
            if (DEBUG) System.out.println("Create PluginJsonConfig for " + ID);
            storage = StorageHandler.createPrimitiveStorage(storageFile, null, configInterface, SAVEDELAYER);
            storage.setEnumCacheEnabled(false);
            STORAGE_CACHE.put(ID, storage);
        }
        StorageHandler<T> storageHandler = new StorageHandler<T>(storage, configInterface) {

            @Override
            protected void addStorageHandler(StorageHandler<? extends ConfigInterface> storageHandler, String interfaceName, String storage) {
            }

            @Override
            protected void validateKeys(CryptedStorage crypted) {
                if (getPrimitiveStorage() != null) {
                    if (!Arrays.equals(getPrimitiveStorage().getCryptKey(), crypted == null ? JSonStorage.KEY : crypted.key())) { throw new InterfaceParseException("Key Mismatch!"); }
                }
            }
        };
        intf = (T) Proxy.newProxyInstance(configInterface.getClassLoader(), new Class<?>[] { configInterface }, storageHandler);
        classLoaderMap.put(ID, new WeakReference<ConfigInterface>(intf));
        return (T) intf;
    }
}
