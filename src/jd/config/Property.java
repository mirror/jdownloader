//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.config;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;

/**
 * Von dieser Klasse kann abgeleitet werden wenn die Neue Klasse Properties unterstützen soll. Die SimpleGUI elemente nutzen das um einfache
 * Dialogelemente zu erstellen. Ein Automatisiertes speichern/laden wird dadurch möglich
 *
 * @author JD-Team
 *
 */
public class Property implements Serializable {
    private static final long       serialVersionUID = -6093927038856757256L;
    /**
     * Nullvalue used to remove a key completly.
     */
    public static final Object      NULL             = new Object();
    /* do not remove to keep stable compatibility */
    private HashMap<String, Object> properties       = null;

    public Property() {
    }

    public boolean removeProperty(String key) {
        return setProperty(key, Property.NULL);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        /* deserialize object and set all transient variables */
        stream.defaultReadObject();
        setProperties(properties);
    }

    /**
     * Gibt einen Boolean zu key zurück. Es wird versuchtden Wert zu einem passendem Wert umzuformen
     *
     * @param key
     * @return
     */
    public Boolean getBooleanProperty(final String key) {
        return getBooleanProperty(key, false);
    }

    public <T> T getObjectProperty(String key, TypeRef<T> typeRef) {
        Object ret = getProperty(key);
        if (ret == null) {
            return null;
        }
        if (typeRef.getType().equals(ret.getClass())) {
            return (T) ret;
        }
        // if (ret instanceof String) {
        // ret = JSonStorage.restoreFromString((String) ret, typeRef);
        // } else {
        ret = JSonStorage.convert(ret, typeRef);
        // }
        setProperty(key, ret);
        return (T) ret;
    }

    public Boolean getBooleanProperty(final String key, final boolean def) {
        try {
            final Object value = getProperty(key, def);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else {
                final String stringValue = String.valueOf(value);
                if (stringValue != null) {
                    if ("false".equalsIgnoreCase(stringValue)) {
                        return false;
                    } else {
                        return stringValue.length() > 0;
                    }
                }
            }
        } catch (final Exception e) {
        }
        return def;
    }

    /**
     * Gibt einen Integerwert zu key zurück. Es wird versucht, den Wert zu einem passendem Integer umzuformen
     *
     * @param key
     *            Schlüssel des Wertes
     * @return Der Wert
     */
    public int getIntegerProperty(final String key) {
        return getIntegerProperty(key, -1);
    }

