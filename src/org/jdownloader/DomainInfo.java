package org.jdownloader;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.ImageIcon;

import jd.controlling.faviconcontroller.FavIconRequestor;
import jd.controlling.faviconcontroller.FavIcons;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.utils.Application;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.Interpolation;
import org.jdownloader.images.NewTheme;

public class DomainInfo implements FavIconRequestor, Comparable<DomainInfo> {

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

    protected MinTimeWeakReference<ImageIcon> hosterIcon          = null;
    protected boolean                         hosterIconRequested = false;

    /**
     * Returns a
     * 
     * @return
     */
    public ImageIcon getFavIcon() {
        ImageIcon ia = null;
        MinTimeWeakReference<ImageIcon> lhosterIcon = hosterIcon;
        if (lhosterIcon != null) {
            ia = lhosterIcon.get();
            // cleanup;
            if (ia == null) {
                resetFavIcon();
            } else {
                return ia;
            }
        }
        if (!hosterIconRequested) {
            hosterIconRequested = true;
            // load it
            ia = FavIcons.getFavIcon(getTld(), this);
            if (ia != null) {
                ia = setFavIcon(ia);
                return ia;
            }
        }

        /* use default favicon */
        ia = setFavIcon(null);
        return ia;

    }

    /* reset customized favicon */
    public void resetFavIcon() {
        hosterIconRequested = false;
        hosterIcon = null;
    }

    public ImageIcon setFavIcon(ImageIcon icon) {
        if (icon == null) {
            MinTimeWeakReference<ImageIcon> lhosterIcon = hosterIcon;
            if (lhosterIcon != null) {
                icon = lhosterIcon.get();
            }
            if (icon == null) {
                icon = FavIcons.getFavIcon(getTld(), this);
            }
        } else {
            icon = new ImageIcon(IconIO.getScaledInstance(icon.getImage(), WIDTH, HEIGHT, Interpolation.BICUBIC, true));
        }
        this.hosterIcon = new MinTimeWeakReference<ImageIcon>(icon, CACHE_TIMEOUT, getTld());
        return icon;
    }

    private static HashMap<String, DomainInfo> CACHE = new HashMap<String, DomainInfo>();
    private static final Object                LOCK  = new Object();

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
            HashMap<String, DomainInfo> newCache = new HashMap<String, DomainInfo>(CACHE);
            if (ret == null) {
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
    public ImageIcon getIcon(int size) {
        ImageIcon ret = null;
        if (NewTheme.I().hasIcon("fav/big." + getTld())) {
            ret = NewTheme.I().getIcon("fav/big." + getTld(), -1);
        }
        if (ret == null && NewTheme.I().hasIcon("fav/" + getTld())) {
            ret = NewTheme.I().getIcon("fav/" + getTld(), -1);
        }
        if (ret != null && ret.getIconHeight() >= size && ret.getIconWidth() >= size) { return new ImageIcon(IconIO.getScaledInstance((BufferedImage) ret.getImage(), size, size)); }
        if (!hosterIconRequested) getFavIcon();
        ret = FavIcons.getFavIcon(getTld(), null);
        if (ret.getIconHeight() >= size && ret.getIconWidth() >= size) { return new ImageIcon(IconIO.getScaledInstance((BufferedImage) ret.getImage(), size, size)); }
        return null;
    }

    public PluginForHost findPlugin() {
        return JDUtilities.getPluginForHost(getTld());
    }

    public int compareTo(DomainInfo o) {
        return getTld().compareTo(o.getTld());
    }
}
