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

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;

import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.utils.JDUtilities;

/**
 * Von dieser Klasse kann abgeleitet werden wenn die Neue Klasse Properties
 * unterstützen soll. Die SimpleGUI elemente nutzen das um einfache
 * Dialogelemente zu erstellen. Ein Automatisiertes speichern/laden wird dadurch
 * möglich
 * 
 * @author JD-Team
 * 
 */
public class Property implements Serializable {

    private static final long serialVersionUID = -6093927038856757256L;
    /**
     * Nullvalue used to remove a key completly.
     */
    public static final Object NULL = new Object();

    protected transient Logger logger = null;

    private HashMap<String, Object> properties;
    private HashMap<String, Integer> propertiesHashes;

    protected transient boolean changes = false;

    public Property() {
        properties = new HashMap<String, Object>();
        propertiesHashes = new HashMap<String, Integer>();

        logger = JDLogger.getLogger();
    }

    /**
     * Returns the saved object casted to the type of the defaultvalue
     * <code>def</code>. So no more casts are necessary.
     * 
     * @param <E>
     *            type of the saved object
     * @param key
     *            key for the saved object
     * @param def
     *            defaultvalue if no object is saved (is used to determine the
     *            type of the saved object)
     * @return the saved object casted to its correct type
     */
    @SuppressWarnings("unchecked")
    public <E> E getGenericProperty(String key, E def) {
        Object r = getProperty(key, def);
        try {
            E ret = (E) r;
            return ret;
        } catch (Exception e) {
            logger.finer("Could not cast " + r.getClass().getSimpleName() + " to " + e.getClass().getSimpleName() + " for key " + key);
            return def;
        }
    }

    /**
     * Gibt einen Boolean zu key zurück. Es wird versuchtden Wert zu einem
     * passendem Wert umzuformen
     * 
     * @param key
     * @return
     */
    public Boolean getBooleanProperty(String key) {
        return getBooleanProperty(key, false);
    }

    public Boolean getBooleanProperty(String key, boolean def) {
        try {
            Object r = getProperty(key, def);
            if (!(r instanceof Boolean)) {
                r = r + "";
                if (((String) r).equals("false")) {
                    r = false;
                } else {
                    r = ((String) r).length() > 0;
                }
            }
            Boolean ret = (Boolean) r;
            return ret;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gibt einen Doublewert zu key zurück. Es wird versuchtden Wert zu einem
     * passendem Wert umzuformen
     * 
     * @param key
     * @return
     */
    public Double getDoubleProperty(String key) {
        return getDoubleProperty(key, -1.0);
    }

    public Double getDoubleProperty(String key, Double def) {
        try {
            Object r = getProperty(key, def);
            if (r instanceof String) {
                r = Double.parseDouble((String) r);
            }
            Double ret = (Double) r;
            return ret;
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Gibt einen Integerwert zu key zurück. Es wird versucht, den Wert zu einem
     * passendem Integer umzuformen
     * 
     * @param key
     *            Schlüssel des Wertes
     * @return Der Wert
     */
    public int getIntegerProperty(String key) {
        return getIntegerProperty(key, -1);
    }

    public int getIntegerProperty(String key, int def) {
        try {
            Object r = getProperty(key, def);
            if (r instanceof String) {
                r = Integer.parseInt((String) r);
            }
            Integer ret = (Integer) r;
            return ret;
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Gibt die interne Properties HashMap zurück
     */
    public HashMap<String, Object> getProperties() {
        return properties;
    }

    /**
     * Gibt den Wert zu key zurück
     * 
     * @param key
     * @return Value zu key
     */
    public Object getProperty(String key) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }

        return properties.get(key);
    }

    /**
     * Gibt den Wert zu key zurück und falls keiner festgelegt ist def
     * 
     * @param key
     * @param def
     * @return value
     */
    public Object getProperty(String key, Object def) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        if (properties.get(key) == null) {
            setProperty(key, def);
            return def;
        }
        return properties.get(key);
    }

    /**
     * Gibt einen String zu key zurück. Es wird versuchtden Wert zu einem
     * passendem Wert umzuformen
     * 
     * @param key
     * @return
     */
    public String getStringProperty(String key) {
        return getStringProperty(key, null);
    }

    public String getStringProperty(String key, String def) {
        try {
            Object r = getProperty(key, def);
            String ret = (r == null) ? null : r.toString();
            return ret;
        } catch (Exception e) {
            return def;
        }
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * setzt die interne prperties hashMap
     * 
     * @param properties
     */
    public void setProperties(HashMap<String, Object> properties) {
        this.properties = properties;
        propertiesHashes = new HashMap<String, Integer>();
    }

    /**
     * Speichert einen Wert ab.
     * 
     * @param key
     * @param value
     */
    @SuppressWarnings("unchecked")
    public void setProperty(String key, Object value) {

        if (value == NULL) {
            if (properties.containsKey(key)) {
                properties.remove(key);
                propertiesHashes.remove(key);
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, key));
                this.changes = true;

            }
            return;

        }
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        if (propertiesHashes == null) {
            propertiesHashes = new HashMap<String, Integer>();
        }

        Object old = getProperty(key);

        properties.put(key, value);

        Integer oldHash = propertiesHashes.get(key);

        /*
         * check for null to avoid nullpointer due to .toString() method
         */
        propertiesHashes.put(key, (value == null) ? null : value.toString().hashCode());

        if (JDUtilities.getController() == null) return;
        try {
            if (old == null && value != null) {
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, key));
                this.changes = true;
                return;
            } else if (value instanceof Comparable) {
                if (((Comparable<Comparable<?>>) value).compareTo((Comparable<?>) old) != 0) {
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, key));
                    this.changes = true;
                }
                return;
            } else {
                if (!value.equals(old) || oldHash != value.hashCode()) {
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, key));
                    this.changes = true;
                }
                return;
            }
        } catch (Exception e) {
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, key));
            this.changes = true;
        }
    }

    public boolean hasChanges() {
        return changes;
    }

    /**
     * GIbt die Proprties als String zurück
     * 
     * @return PropertyString
     */
    // @Override
    public String toString() {
        if (properties.size() == 0) return "";
        return "Property: " + properties;
    }

}