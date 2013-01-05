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

import org.appwork.exceptions.WTFException;

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

    private HashMap<String, Object> properties       = null;

    public Property() {
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        /* deserialize object and then fill other stuff(transient..) */
        stream.defaultReadObject();
        if (properties != null && properties.isEmpty()) {
            properties = null;
        }
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

    public Boolean getBooleanProperty(final String key, final boolean def) {
        try {
            Object r = getProperty(key, def);
            if (!(r instanceof Boolean)) {
                r = r + "";
                if ("false".equalsIgnoreCase(((String) r))) {
                    r = false;
                } else {
                    r = ((String) r).length() > 0;
                }
            }
            final Boolean ret = (Boolean) r;
            return ret;
        } catch (final Exception e) {
            return false;
        }
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
            Object r = getProperty(key, def);
            if (r instanceof String) {
                r = Integer.parseInt((String) r);
            }
            final Integer ret = (Integer) r;
            return ret;
        } catch (final Exception e) {
            return def;
        }
    }

    /**
     * DO not use in plugins for old 09581 Stable or try/catch
     * 
     * @since JD2
     * */
    @Deprecated
    public long getLongProperty(final String key, final long def) {
        try {
            Object r = getProperty(key, def);
            if (r instanceof String) {
                r = Long.parseLong((String) r);
            } else if (r instanceof Integer) {
                r = ((Integer) r).longValue();
            }
            final Long ret = (Long) r;
            return ret;
        } catch (final Exception e) {
            return def;
        }
    }

    /**
     * Returns the internal HashMap Properties
     */
    public HashMap<String, Object> getProperties() {
        return properties;
    }

    public void copyTo(Property dest) {
        if (dest != null && dest != this) {
            if (properties != null) {
                if (dest.properties == null) {
                    dest.properties = new HashMap<String, Object>();
                }
                dest.properties.putAll(this.properties);
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
        if (properties == null) return null;
        return properties.get(key);
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
        if (ret == null) { return def; }
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
        if (properties == null) return false;
        return properties.containsKey(key);
    }

    /**
     * setzt die interne prperties hashMap
     * 
     * @param properties
     */
    public void setProperties(final HashMap<String, Object> properties) {
        if (properties != null && properties.isEmpty()) {
            this.properties = null;
        } else {
            this.properties = properties;
        }
    }

    /**
     * Stores a value. Warning: DO not store other stuff than primitives/lists/maps!!
     * 
     * @param key
     * @param value
     */
    public void setProperty(final String key, final Object value) {
        if (key == null) { throw new WTFException("key ==null is forbidden!"); }
        if (value == NULL) {
            if (properties != null) {
                properties.remove(key);
            }
            return;
        }
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        final Object old = getProperty(key);
        if (old == null && value == null) {
            /* old and new values are null , so nothing changed */
            return;
        }
        properties.put(key, value);
    }

    /**
     * GIbt die Proprties als String zurück
     * 
     * @return PropertyString
     */
    // @Override
    @Override
    public String toString() {
        if (properties != null) return (properties.size() == 0) ? "" : "Property: " + properties;
        return "no properties set";
    }

}