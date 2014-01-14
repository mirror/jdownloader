package org.jdownloader;

import java.util.HashMap;
import java.util.Locale;

import javax.swing.Icon;

import jd.controlling.faviconcontroller.FavIconRequestor;
import jd.controlling.faviconcontroller.FavIcons;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.storage.config.MinTimeWeakReferenceCleanup;
import org.appwork.utils.Application;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.Interpolation;
import org.jdownloader.images.NewTheme;

public class DomainInfo implements FavIconRequestor, Comparable<DomainInfo>, MinTimeWeakReferenceCleanup {

    private static final long CACHE_TIMEOUT = 30000;
    private static final int  WIDTH         = 16;
    private static final int  HEIGHT        = 16;

    private DomainInfo(String tld) {
        this.tld = tld;
    }

    public String toString() {
        return tld;
    }

    private final String tld;

    public String getTld() {
        return tld;
    }

    protected volatile MinTimeWeakReference<Icon> hosterIcon = null;

    /**
     * Returns a
     * 
     * @return
     */
    public Icon getFavIcon() {
        Icon icon = null;
        MinTimeWeakReference<Icon> lhosterIcon = hosterIcon;
        if (lhosterIcon != null) {
            icon = lhosterIcon.get();
            if (icon != null) return icon;
        }
        // load it
        return setFavIcon(FavIcons.getFavIcon(getTld(), this));
    }

    public Icon setFavIcon(Icon icon) {
        if (icon == null) {
            MinTimeWeakReference<Icon> lhosterIcon = hosterIcon;
            if (lhosterIcon != null) {
                icon = lhosterIcon.get();
                if (icon != null) return icon;
            }
            icon = FavIcons.getFavIcon(getTld(), this);
        }
        if (icon != null) {
            icon = IconIO.getScaledInstance(icon, WIDTH, HEIGHT, Interpolation.BICUBIC);
            this.hosterIcon = new MinTimeWeakReference<Icon>(icon, CACHE_TIMEOUT, getTld(), this);
        }
        return icon;
    }

    private static volatile HashMap<String, DomainInfo> CACHE = new HashMap<String, DomainInfo>();
    private static final Object                         LOCK  = new Object();

    public static DomainInfo getInstance(String tld) {
        if (tld == null) return null;
        String lcaseTld = tld.toLowerCase(Locale.ENGLISH);
        DomainInfo ret = CACHE.get(lcaseTld);
        if (ret != null) return ret;
        synchronized (LOCK) {
            ret = CACHE.get(lcaseTld);
            if (ret != null) return ret;
            if (Application.getJavaVersion() >= Application.JAVA17) {
                lcaseTld = lcaseTld.intern();
            }
            if (ret == null) {
                HashMap<String, DomainInfo> newCache = new HashMap<String, DomainInfo>(CACHE);
                newCache.put(lcaseTld, ret = new DomainInfo(lcaseTld));
                CACHE = newCache;
            }
            return ret;
        }
    }

    /**
     * returns a high quality icon for this domain. most domains do not support this and will return null; the icon is NOT cached. use with care
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
        if (ret != null && ret.getIconHeight() >= size && ret.getIconWidth() >= size) { return IconIO.getScaledInstance(ret, size, size); }
        ret = FavIcons.getFavIcon(getTld(), null);
        if (ret.getIconHeight() >= size && ret.getIconWidth() >= size) { return IconIO.getScaledInstance(ret, size, size); }
        return null;
    }

    public PluginForHost findPlugin() {
        return JDUtilities.getPluginForHost(getTld());
    }

    public int compareTo(DomainInfo o) {
        return getTld().compareTo(o.getTld());
    }

    @Override
    public void onMinTimeWeakReferenceCleanup(MinTimeWeakReference<?> minTimeWeakReference) {
        if (minTimeWeakReference == hosterIcon) hosterIcon = null;
    }
}
