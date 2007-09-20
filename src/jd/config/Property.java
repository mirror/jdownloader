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
    private static final long serialVersionUID = -6093927038856757256L;

    private HashMap<String, Object> properties = new HashMap<String, Object>();
    private long saveCount=0;
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

        logger.finer("Config property: " + key + " = " + value);

    }

  /**
   * GIbt die Proprties als String zurück
   * @return PropertyString
   */
    public String toString() {
        return "Property("+saveCount+"): " + properties;
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
     * @return gibt die INterne properties hashmap zurück
     */
    public HashMap<String, Object> getProperties() {
        return properties;
    }

    /**
     * setzt die interne prperties hashMap
     * @param properties
     */
    public void setProperties(HashMap<String, Object> properties) {
        saveCount++;
        this.properties = properties;
    }
    /**
     * Gibt zurück wie oft in dieser propertyinstanz schon Werte geändert wurden
     * @return zahl der Änderungen
     */
    public long getSaveCount(){
        return saveCount;
    }
   

}