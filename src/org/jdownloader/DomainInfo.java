package org.jdownloader;

import java.awt.Component;
import java.awt.Graphics;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.config.Property;
import jd.controlling.faviconcontroller.FavIconRequestor;
import jd.controlling.faviconcontroller.FavIcons;
import jd.http.Browser;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.swing.components.IDIcon;
import org.appwork.swing.components.IconIdentifier;
import org.appwork.utils.images.IconIO;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;

public class DomainInfo implements FavIconRequestor, Comparable<DomainInfo>, Icon, IDIcon {
    private static final HashMap<String, String> HARDCODEDFAVICONS = new HashMap<String, String>();
    static {
        HARDCODEDFAVICONS.put("http", IconKey.ICON_URL);// 'http links' results in 'http'
        HARDCODEDFAVICONS.put("ftp", IconKey.ICON_URL);
        HARDCODEDFAVICONS.put("filesystem", IconKey.ICON_HARDDISK);
        HARDCODEDFAVICONS.put("directhttp", IconKey.ICON_URL);
        HARDCODEDFAVICONS.put("f4m", IconKey.ICON_URL);
        HARDCODEDFAVICONS.put("m3u8", IconKey.ICON_URL);
        HARDCODEDFAVICONS.put("updaterequired", IconKey.ICON_URL);
        HARDCODEDFAVICONS.put("jdlog", IconKey.ICON_URL);
        HARDCODEDFAVICONS.put("cloudcache", IconKey.ICON_URL);
        HARDCODEDFAVICONS.put("usenet", IconKey.ICON_LOGO_NZB);
        HARDCODEDFAVICONS.put("genericusenet", IconKey.ICON_LOGO_NZB);
    }
    private static final int                     WIDTH             = 16;
    private static final int                     HEIGHT            = 16;
    private final String                         domain;
    private final IconIdentifier                 iconIdentifier;

    private DomainInfo(String tld, String domain) {
        this.tld = Property.dedupeString(tld);
        if (domain == null || domain.equals(tld)) {
            this.domain = tld;
        } else {
            this.domain = Property.dedupeString(domain);
        }
        this.iconIdentifier = new IconIdentifier("DomainInfo", domain);
    }

    public String toString() {
        return tld;
    }

    private final String tld;

    public String getDomain() {
        return domain;
    }

    public String getTld() {
        return tld;
    }

    protected Icon hosterIcon = null;

    protected void setHosterIcon(Icon icon) {
        this.hosterIcon = icon;
    }

    protected Icon getHosterIcon() {
        return hosterIcon;
    }

    protected Icon getIcon(final FavIconRequestor requestor, int width, int height, final boolean updatePermission) {
        Icon ret = null;
        final NewTheme theme = NewTheme.I();
        if (theme.hasIcon("fav/big." + domain)) {
            ret = new AbstractIcon("fav/big." + domain, -1);
        }
        if (ret == null && theme.hasIcon("fav/" + domain)) {
            ret = new AbstractIcon("fav/" + domain, -1);
        }
        if (ret != null && ret.getIconHeight() >= height && ret.getIconWidth() >= width) {
            return IconIO.getScaledInstance(ret, width, height);
        } else {
            return getFavIcon(requestor, width, height, updatePermission);
        }
    }

    protected Icon getFavIcon(final FavIconRequestor requestor, int width, int height, final boolean updatePermission) {
        Icon ret = getHosterIcon();
        if (ret == null) {
            final String hardcodedFavIcon = HARDCODEDFAVICONS.get(domain);
            if (hardcodedFavIcon != null) {
                ret = NewTheme.I().getIcon(hardcodedFavIcon, -1);
            } else {
                ret = FavIcons.getFavIcon(domain, requestor, updatePermission);
                if (ret == null && !updatePermission) {
                    ret = FavIcons.getDefaultIcon(domain, false);
                }
            }
            if (ret != null) {
                ret = new ImageIcon(FavIcons.getCroppedImage(IconIO.toBufferedImage(ret), false));
            }
        }
        if (ret != null) {
            ret = IconIO.getScaledInstance(ret, width, height);
        }
        if (updatePermission) {
            setHosterIcon(ret);
        }
        return ret;
    }

    public Icon getFavIcon(final boolean updatePermission) {
        return getFavIcon(this, WIDTH, HEIGHT, updatePermission);
    }

    public Icon getFavIcon() {
        return getFavIcon(true);
    }

    public Icon setFavIcon(Icon icon) {
        if (icon == null) {
            icon = getFavIcon(true);
        }
        setHosterIcon(icon);
        return icon;
    }

    private static final HashMap<String, WeakReference<DomainInfo>> CACHE = new HashMap<String, WeakReference<DomainInfo>>();

    private static String getCacheID(String domain) {
        String ret = domain.toLowerCase(Locale.ENGLISH);
        int index = ret.indexOf(" ");
        if (index > 0) {
            // for examle recaptcha.com (google)
            ret = ret.substring(0, index);
        }
        index = ret.indexOf("/");
        if (index > 0) {
            // for examle recaptcha.com/bla
            ret = ret.substring(0, index);
        }
        return ret;
    }

    public static DomainInfo getInstance(final String domain) {
        if (domain != null) {
            final String lcaseTld = getCacheID(domain);
            synchronized (CACHE) {
                DomainInfo ret = null;
                WeakReference<DomainInfo> domainInfo = CACHE.get(lcaseTld);
                if (domainInfo == null || (ret = domainInfo.get()) == null) {
                    ret = new DomainInfo(Browser.getHost(lcaseTld), lcaseTld);
                    CACHE.put(lcaseTld, new WeakReference<DomainInfo>(ret));
                }
                return ret;
            }
        }
        return null;
    }

    /**
     * WARNING: MAY RETURN null if size is too big returns a high quality icon for this domain. most domains do not support this and will
     * return null; the icon is NOT cached. use with care
     *
     * @param i
     * @return
     */
    public Icon getIcon(int size) {
        return getIcon(size, true);
    }

    public Icon getIcon(int size, final boolean updatePermission) {
        return getIcon(null, size, size, updatePermission);
    }

    @Deprecated
    public PluginForHost findPlugin() {
        return JDUtilities.getPluginForHost(getTld());
    }

    public int compareTo(DomainInfo o) {
        return getTld().compareTo(o.getTld());
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        getFavIcon().paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
        return WIDTH;
    }

    @Override
    public int getIconHeight() {
        return HEIGHT;
    }

    @Override
    public IconIdentifier getIdentifier() {
        return iconIdentifier;
    }
}
