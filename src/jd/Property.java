package jd;

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
        if (properties == null) properties = new HashMap<String, Object>();
        properties.put(key, value);
        if (logger == null) logger = Plugin.getLogger();

        logger.finer("Config property: " + key + " = " + value);

    }

    public String toString() {
        return "Property: " + properties;
    }

    /**
     * Gibt dne Wert zu key zurück
     * 
     * @param key
     * @return
     */
    public Object getProperty(String key) {
        if (properties == null) properties = new HashMap<String, Object>();
        return properties.get(key);
    }

    public HashMap<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(HashMap<String, Object> properties) {
        this.properties = properties;
    }

}