package org.jdownloader.plugins.config;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import jd.config.SubConfiguration;
import jd.plugins.DecrypterPlugin;
import jd.plugins.HostPlugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.JsonKeyValueStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.InterfaceParseException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.CryptedStorage;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
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
    private final static boolean                                                                   DEBUG         = false;
    static {
        File pluginsFolder = Application.getResource("cfg/plugins/");
        if (!pluginsFolder.exists()) {
            pluginsFolder.mkdirs();
        }
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public long getMaxDuration() {
                return 0;
            }

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

    public synchronized static <T extends PluginConfigInterface> T get(Class<T> configInterface) {
        String host = null;
        Type type = null;
        PluginHost hostAnnotation = configInterface.getAnnotation(PluginHost.class);
        if (hostAnnotation != null) {
            host = hostAnnotation.host();
            type = hostAnnotation.type();
        } else {
            Class<?> enc = configInterface.getEnclosingClass();
            if (enc != null) {
                if (PluginForHost.class.isAssignableFrom(enc)) {
                    type = Type.HOSTER;
                    HostPlugin anno = enc.getAnnotation(HostPlugin.class);
                    String[] names = anno.names();
                    if (names.length == 1) {
                        host = names[0];
                    } else {
                        throw new WTFException("Bad Config Interface Definition. " + enc + " defines " + names.length + " Hosts. you have to define an own config interface class for each one and use the @PluginHost(\"domain.de\") Annotation");
                    }
                } else if (PluginForDecrypt.class.isAssignableFrom(enc)) {
                    type = Type.CRAWLER;
                    DecrypterPlugin anno = enc.getAnnotation(DecrypterPlugin.class);
                    String[] names = anno.names();
                    if (names.length == 1) {
                        if (StringUtils.isEmpty(host)) {
                            host = names[0];
                        }
                    } else {
                        throw new WTFException("Bad Config Interface Definition. " + enc + " defines " + names.length + " Hosts. you have to define an own config interface class for each one and use the @PluginHost(\"domain.de\") Annotation");
                    }
                }
            } else {
                throw new WTFException("Bad Config Interface Definition. " + configInterface.getName() + ". @PluginHost(\"domain.de\") or    public Class<? extends UsenetConfigInterface> getConfigInterface() {... is missing");
            }
        }
        String ID = JsonConfig.getStorageName(configInterface);
        if (ID.equals(configInterface.getName())) {
            ID = host;
            ID = type + "/" + ID;
        }
        final ClassLoader cl = configInterface.getClassLoader();
        if (!(cl instanceof PluginClassLoaderChild)) {
            final File storageFile = Application.getResource("cfg/plugins/" + ID);
            return JsonConfig.create(storageFile, configInterface);
        }
        HashMap<String, WeakReference<ConfigInterface>> classLoaderMap = CONFIG_CACHE.get(cl);
        if (classLoaderMap == null) {
            classLoaderMap = new HashMap<String, WeakReference<ConfigInterface>>();
            CONFIG_CACHE.put(cl, classLoaderMap);
        }
        final WeakReference<ConfigInterface> ret = classLoaderMap.get(ID);
        ConfigInterface intf = null;
        if (ret != null && (intf = ret.get()) != null) {
            if (DEBUG) {
                System.out.println("Reuse cached ConfigInterface " + ID);
            }
            return (T) intf;
        } else {
            if (DEBUG) {
                System.out.println("Create new ConfigInterface " + ID);
            }
        }
        JsonKeyValueStorage storage = STORAGE_CACHE.get(ID);
        if (storage == null) {
            final File storageFile = Application.getResource("cfg/plugins/" + ID);
            if (DEBUG) {
                System.out.println("Create PluginJsonConfig for " + ID);
            }
            storage = StorageHandler.createPrimitiveStorage(storageFile, null, configInterface);
            storage.setEnumCacheEnabled(false);
            STORAGE_CACHE.put(ID, storage);
        }
        final StorageHandler<T> storageHandler = new StorageHandler<T>(storage, configInterface) {
            @Override
            protected void error(final Throwable e) {
                if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    new Thread("ERROR THROWER") {
                        @Override
                        public void run() {
                            Dialog.getInstance().showExceptionDialog(e.getClass().getSimpleName(), e.getMessage(), e);
                        }
                    }.start();
                }
            }

            @Override
            protected void requestSave() {
                PluginJsonConfig.SAVEDELAYER.resetAndStart();
            }

            @Override
            protected void addStorageHandler(StorageHandler<? extends ConfigInterface> storageHandler, String interfaceName, String storage) {
            }

            @Override
            protected void validateKeys(CryptedStorage crypted) {
                if (getPrimitiveStorage() != null) {
                    if (!Arrays.equals(getPrimitiveStorage().getCryptKey(), crypted == null ? JSonStorage.KEY : crypted.key())) {
                        throw new InterfaceParseException("Key Mismatch!");
                    }
                }
            }
        };
        intf = (T) Proxy.newProxyInstance(configInterface.getClassLoader(), new Class<?>[] { configInterface }, storageHandler);
        classLoaderMap.put(ID, new WeakReference<ConfigInterface>(intf));
        SubConfiguration oldSub = SubConfiguration.getConfig(host, true);
        if (oldSub != null) {
            for (KeyHandler handler : storageHandler.getKeyHandler()) {
                TakeValueFromSubconfig takeFrom = (TakeValueFromSubconfig) handler.getAnnotation(TakeValueFromSubconfig.class);
                if (takeFrom != null) {
                    if (oldSub.hasProperty(takeFrom.value())) {
                        Object value = oldSub.getProperty(takeFrom.value());
                        value = JSonStorage.convert(value, new TypeRef<Object>(handler.getRawType()) {
                        });
                        handler.setValue(value);
                        oldSub.removeProperty(takeFrom.value());
                    }
                }
            }
        }
        return (T) intf;
    }
}
