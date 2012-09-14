package org.jdownloader.plugins.controller.host;

import javax.swing.ImageIcon;

import jd.plugins.PluginForHost;

import org.jdownloader.DomainInfo;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;

public class LazyHostPlugin extends LazyPlugin<PluginForHost> {

    private static final String JD_PLUGINS_HOSTER = "jd.plugins.hoster.";
    private String              premiumUrl;
    private boolean             hasConfig         = false;
    private LazyHostPlugin      fallBackPlugin    = null;

    private long                parses            = 0;
    private long                parsesRuntime     = 0;

    public long getAverageParseRuntime() {
        synchronized (this) {
            if (parses == 0 || parsesRuntime == 0) return 0;
            long ret = parsesRuntime / parses;
            return ret;
        }
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

    public LazyHostPlugin(AbstractHostPlugin ap, Class<PluginForHost> class1, PluginClassLoaderChild classLoaderChild) {
        super(ap.getPattern(), JD_PLUGINS_HOSTER + ap.getClassname(), ap.getDisplayName(), ap.getVersion(), class1, classLoaderChild);
        premiumUrl = ap.getPremiumUrl();
        premium = ap.isPremium();
        hasConfig = ap.isHasConfig();
    }

    public ImageIcon getIcon() {
        return DomainInfo.getInstance(getDisplayName()).getFavIcon();
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
