package jd.http;

import java.util.LinkedHashMap;

public class LowerCaseHashMap<V> extends LinkedHashMap<String, V> {

    /**
     * 
     */
    private static final long serialVersionUID = 4571590512548374247L;

    public V get(Object key) {
        if (key != null && key.getClass() == String.class) { return super.get(((String) key).toLowerCase()); }
        return super.get(key);
    }

    public V put(String key, V value) {
        if (key != null) {
            return super.put(key.toLowerCase(), value);
        } else {
            return super.put(key, value);
        }
    }
    
    public V remove(Object key){
        if (key != null && key.getClass() == String.class) { return super.remove(((String) key).toLowerCase()); }
        return super.remove(key);
    }
}
