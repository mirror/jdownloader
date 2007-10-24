package jd.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;

import jd.plugins.Plugin;

/**
 * Von dieser Klasse kann abgeleitet werden wenn die Neue Klasse Properties
 * unterstützen soll. Die SimpleGUI elemente nutzen das um einfache
 * Dialogelemente zu erstellen. Ein Automatisiertes speichern/laden wird dadurch
 * möglich
 * 
 * @author coalado
 * 
 */
public class Property implements Serializable {

    /**
     * 
     */
    private static final long       serialVersionUID = -6093927038856757256L;

    private HashMap<String, Object> properties       = new HashMap<String, Object>();

    private long                    saveCount        = 0;

    private transient Logger        logger;

    /**
     * 
     */
    public Property() {
        logger = Plugin.getLogger();
    }

    /**
     * Speichert einen Wert ab.
     * 
     * @param key
     * @param value
     */
    public void setProperty(String key, Object value) {
        saveCount++;
        if (properties == null) properties = new HashMap<String, Object>();
        properties.put(key, value);
        if (logger == null) logger = Plugin.getLogger();

        logger.finer("Config property: " + key + " = " + value+" - "+this);

    }

    /**
     * GIbt die Proprties als String zurück
     * 
     * @return PropertyString
     */
    public String toString() {
        return "Property(" + saveCount + "): " + properties;
    }

    /**
     * Gibt dne Wert zu key zurück
     * 
     * @param key
     * @return Value zu key
     */
    public Object getProperty(String key) {
        if (properties == null) properties = new HashMap<String, Object>();
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
        if (properties == null) properties = new HashMap<String, Object>();
        if (getProperty(key) == null) {
            setProperty(key, def);
            return def;
        }
        return properties.get(key);
    }

    /**
     * Gibt einen Integerwert zu key zurück. Es wird versucht, den Wert zu einem
     * passendem Integer umzuformen
     * 
     * @param key Schlüssel des Wertes
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
        }
        catch (Exception e) {
            return def;
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
        }
        catch (Exception e) {
            return def;
        }

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
        }
        catch (Exception e) {
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
        Boolean ret;
        try {
            Object r = getProperty(key, def);
            if (!(r instanceof Boolean)) {
                r = r + "";
                if (((String) r).equals("false")) {
                    r = false;
                }
                else {
                    r = ((String) r).length() > 0;
                }
            }
            ret = (Boolean) r;
            return ret;
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * @return gibt die INterne properties hashmap zurück
     */
    public HashMap<String, Object> getProperties() {
        return properties;
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
     * Gibt zurück wie oft in dieser propertyinstanz schon Werte geändert wurden
     * 
     * @return zahl der Änderungen
     */
    public long getSaveCount() {
        return saveCount;
    }

}