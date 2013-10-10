package org.jdownloader.plugins.controller.host;

import jd.plugins.PluginForHost;

import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;

public class LazyHostPlugin extends LazyPlugin<PluginForHost> {

    protected static final String JD_PLUGINS_HOSTER = "jd.plugins.hoster.";
    private String                premiumUrl;
    private boolean               hasConfig         = false;
    private LazyHostPlugin        fallBackPlugin    = null;

    private long                  parses            = 0;
    private long                  parsesRuntime     = 0;

    public long getAverageParseRuntime() {
        synchronized (this) {
            if (parses == 0 || parsesRuntime == 0) return 0;
            long ret = parsesRuntime / parses;
            return ret;
        }
    }

    protected AbstractHostPlugin getAbstractHostPlugin() {
        /* we only need the classname and not complete path */
        String className = getClassname().substring(JD_PLUGINS_HOSTER.length());
        AbstractHostPlugin ap = new AbstractHostPlugin(className);
        ap.setDisplayName(getDisplayName());
        if (isPremium()) {
            ap.setPremium(isPremium());
            ap.setPremiumUrl(getPremiumUrl());
        }
        ap.setHasAccountRewrite(isHasAccountRewrite());
        ap.setHasLinkRewrite(isHasLinkRewrite());
        ap.setPattern(getPatternSource());
        ap.setVersion(getVersion());
        ap.setHasConfig(isHasConfig());
        ap.setInterfaceVersion(getInterfaceVersion());

        ap.setMainClassSHA256(getMainClassSHA256());
        ap.setMainClassLastModified(getMainClassLastModified());
        ap.setMainClassFilename(getMainClassFilename());

        ap.setCacheVersion(AbstractHostPlugin.CACHEVERSION);
        return ap;
    }

    public void updateParseRuntime(long r) {
        synchronized (this) {
            if (r >= 0) {
                parses++;
                parsesRuntime += r;
            }
        }
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

    public LazyHostPlugin(AbstractHostPlugin ap, Class<PluginForHost> class1, PluginClassLoaderChild classLoaderChild) {
        super(ap.getPattern(), JD_PLUGINS_HOSTER + ap.getClassname(), ap.getDisplayName(), ap.getVersion(), class1, classLoaderChild);
        premiumUrl = ap.getPremiumUrl();
        premium = ap.isPremium();
        hasConfig = ap.isHasConfig();
        mainClassFilename = ap.getMainClassFilename();
        mainClassLastModified = ap.getMainClassLastModified();
        mainClassSHA256 = ap.getMainClassSHA256();
        interfaceVersion = ap.getInterfaceVersion();
        hasAccountRewrite = ap.isHasAccountRewrite();
        hasLinkRewrite = ap.isHasLinkRewrite();
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
            if (fallBackPlugin != null) {
                /* we could not create new instance, so let us use fallback LazyHostPlugin to instance a new Plugin */
                this.setClassname(fallBackPlugin.getClassname());
                this.setPluginClass(null);
                this.setClassLoader(null);
                /* remove reference from fallBackPlugin */
                fallBackPlugin = null;
                /* our fallBack Plugin does not have any settings */
                this.setHasConfig(false);
                ret = super.newInstance(classLoader);
                ret.setLazyP(this);
                return ret;
            } else
                throw e;
        }
    }

    @Override
    public PluginForHost getPrototype(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        PluginForHost ret = null;
        try {
            ret = super.getPrototype(classLoader);
            return ret;
        } catch (UpdateRequiredClassNotFoundException e) {
            if (fallBackPlugin != null) {
                /* we could not create protoType, so let us use fallback LazyHostPlugin to instance a new Prototype */
                this.setClassname(fallBackPlugin.getClassname());
                this.setPluginClass(null);
                this.setClassLoader(null);
                /* remove reference from fallBackPlugin */
                fallBackPlugin = null;
                /* our fallBack Plugin does not have any settings */
                this.setHasConfig(false);
                ret = super.getPrototype(classLoader);
                ret.setLazyP(this);
                return ret;
            } else
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
