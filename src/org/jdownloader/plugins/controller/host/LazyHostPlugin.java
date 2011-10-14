package org.jdownloader.plugins.controller.host;

import javax.swing.ImageIcon;

import jd.plugins.PluginForHost;

import org.jdownloader.DomainInfo;
import org.jdownloader.plugins.controller.LazyPlugin;

public class LazyHostPlugin extends LazyPlugin<PluginForHost> {

    private static final String JD_PLUGINS_HOSTER = "jd.plugins.hoster.";
    private String              premiumUrl;

    public String getHost() {
        return getDisplayName();
    }

    public String getPremiumUrl() {
        return premiumUrl;
    }

    public void setPremiumUrl(String premiumUrl) {
        this.premiumUrl = premiumUrl;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    private boolean premium;

    public LazyHostPlugin(AbstractHostPlugin ap, Class<PluginForHost> plgClass, PluginForHost prototype) {
        super(ap.getPattern(), JD_PLUGINS_HOSTER + ap.getClassname(), ap.getDisplayName());
        premiumUrl = ap.getPremiumUrl();
        premium = ap.isPremium();

        this.pluginClass = plgClass;
        this.prototypeInstance = prototype;
    }

    public LazyHostPlugin(AbstractHostPlugin ap) {
        super(ap.getPattern(), JD_PLUGINS_HOSTER + ap.getClassname(), ap.getDisplayName());
        premiumUrl = ap.getPremiumUrl();
        premium = ap.isPremium();
    }

    public boolean hasConfig() {
        return false;
    }

    public ImageIcon getIcon() {
        return DomainInfo.getInstance(getDisplayName()).getFavIcon();
    }

}
