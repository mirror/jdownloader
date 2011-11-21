package org.jdownloader;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.ImageIcon;

import jd.controlling.FavIconController;
import jd.controlling.FavIconRequestor;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.Interpolation;
import org.jdownloader.images.NewTheme;

public class DomainInfo implements FavIconRequestor {
    private static final long CACHE_TIMEOUT = 30000;
    private static final int  WIDTH         = 16;
    private static final int  HEIGHT        = 16;

    private DomainInfo(String tld) {
        this.tld = tld;
    }

    private String tld;

    public String getTld() {
        return tld;
    }

    protected MinTimeWeakReference<ImageIcon> hosterIcon          = null;
    protected boolean                         hosterIconRequested = false;

    public void setTld(String tld) {
        this.tld = tld;
    }

    /**
     * Returns a
     * 
     * @return
     */
    public synchronized ImageIcon getFavIcon() {
        ImageIcon ia = null;
        if (hosterIcon != null) {
            ia = hosterIcon.get();
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
            ia = FavIconController.getFavIcon(getTld(), this, true);
            if (ia != null) {
                ia = setFavIcon(ia);
                return ia;
            }
        }
        /* use default favicon */
        ia = setFavIcon(null);
        return ia;

    }

    /**
     * Creates a dummyHosterIcon
     */
    public BufferedImage createDefaultFavIcon() {
        int w = 16;
        int h = 16;
        int size = 9;
        System.out.println("Create Default " + getTld());
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final BufferedImage image = gc.createCompatibleImage(w, h, Transparency.BITMASK);

        String tld = Files.getExtension(getTld());
        if (tld != null) tld = tld.toLowerCase(Locale.ENGLISH);
        String dummy = getTld().toUpperCase();

        // remove tld
        try {
            dummy = dummy.substring(0, dummy.lastIndexOf("."));
        } catch (Throwable t) {

        }

        // clean up
        dummy = dummy.replaceAll("[\\d\\WEIOAJYU]", "");

        try {
            dummy = "" + dummy.charAt(0) + dummy.charAt(dummy.length() / 2);
        } catch (Throwable t) {

        }
        if (dummy.length() <= 0 || dummy.length() > 2) dummy = getTld().substring(0, 2);
        // paint
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("Helvetica", Font.BOLD, size));

        Rectangle2D bounds = g.getFontMetrics().getStringBounds(dummy, g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK);
        g.drawString(dummy, (int) (w - bounds.getWidth()) / 2, (int) (-bounds.getY() + (h - bounds.getHeight()) / 2) - (tld == null ? 0 : 1));
        if (tld != null) {
            g.setFont(new Font("Arial", 0, 6));
            bounds = g.getFontMetrics().getStringBounds("." + tld, g);
            g.setColor(Color.BLACK);
            g.drawString("." + tld, (int) (w - bounds.getWidth()) - 1, (int) (h) - 1);
        }
        g.dispose();

        return image;
    }

    /* reset customized favicon */
    public void resetFavIcon() {
        hosterIconRequested = false;
        hosterIcon = null;
    }

    public synchronized ImageIcon setFavIcon(ImageIcon icon) {
        if (icon == null) {
            if (hosterIcon != null) {
                icon = hosterIcon.get();
            }
            if (icon == null) {
                icon = new ImageIcon(createDefaultFavIcon());
            }
        } else {
            icon = new ImageIcon(IconIO.getScaledInstance(icon.getImage(), WIDTH, HEIGHT, Interpolation.BICUBIC, true));
        }
        this.hosterIcon = new MinTimeWeakReference<ImageIcon>(icon, CACHE_TIMEOUT, getTld());
        return icon;
    }

    private static HashMap<String, DomainInfo> CACHE = new HashMap<String, DomainInfo>();

    public static DomainInfo getInstance(String host) {
        // WARNING: can be a memleak
        synchronized (CACHE) {
            DomainInfo ret = CACHE.get(host);
            if (ret == null) {
                CACHE.put(host, ret = new DomainInfo(host));
            }
            return ret;
        }
    }

    public AffiliateSettings getAffiliateSettings() {
        URL url = Application.getRessourceURL("cfg/aff." + getTld() + ".json");
        if (url != null && url.getProtocol().equals("file")) {
            try {
                File path = new File(new File(url.toURI()).getParentFile(), "aff." + getTld());
                return JsonConfig.create(path, AffiliateSettings.class);
            } catch (URISyntaxException e) {
                return JsonConfig.create(AffiliateSettings.class);
            }
        } else {
            return JsonConfig.create(AffiliateSettings.class);
        }
    }

    public String getName() {
        String name = getAffiliateSettings().getName();
        if (name == null) return getTld();
        return name;
    }

    /**
     * returns a high quality icon for this domain. most domains do not support
     * this and will return null; the icon is NOT cached. use with care
     * 
     * @param i
     * @return
     */
    public ImageIcon getIcon(int size) {

        ImageIcon ret = null;

        if (NewTheme.I().hasIcon("fav/" + getTld())) {
            ret = NewTheme.I().getIcon("fav/" + getTld(), -1);
        }

        if (ret != null && ret.getIconHeight() >= size && ret.getIconWidth() >= size) {

        return new ImageIcon(IconIO.getScaledInstance((BufferedImage) ret.getImage(), size, size));

        }
        if (!hosterIconRequested) getFavIcon();
        ret = FavIconController.getFavIcon(getTld(), null, true);
        if (ret.getIconHeight() >= size && ret.getIconWidth() >= size) { return new ImageIcon(IconIO.getScaledInstance((BufferedImage) ret.getImage(), size, size)); }
        return null;
    }

    public PluginForHost findPlugin() {
        return JDUtilities.getPluginForHost(getTld());
    }
}
