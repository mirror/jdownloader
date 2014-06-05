package org.jdownloader;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.Icon;

import jd.controlling.faviconcontroller.FavIconRequestor;
import jd.controlling.faviconcontroller.FavIcons;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.Application;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.Interpolation;
import org.jdownloader.images.NewTheme;

public class DomainInfo implements FavIconRequestor, Comparable<DomainInfo> {

    private static final int WIDTH  = 16;
    private static final int HEIGHT = 16;

    private DomainInfo(String tld) {
        if (Application.getJavaVersion() >= Application.JAVA17) {
            this.tld = tld.intern();
        } else {
            this.tld = tld;
        }
    }

    public String toString() {
        return tld;
    }

    private final String tld;

    public String getTld() {
        return tld;
    }

    protected Icon hosterIcon = null;

    /**
     * Returns a
     * 
     * @return
     */
    public Icon getFavIcon() {
        Icon icon = hosterIcon;
        if (icon == null) {
            icon = setFavIcon(FavIcons.getFavIcon(getTld(), this));
        }
        return icon;
    }

    public Icon setFavIcon(Icon icon) {
        if (icon == null) {
            icon = hosterIcon;
            if (icon != null) {
                return icon;
            }
            icon = FavIcons.getFavIcon(getTld(), this);
        }
        if (icon != null) {
            icon = IconIO.getScaledInstance(icon, WIDTH, HEIGHT, Interpolation.BICUBIC);
        }
        hosterIcon = icon;
        return icon;
    }

    private static final HashMap<String, WeakReference<DomainInfo>> CACHE = new HashMap<String, WeakReference<DomainInfo>>();

    public static DomainInfo getInstance(String tld) {
        if (tld != null) {
            final String lcaseTld = tld.toLowerCase(Locale.ENGLISH);
            synchronized (CACHE) {
                DomainInfo ret = null;
                WeakReference<DomainInfo> domainInfo = CACHE.get(lcaseTld);
                if (domainInfo == null || (ret = domainInfo.get()) == null) {
                    ret = new DomainInfo(lcaseTld);
                    CACHE.put(lcaseTld, new WeakReference<DomainInfo>(ret));
                }
                return ret;
            }
        }
        return null;
    }

    /**
     * returns a high quality icon for this domain. most domains do not support this and will return null; the icon is NOT cached. use with
     * care
     * 
     * @param i
     * @return
     */
    public Icon getIcon(int size) {
        Icon ret = null;
        if (NewTheme.I().hasIcon("fav/big." + getTld())) {
            ret = NewTheme.I().getIcon("fav/big." + getTld(), -1);
        }
        if (ret == null && NewTheme.I().hasIcon("fav/" + getTld())) {
            ret = NewTheme.I().getIcon("fav/" + getTld(), -1);
        }
        if (ret != null && ret.getIconHeight() >= size && ret.getIconWidth() >= size) {
            return IconIO.getScaledInstance(ret, size, size);
        }
        ret = FavIcons.getFavIcon(getTld(), null);
        if (ret.getIconHeight() >= size && ret.getIconWidth() >= size) {
            return IconIO.getScaledInstance(ret, size, size);
        }
        return null;
    }

    public PluginForHost findPlugin() {
        return JDUtilities.getPluginForHost(getTld());
    }

    public int compareTo(DomainInfo o) {
        return getTld().compareTo(o.getTld());
    }

}
