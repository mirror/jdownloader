package jd.plugins;

public interface PluginCache {
    public Object set(String key, Object value);
    
    public Object remove(String key);
    
    public <T> T get(String key, T defaultValue);
    
    public void clear();
    
    public boolean containsKey(String key);
    
    public String getID();
}
