package org.jdownloader.plugins.controller.host;

import jd.plugins.PluginForHost;

import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.LazyPluginClass;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;

public class LazyHostPlugin extends LazyPlugin<PluginForHost> {

    private String         premiumUrl;
    private boolean        hasConfig      = false;
    private LazyHostPlugin fallBackPlugin = null;

    private volatile long  parsesLifetime = 0;

    public long getPluginUsage() {
        return parsesLifetime + parses;
    }

    public void setPluginUsage(long pluginUsage) {
        this.parsesLifetime = Math.max(0, pluginUsage);
    }

    private volatile long parses         = 0;
    private volatile long parsesRuntime  = 0;
    private volatile long averageRuntime = 0;

    public long getAverageParseRuntime() {
        return averageRuntime;
    }

    public void updateParseRuntime(long r) {
        synchronized (this) {
            parses++;
            if (r >= 0) {
                parsesRuntime += r;
            }
            averageRuntime = parsesRuntime / parses;
        }
    }

    @Override
    public String getClassName() {
        return "jd.plugins.hoster.".concat(getLazyPluginClass().getClassName());
    }

    public boolean isHasConfig() {
        return hasConfig;
    }

    protected void setHasConfig(boolean hasConfig) {
        this.hasConfig = hasConfig;
    }

    public String getHost() {
        return getDisplayName();
    }

    public String getPremiumUrl() {
        return premiumUrl;
    }

    protected void setPremiumUrl(String premiumUrl) {
        this.premiumUrl = premiumUrl;
    }

    public boolean isPremium() {
        return premium;
    }

    protected void setPremium(boolean premium) {
        this.premium = premium;
    }

    private boolean premium;
    private boolean hasAccountRewrite;
    private boolean hasLinkRewrite;
    private String  configInterface = null;

    public LazyHostPlugin(LazyPluginClass lazyPluginClass, String pattern, String displayName, Class<PluginForHost> pluginClass, PluginClassLoaderChild classLoaderChild) {
        super(lazyPluginClass, pattern, displayName, pluginClass, classLoaderChild);
    }

    public String getConfigInterface() {
        return configInterface;
    }

    public void setConfigInterface(String configInterface) {
        this.configInterface = configInterface;
    }

    public boolean isHasAccountRewrite() {
        return hasAccountRewrite;
    }

    public void setHasAccountRewrite(boolean hasAccountRewrite) {
        this.hasAccountRewrite = hasAccountRewrite;
    }

    public boolean isHasLinkRewrite() {
        return hasLinkRewrite;
    }

    public void setHasLinkRewrite(boolean hasLinkRewrite) {
        this.hasLinkRewrite = hasLinkRewrite;
    }

    @Override
    public PluginForHost newInstance(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        PluginForHost ret = null;
        try {
            ret = super.newInstance(classLoader);
            ret.setLazyP(this);
            return ret;
        } catch (UpdateRequiredClassNotFoundException e) {
            final LazyHostPlugin lFallBackPlugin = fallBackPlugin;
            if (lFallBackPlugin != null && lFallBackPlugin != this) {
                ret = lFallBackPlugin.newInstance(classLoader);
                if (ret != null) {
                    ret.setLazyP(lFallBackPlugin);
                    return ret;
                }
            }
            throw e;

        }
    }

    @Override
    public PluginForHost getPrototype(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        PluginForHost ret = null;
        try {
            ret = super.getPrototype(classLoader);
            ret.setLazyP(this);
            return ret;
        } catch (UpdateRequiredClassNotFoundException e) {
            final LazyHostPlugin lFallBackPlugin = fallBackPlugin;
            if (lFallBackPlugin != null && lFallBackPlugin != this) {
                ret = lFallBackPlugin.getPrototype(classLoader);
                if (ret != null) {
                    ret.setLazyP(lFallBackPlugin);
                    return ret;
                }
            }
            throw e;
        }
    }

    /**
     * use fallBackPlugin LazyHostPlugin in case we can't initialize/instance this LazyHostPlugin
     * 
     * @param fallBackPlugin
     */
    protected void setFallBackPlugin(LazyHostPlugin fallBackPlugin) {
        this.fallBackPlugin = fallBackPlugin;
    }

}
