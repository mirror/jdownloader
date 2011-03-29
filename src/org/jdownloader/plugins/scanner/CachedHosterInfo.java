package org.jdownloader.plugins.scanner;

import org.appwork.storage.Storable;

public class CachedHosterInfo extends CachePluginInfo implements Storable {

    @SuppressWarnings("unused")
    private CachedHosterInfo() {
        super();
        // required by Storable;
    }

    public boolean isMenuItemsAvailable() {
        return menuItemsAvailable;
    }

    public void setMenuItemsAvailable(boolean menuItems) {
        this.menuItemsAvailable = menuItems;
    }

    private boolean menuItemsAvailable;
    private String  tosLink;
    private boolean premiumEnabled;
    private String  premiumLink;

    public String getPremiumLink() {
        return premiumLink;
    }

    public void setPremiumLink(String premiumLink) {
        this.premiumLink = premiumLink;
    }

    public boolean isPremiumEnabled() {
        return premiumEnabled;
    }

    public void setPremiumEnabled(boolean premiumEnabled) {
        this.premiumEnabled = premiumEnabled;
    }

    public CachedHosterInfo(String name, String pattern, String revision, String hash, String classPath) {
        super(name, pattern, revision, hash, classPath);
    }

    public void setTosLink(String tosLink) {
        this.tosLink = tosLink;
    }

    public String getTosLink() {
        return tosLink;
    }

}
