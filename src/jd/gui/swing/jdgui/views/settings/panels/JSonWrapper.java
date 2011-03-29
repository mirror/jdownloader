package jd.gui.swing.jdgui.views.settings.panels;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.gui.swing.laf.LookAndFeelWrapper;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.JsonKeyValueStorage;
import org.appwork.storage.Storage;
import org.appwork.storage.StorageEvent;
import org.appwork.storage.StorageKeyAddedEvent;
import org.appwork.storage.StorageKeyRemovedEvent;
import org.appwork.storage.StorageValueChangeEvent;
import org.appwork.storage.TypeRef;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.logging.Log;
import org.appwork.utils.reflection.Clazz;

/**
 * A Wrapperclass that wrapps a (Old) JD- Property class around the new appwork
 * utils jsonstorage
 * 
 * @author thomas
 * 
 */
public class JSonWrapper extends Property implements DefaultEventListener<StorageEvent<?>> {
    /**
     * 
     */
    private static final long                    serialVersionUID = 1L;
    private static HashMap<Storage, JSonWrapper> MAP              = new HashMap<Storage, JSonWrapper>();
    private Storage                              storage;
    private JDController                         jdController;
    private String                               path;
    private boolean                              plain;

    private JSonWrapper(Storage json) {
        this.storage = json;
        jdController = JDController.getInstance();
        storage.getEventSender().addListener(this);
        String str = ((JsonKeyValueStorage) storage).getFile().getAbsolutePath();
        path = str.substring(0, str.length() - (str.endsWith(".json") ? 5 : 4));
        plain = ((JsonKeyValueStorage) storage).isPlain();

    }

    public static JSonWrapper get(String string) {
        synchronized (MAP) {

            Storage json = JSonStorage.getPlainStorage(string);

            JSonWrapper ret = MAP.get(json);
            if (ret == null) {
                ret = new JSonWrapper(json);
                convert(string, ret);

                MAP.put(json, ret);
            }
            return ret;
        }
    }

    /**
     * converts from old subconfig to new JSOnstorage
     * 
     * @param string
     * @param ret
     */
    private static void convert(String string, JSonWrapper ret) {

        SubConfiguration subConfig = SubConfiguration.getConfig(string);
        HashMap<String, Object> props = subConfig.getProperties();
        if (props.size() > 0) {
            Entry<String, Object> next;
            for (Iterator<Entry<String, Object>> it = props.entrySet().iterator(); it.hasNext();) {
                next = it.next();
                try {
                    ret.setProperty(next.getKey(), next.getValue());
                } catch (Throwable e) {
                    Log.exception(e);
                }
                it.remove();
            }

            subConfig.setProperties(props);
            subConfig.save();
            ret.save();
        }

    }

    public <E> E getGenericProperty(final String key, final E def) {
        if (def == null || Clazz.isPrimitive(def.getClass())) {
            return storage.get(key, def);
        } else {
            System.out.println("Read " + path + "." + key + (plain ? ".json" : ".ejs"));
            return JSonStorage.restoreFrom(new File(path + "." + key + (plain ? ".json" : ".ejs")), def);
        }
    }

    public Boolean getBooleanProperty(final String key) {
        return storage.get(key, false);
    }

    public Boolean getBooleanProperty(final String key, final boolean def) {
        return storage.get(key, def);
    }

    public Double getDoubleProperty(final String key) {
        return storage.get(key, -1.0d);
    }

    public Double getDoubleProperty(final String key, final Double def) {
        return storage.get(key, def);
    }

    public int getIntegerProperty(final String key) {
        return storage.get(key, -1);
    }

    public int getIntegerProperty(final String key, final int def) {
        return storage.get(key, def);
    }

    public HashMap<String, Object> getProperties() {
        throw new RuntimeException("no Implemented");
    }

    public Object getProperty(final String key) {
        try {
            if (storage.hasProperty(key)) {
                return this.storage.get(key, (String) null);
            } else if (getObjectKey(key).exists()) { return getProperty(key, null); }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object getProperty(final String key, final Object def) {
        try {
            if (storage.hasProperty(key)) {
                return this.storage.get(key, (String) null);
            } else if (getObjectKey(key).exists()) {

                if (def != null) {
                    return JSonStorage.restoreFrom(getObjectKey(key), def);
                } else {
                    TypeRef<?> ref = getType(key);
                    System.out.println("Read " + path + "." + key + (plain ? ".json" : ".ejs"));
                    if (ref == null) {
                        Log.exception(new Exception(path + ".json." + key + " type missing"));
                    }
                    return JSonStorage.restoreFrom(getObjectKey(key), true, null, ref, null);
                }

            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return def;

    }

    private static final HashMap<String, TypeRef<?>> MAPPING = new HashMap<String, TypeRef<?>>();
    static {
        // convertermappings
        MAPPING.put("jdgui.json.PLAF5", new TypeRef<LookAndFeelWrapper>() {
        });

        MAPPING.put("folderwatch.json.FOLDER_LIST", new TypeRef<Vector<String>>() {
        });

    }

    private TypeRef<?> getType(String key) {
        return MAPPING.get(((JsonKeyValueStorage) storage).getFile().getName() + "." + key);
    }

    public String getStringProperty(final String key) {
        return storage.get(key, (String) null);
    }

    public String getStringProperty(final String key, final String def) {
        return storage.get(key, def);
    }

    public boolean hasProperty(final String key) {
        if (storage.hasProperty(key)) {
            return true;
        } else {
            return getObjectKey(key).exists();
        }
    }

    public void setProperties(final HashMap<String, Object> properties) {
        throw new RuntimeException("no Implemented");
    }

    /**
     * Stores a value.
     * 
     * @param key
     * @param value
     */
    public void setProperty(final String key, final Object value) {
        if (value == null) {
            storage.remove(key);
        } else {
            if (value instanceof Boolean) {
                storage.put(key, (Boolean) value);
            } else if (value instanceof Long) {
                storage.put(key, (Long) value);
            } else if (value instanceof Integer) {
                storage.put(key, (Integer) value);
            } else if (value instanceof Byte) {
                storage.put(key, (Byte) value);
            } else if (value instanceof String) {
                storage.put(key, (String) value);
            } else if (value instanceof Double) {
                storage.put(key, (Double) value);
            } else {
                System.out.println("Write " + path + "." + key + (plain ? ".json" : ".ejs"));
                JSonStorage.saveTo(getObjectKey(key), value);
            }

            // throw new RuntimeException("Type " + value.getClass() +
            // " not supported");
            // }
        }
    }

    private File getObjectKey(String key) {
        return new File(path + "." + key + (plain ? ".json" : ".ejs"));
    }

    public boolean hasChanges() {
        return changes;
    }

    @Override
    public String toString() {
        return storage.toString();
    }

    public void onEvent(StorageEvent<?> event) {
        // delegate events
        if (event instanceof StorageKeyAddedEvent) {
            jdController.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, event.getKey()));
        } else if (event instanceof StorageValueChangeEvent) {
            if (((StorageValueChangeEvent<?>) event).hasChanged()) {
                changes = true;
                jdController.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, event.getKey()));
            }
        } else if (event instanceof StorageKeyRemovedEvent) {
            changes = true;
            jdController.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, event.getKey()));

        }
    }

    public void save() {
        storage.save();
    }

}
