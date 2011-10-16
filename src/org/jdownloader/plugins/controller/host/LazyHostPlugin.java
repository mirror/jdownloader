package org.jdownloader.plugins.controller.host;

import javax.swing.ImageIcon;

import jd.plugins.PluginForHost;

import org.jdownloader.DomainInfo;
import org.jdownloader.plugins.controller.LazyPlugin;

public class LazyHostPlugin extends LazyPlugin<PluginForHost> {

    private static final String JD_PLUGINS_HOSTER = "jd.plugins.hoster.";
    private String              premiumUrl;
    private boolean             hasConfig         = false;

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

    public LazyHostPlugin(AbstractHostPlugin ap) {
        super(ap.getPattern(), JD_PLUGINS_HOSTER + ap.getClassname(), ap.getDisplayName());
        premiumUrl = ap.getPremiumUrl();
        premium = ap.isPremium();
        hasConfig = ap.isHasConfig();
    }

    public ImageIcon getIcon() {
        return DomainInfo.getInstance(getDisplayName()).getFavIcon();
    }

    @Override
    public PluginForHost newInstance() {
        PluginForHost ret = super.newInstance();
        ret.setLazyP(this);
        return ret;
    }

}
