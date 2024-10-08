package org.jdownloader.plugins.controller.crawler;

import jd.plugins.PluginForDecrypt;

import org.jdownloader.DomainInfo;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.LazyPluginClass;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;

public class LazyCrawlerPlugin extends LazyPlugin<PluginForDecrypt> {
    public LazyCrawlerPlugin(LazyPluginClass lazyPluginClass, String pattern, String displayName, Class<PluginForDecrypt> pluginClass, PluginClassLoaderChild classLoaderChild) {
        super(lazyPluginClass, pattern, displayName, pluginClass, classLoaderChild);
    }

    private volatile long decrypts               = 0;
    private volatile long decryptsRuntime        = 0;
    private volatile long averageRuntime         = 0;
    private volatile long pluginUsage            = 0;
    private int           maxConcurrentInstances = Integer.MAX_VALUE;
    private boolean       hasConfig              = false;
    private String        configInterface        = null;

    public long getPluginUsage() {
        return decrypts + pluginUsage;
    }

    public void setPluginUsage(long pluginUsage) {
        this.pluginUsage = Math.max(0, pluginUsage);
    }

    public boolean isHasConfig() {
        return hasConfig;
    }

    public String getHost() {
        return getDisplayName();
    }

    @Override
    protected void setFeatures(LazyPlugin.FEATURE[] features) {
        super.setFeatures(features);
    }

    @Override
    public String getClassName() {
        return "jd.plugins.decrypter.".concat(getLazyPluginClass().getClassName());
    }

    protected void setHasConfig(boolean hasConfig) {
        this.hasConfig = hasConfig;
    }

    public String getConfigInterface() {
        return configInterface;
    }

    public void setConfigInterface(String configInterface) {
        this.configInterface = configInterface;
    }

    public long getAverageCrawlRuntime() {
        return averageRuntime;
    }

    private String[] sitesSupported = null;

    public String[] getSitesSupported() {
        final String[] sitesSupported = this.sitesSupported;
        if (sitesSupported != null) {
            return sitesSupported;
        } else {
            return new String[] { getHost() };
        }
    }

    protected void setSitesSupported(final String[] sitesSupported) {
        if (sitesSupported == null || sitesSupported.length == 0) {
            this.sitesSupported = null;
        } else if (sitesSupported.length == 1 && getHost().equals(sitesSupported[0])) {
            this.sitesSupported = null;
        } else {
            this.sitesSupported = sitesSupported;
        }
    }

    public void updateCrawlRuntime(long r) {
        synchronized (this) {
            decrypts++;
            if (r >= 0) {
                decryptsRuntime += r;
            }
            averageRuntime = decryptsRuntime / decrypts;
        }
    }

    public DomainInfo getDomainInfo() {
        return DomainInfo.getInstance(getHost());
    }

    @Override
    public PluginForDecrypt newInstance(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        try {
            final PluginForDecrypt ret = super.newInstance(classLoader);
            ret.setLazyC(this);
            return ret;
        } catch (UpdateRequiredClassNotFoundException e) {
            this.setHasConfig(false);
            throw e;
        }
    }

    @Override
    public PluginForDecrypt getPrototype(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        try {
            final PluginForDecrypt ret = super.getPrototype(classLoader);
            ret.setLazyC(this);
            return ret;
        } catch (UpdateRequiredClassNotFoundException e) {
            this.setHasConfig(false);
            throw e;
        }
    }

    /**
     * @return the maxConcurrentInstances
     */
    public int getMaxConcurrentInstances() {
        return maxConcurrentInstances;
    }

    /**
     * @param maxConcurrentInstances
     *            the maxConcurrentInstances to set
     */
    public void setMaxConcurrentInstances(int maxConcurrentInstances) {
        this.maxConcurrentInstances = Math.max(1, maxConcurrentInstances);
    }

    @Override
    public PluginForDecrypt getPrototype(PluginClassLoaderChild classLoader, boolean fallBackPlugin) throws UpdateRequiredClassNotFoundException {
        return getPrototype(classLoader);
    }

    @Override
    public PluginForDecrypt newInstance(PluginClassLoaderChild classLoader, boolean fallBackPlugin) throws UpdateRequiredClassNotFoundException {
        return newInstance(classLoader);
    }
}
