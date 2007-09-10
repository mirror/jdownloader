package jd;

import java.io.Serializable;
import java.util.HashMap;
/**
 * Von dieser Klasse kann abgeleitet werden wenn die Neue Klasse Properties unterstützen soll.
 * Die SimpleGUI elemente nutzen das um einfache Dialogelemente zu erstellen. Ein Automatisiertes speichern/laden wird dadurch möglich
 * @author coalado
 *
 */
public abstract class Property implements Serializable {

    private HashMap<String, Object> properties = new HashMap<String, Object>();

    public Property(){}
    /**
     * Speichert einen Wert ab.
     * 
     * @param key
     * @param value
     */
    public void setProperty(String key, Object value) {
        if(properties==null)properties = new HashMap<String, Object>();
        properties.put(key, value);
        
    }

    /**
     * Gibt dne Wert zu key zurück
     * 
     * @param key
     * @return
     */
    public Object getProperty(String key) {
        if(properties==null)properties = new HashMap<String, Object>();
        return properties.get(key);
    }

    public HashMap<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(HashMap<String, Object> properties) {
        this.properties = properties;
    }

}