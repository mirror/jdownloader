package jd.plugins;


public interface PluginCache {
    public void setCache(String key, Object value);

    public void removeCache(String key);

    public <T> T getCache(String key, T defaultValue);

    public void clearCache();
}