    public int getIntegerProperty(final String key, final int def) {
        try {
            final Object value = getProperty(key, def);
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (final Exception e) {
        }
        return def;
    }

    /**
     * DO not use in plugins for old 09581 Stable or try/catch
     *
     * @since JD2
     */
    public long getLongProperty(final String key, final long def) {
        try {
            final Object value = getProperty(key, def);
            if (value instanceof String) {
                return Long.parseLong((String) value);
            } else if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        } catch (final Exception e) {
        }
        return def;
    }

    /**
     * returns a copy of the internal Map
     *
     * @return
     */
    public Map<String, Object> getProperties() {
        final HashMap<String, Object> lInternal = properties;
        if (lInternal == null || lInternal.size() == 0) {
            return new HashMap<String, Object>();
        } else {
            synchronized (lInternal) {
                return new HashMap<String, Object>(lInternal);
            }
        }
    }

    /**
     * Returns the value for key
     *
     * @param key
     * @return Value for key
     */
    public Object getProperty(final String key) {
        if (key == null) {
            throw new WTFException("key ==null is forbidden!");
        }
        final HashMap<String, Object> lInternal = properties;
        if (lInternal == null || lInternal.size() == 0) {
            return null;
        } else {
            synchronized (lInternal) {
                return lInternal.get(key);
            }
        }
    }

    /**
     * Returns the value for key, and if none is set def
     *
     * @param key
     * @param def
     * @return value
     */
    public Object getProperty(final String key, final Object def) {
        Object ret = getProperty(key);
        if (def instanceof Long && ret instanceof Integer) {
            /* fix for integer in property map, but long wanted */
            ret = ((Integer) ret).longValue();
        }
        if (ret == null) {
            return def;
        }
        return ret;
    }

    /**
     * Gibt einen String zu key zurück. Es wird versuchtden Wert zu einem passendem Wert umzuformen
     *
     * @param key
     * @return
     */
    public String getStringProperty(final String key) {
        return getStringProperty(key, null);
    }

    public String getStringProperty(final String key, final String def) {
        try {
            final Object r = getProperty(key, def);
            final String ret = (r == null) ? null : r.toString();
            return ret;
        } catch (final Exception e) {
            return def;
        }
    }

    public boolean hasProperty(final String key) {
        if (key == null) {
            throw new WTFException("key ==null is forbidden!");
        } else {
            final HashMap<String, Object> lInternal = properties;
            if (lInternal == null || lInternal.size() == 0) {
                return false;
            } else {
                synchronized (lInternal) {
                    return lInternal.containsKey(key);
                }
            }
        }
    }

    // convert LinkedHashMap to HashMap, reduces memory
    private final HashMap<String, Object> optimizeHashMap(final HashMap<String, Object> map) {
        if (map != null) {
            if (map instanceof LinkedHashMap) {
                final HashMap<String, Object> ret = new HashMap<String, Object>();
                final Iterator<Entry<String, Object>> it = map.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, Object> next = it.next();
                    if (next.getKey() == null || next.getValue() == null) {
                        continue;
                    } else {
                        if (next.getValue() instanceof HashMap) {
                            ret.put(next.getKey(), optimizeHashMap((HashMap<String, Object>) next.getValue()));
                        } else {
                            ret.put(next.getKey(), next.getValue());
                        }
                    }
                }
                return ret;
            }
        }
        return map;
    }

    public void setProperties(final Map<String, Object> properties) {
        final HashMap<String, Object> newProperties = new HashMap<String, Object>();
        if (properties != null && properties.size() > 0) {
            final Iterator<Entry<String, Object>> it = properties.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> next = it.next();
                if (next.getKey() == null || next.getValue() == null) {
                    //
                    continue;
                }
                final String deDupKey = deDuplicateString(next.getKey());
                newProperties.put(deDupKey, next.getValue());
            }
        }
        if (newProperties.size() > 0) {
            this.properties = newProperties;
        } else {
            this.properties = null;
        }
    }

    public void setPropertiesUnsafe(final HashMap<String, Object> properties) {
        if (properties != null && properties instanceof LinkedHashMap) {
            this.properties = optimizeHashMap(properties);
        } else {
            this.properties = properties;
        }
    }

    private static String deDuplicateString(String string) {
        if (string != null && Application.getJavaVersion() >= Application.JAVA17) {
            return string.intern();
        } else {
            return string;
        }
    }

    /**
     * Stores a value. Warning: DO not store other stuff than primitives/lists/maps!!
     *
     * @param key
     * @param value
     */
    public boolean setProperty(final String key, final Object value) {
        if (key == null) {
            throw new WTFException("key ==null is forbidden!");
        }
        final HashMap<String, Object> lInternal = properties;
        if (lInternal == null) {
            if (value == null || value == NULL) {
                return false;
            } else {
                properties = new HashMap<String, Object>();
                return setProperty(key, value);
            }
        } else {
            synchronized (lInternal) {
                if (value == NULL || value == null) {
                    return lInternal.remove(key) != null;
                }
                final String deDupKey = deDuplicateString(key);
                final Object old = lInternal.put(deDupKey, value);
                if (old == null && value != null) {
                    return true;
                }
                return !old.equals(value);
            }
        }
    }

    /**
     * GIbt die Proprties als String zurück
     *
     * @return PropertyString
     */
    // @Override
    @Override
    public String toString() {
        final HashMap<String, Object> lInternal = properties;
        if (lInternal == null || lInternal.size() == 0) {
            return "Property: empty";
        } else {
            synchronized (lInternal) {
                return "Property: " + lInternal;
            }
        }
    }
}