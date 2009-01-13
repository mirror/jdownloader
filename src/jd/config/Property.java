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

    protected transient Logger logger = JDUtilities.getLogger();

    private HashMap<String, Object> properties = new HashMap<String, Object>();

    private long saveCount = 0;

    /**
     * 
     */
    public Property() {

    }

    public Property(Object obj) {
        this();
        setProperty(null, obj);
    }

    public Property(String value, Object obj) {
        this();
        setProperty(value, obj);
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
        Boolean ret;
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
            ret = (Boolean) r;
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
        Double ret;
        try {
            Object r = getProperty(key, def);
            if (r instanceof String) {
                r = Double.parseDouble((String) r);
            }
            ret = (Double) r;
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
        int ret;
        try {
            Object r = getProperty(key, def);
            if (r instanceof String) {
                r = Integer.parseInt((String) r);
            }
            ret = (Integer) r;
            return ret;
        } catch (Exception e) {
            return def;
        }

    }

    /**
     * @return gibt die INterne properties hashmap zurück
     */
    public HashMap<String, Object> getProperties() {
        return properties;
    }

    /**
     * Gibt dne Wert zu key zurück
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
     * Gibtd en wert zu key zurück und falls keiner festgelegt ist def
     * 
     * @param key
     * @param def
     * @return value
     */
    public Object getProperty(String key, Object def) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        if (getProperty(key) == null) {
            setProperty(key, def);
            return def;
        }
        return properties.get(key);
    }

    /**
     * Gibt zurück wie oft in dieser propertyinstanz schon Werte geändert wurden
     * 
     * @return zahl der Änderungen
     */
    public long getSaveCount() {
        return saveCount;
    }
    
    /**
     * Gibt die Anzahl der gespeicherten Einträge zurück
     * 
     * @return Zahl der Elemente
     */
    public long getCount() {
    	return properties.size();
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
        String ret;
        try {
            Object r = getProperty(key, def);
            if (!(r instanceof String) && r != null) {
                r = r + "";
            }
            ret = (String) r;
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
        saveCount++;
        this.properties = properties;
    }

    /**
     * Speichert einen Wert ab.
     * 
     * @param key
     * @param value
     */
    @SuppressWarnings("unchecked")
    public void setProperty(String key, Object value) {
        // if(key==Configuration.PARAM_USE_GLOBAL_PREMIUM&&this==JDUtilities.getConfiguration()){
        // logger.info("II");
        // }
        saveCount++;
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        Object old = getProperty(key);

        properties.put(key, value);

        if (logger == null) {
            logger = JDUtilities.getLogger();
        }
        if (JDUtilities.getController() == null) { return; }
        try {
            if (old == null && value != null) {
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, key));

            } else if (value instanceof Comparable) {
                if (((Comparable<Comparable<?>>) value).compareTo((Comparable<?>) old) != 0) {
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, key));
                }
            } else if (value instanceof Object) {
                if (!value.equals(old)) {
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, key));

                }
            } else {
                if (value != old) {
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, key));
                }

            }
        } catch (Exception e) {
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_JDPROPERTY_CHANGED, key));
        }

        // logger.finer("Config property: " + key + " = " + value+" - "+this);

    }

    /**
     * GIbt die Proprties als String zurück
     * 
     * @return PropertyString
     */
    @Override
    public String toString() {
        return "Property(" + saveCount + "): " + properties;
    }

}