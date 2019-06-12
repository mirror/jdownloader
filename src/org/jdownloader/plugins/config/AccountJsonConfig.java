package org.jdownloader.plugins.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import jd.plugins.Account;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.storage.StorageException;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.CryptedStorage;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.ListHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.DebugMode;
import org.appwork.utils.IO;
import org.appwork.utils.IO.SYNC;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.logging.LogController;

public class AccountJsonConfig {
    private static final WeakHashMap<ClassLoader, HashMap<String, WeakReference<ConfigInterface>>> CONFIG_CACHE     = new WeakHashMap<ClassLoader, HashMap<String, WeakReference<ConfigInterface>>>();
    private final static boolean                                                                   DEBUG            = false;
    private final static String                                                                    PREFIX_PRIMITIVE = "configInterface.primitive.";
    private final static String                                                                    PREFIX_OBJECT    = "configInterface.object.";

    public synchronized static <T extends AccountConfigInterface> T get(final Account account) {
        if (account.getHoster() == null) {
            throw new WTFException("Hoster not Set");
        }
        if (account.getPlugin() == null) {
            throw new WTFException("Plugin not Set");
        }
        final Class<T> configInterface = (Class<T>) account.getPlugin().getAccountConfigInterface(account);
        if (configInterface == null) {
            throw new WTFException("no ConfigInterface");
        }
        final String CACHEID = account.getHoster() + "/" + account.getId().getID();
        final ClassLoader classloader = configInterface.getClassLoader();
        HashMap<String, WeakReference<ConfigInterface>> classLoaderMap = CONFIG_CACHE.get(classloader);
        if (classLoaderMap == null) {
            classLoaderMap = new HashMap<String, WeakReference<ConfigInterface>>();
            CONFIG_CACHE.put(classloader, classLoaderMap);
        }
        final WeakReference<ConfigInterface> ret = classLoaderMap.get(CACHEID);
        ConfigInterface intf = null;
        if (ret != null && (intf = ret.get()) != null) {
            if (DEBUG) {
                System.out.println("Reuse cached ConfigInterface " + CACHEID);
            }
            return (T) intf;
        } else {
            if (DEBUG) {
                System.out.println("Create new ConfigInterface " + CACHEID);
            }
        }
        final Storage storage = new Storage() {
            @Override
            public void clear() throws StorageException {
                for (final String key : account.getProperties().keySet()) {
                    if (key.startsWith(PREFIX_PRIMITIVE)) {
                        account.removeProperty(key);
                    }
                }
            }

            @Override
            public void close() {
            }

            @Override
            public <E> E get(String key, E def, Boolean autoPutValue) throws StorageException {
                final boolean contains = hasProperty(key);
                final boolean autoPutDefaultValue = autoPutValue == null ? isAutoPutValues() : Boolean.TRUE.equals(autoPutValue);
                Object ret = contains ? account.getProperty(PREFIX_PRIMITIVE + key) : null;
                if (ret != null && def != null && ret.getClass() != def.getClass()) {
                    /* ret class different from def class, so we have to convert */
                    if (def instanceof Long) {
                        if (ret instanceof Number) {
                            ret = ((Number) ret).longValue();
                        } else if (ret instanceof String) {
                            ret = Long.parseLong((String) ret);
                        }
                    } else if (def instanceof Integer) {
                        if (ret instanceof Number) {
                            ret = ((Number) ret).intValue();
                        } else if (ret instanceof String) {
                            ret = Integer.parseInt((String) ret);
                        }
                    } else if (def instanceof Double) {
                        if (ret instanceof Float) {
                            ret = ((Double) ret).doubleValue();
                        }
                    } else if (def instanceof Float) {
                        if (ret instanceof Double) {
                            ret = ((Float) ret).floatValue();
                        }
                    }
                }
                // put entry if we have no entry
                if (!contains) {
                    ret = def;
                    if (autoPutDefaultValue) {
                        if (def instanceof Boolean) {
                            this.put(key, (Boolean) def);
                        } else if (def instanceof Long) {
                            this.put(key, (Long) def);
                        } else if (def instanceof Integer) {
                            this.put(key, (Integer) def);
                        } else if (def instanceof Byte) {
                            this.put(key, (Byte) def);
                        } else if (def instanceof String || def == null) {
                            this.put(key, (String) def);
                        } else if (def instanceof Enum<?>) {
                            this.put(key, (Enum<?>) def);
                        } else if (def instanceof Double) {
                            this.put(key, (Double) def);
                        } else if (def instanceof Float) {
                            this.put(key, (Float) def);
                        } else {
                            throw new StorageException("Invalid datatype: " + (def != null ? def.getClass() : "null"));
                        }
                    }
                }
                if (def instanceof Enum<?> && ret instanceof String) {
                    try {
                        ret = Enum.valueOf(((Enum<?>) def).getDeclaringClass(), (String) ret);
                    } catch (final Throwable e) {
                        if (e instanceof IllegalArgumentException) {
                            LogController.CL().info("Could not restore the enum. There is no value for " + ret + " in " + ((Enum<?>) def).getDeclaringClass());
                        }
                        LogController.CL().log(e);
                        ret = def;
                    }
                }
                return (E) ret;
            }

            @Override
            public <E> E get(String key, E def) throws StorageException {
                return get(key, def, null);
            }

            @Override
            public byte[] getCryptKey() {
                return null;
            }

            @Override
            public String getID() {
                return null;
            }

            @Override
            public boolean hasProperty(String key) {
                if (key != null) {
                    return account.hasProperty(PREFIX_PRIMITIVE + key);
                }
                return false;
            }

            @Override
            public boolean isAutoPutValues() {
                return false;
            }

            @Override
            public void put(String key, Boolean value) throws StorageException {
                putInternal(key, value);
            }

            @Override
            public void put(String key, Byte value) throws StorageException {
                putInternal(key, value);
            }

            @Override
            public void put(String key, Double value) throws StorageException {
                putInternal(key, value);
            }

            @Override
            public void put(String key, Enum<?> value) throws StorageException {
                putInternal(key, value);
            }

            @Override
            public void put(String key, Float value) throws StorageException {
                putInternal(key, value);
            }

            @Override
            public void put(String key, Integer value) throws StorageException {
                putInternal(key, value);
            }

            @Override
            public void put(String key, Long value) throws StorageException {
                putInternal(key, value);
            }

            @Override
            public void put(String key, String value) throws StorageException {
                putInternal(key, value);
            }

            private void putInternal(String key, Object value) {
                if (key != null) {
                    account.setProperty(PREFIX_PRIMITIVE + key, value);
                }
            }

            @Override
            public Object remove(String key) {
                if (key != null) {
                    return account.removeProperty(PREFIX_PRIMITIVE + key);
                } else {
                    return null;
                }
            }

            @Override
            public void save() throws StorageException {
            }

            @Override
            public void setAutoPutValues(boolean b) {
            }

            @Override
            public int size() {
                int size = 0;
                for (final String key : account.getProperties().keySet()) {
                    if (key.startsWith(PREFIX_PRIMITIVE)) {
                        size++;
                    }
                }
                return size;
            }

            @Override
            public SYNC getStorageSyncMode() {
                return SYNC.META_AND_DATA;
            }

            @Override
            public void setStorageSyncMode(SYNC storageSyncMode) {
            }
        };
        final StorageHandler<T> storageHandler = new StorageHandler<T>(configInterface) {
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
            }

            @Override
            public Storage getPrimitiveStorage() {
                return storage;
            }

            @Override
            protected boolean isDelayedWriteAllowed(KeyHandler<?> keyHandler) {
                return false;
            }

            @Override
            protected void addStorageHandler(StorageHandler<? extends ConfigInterface> storageHandler, String interfaceName, String storage) {
            }

            @Override
            public void write() {
            }

            @Override
            protected void writeObject(final ListHandler<?> keyHandler, final Object object) {
                final String key = PREFIX_OBJECT + keyHandler.getKey();
                final String jsonString = JSonStorage.serializeToJson(object);
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    final GZIPOutputStream gzip = new GZIPOutputStream(bos);
                    gzip.write(jsonString.getBytes("UTF-8"));
                    gzip.close();
                    final String compressedJSonString = Base64.encodeToString(bos.toByteArray(), false);
                    account.setProperty(key, compressedJSonString);
                } catch (Exception e) {
                    LogController.CL().log(e);
                }
            }

            @Override
            protected Object readObject(final ListHandler<?> keyHandler, final AtomicBoolean readFlag) {
                Object readObject = null;
                try {
                    final String key = PREFIX_OBJECT + keyHandler.getKey();
                    final String compressedJSonString = account.getStringProperty(key);
                    if (compressedJSonString != null) {
                        final byte[] bytes = Base64.decode(compressedJSonString);
                        final String jsonString = IO.readInputStreamToString(new GZIPInputStream(new ByteArrayInputStream(bytes)));
                        readFlag.set(true);
                        readObject = JSonStorage.restoreFromString(jsonString, ((ListHandler<?>) keyHandler).getTypeRef(), null);
                    }
                } catch (Exception e) {
                    LogController.CL().log(e);
                }
                return readObject;
            }

            @Override
            protected void validateKeys(CryptedStorage crypted) {
            }
        };
        intf = (T) Proxy.newProxyInstance(configInterface.getClassLoader(), new Class<?>[] { configInterface }, storageHandler);
        classLoaderMap.put(CACHEID, new WeakReference<ConfigInterface>(intf));
        return (T) intf;
    }
}
