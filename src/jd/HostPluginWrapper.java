//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.swing.ImageIcon;

import jd.controlling.FavIconController;
import jd.controlling.FavIconRequestor;
import jd.controlling.JDLogger;
import jd.gui.swing.components.JDLabelContainer;
import jd.nutils.JDFlags;
import jd.nutils.JDImage;
import jd.plugins.PluginForHost;

public class HostPluginWrapper extends PluginWrapper implements JDLabelContainer, FavIconRequestor {
    private static final ArrayList<HostPluginWrapper> HOST_WRAPPER = new ArrayList<HostPluginWrapper>();

    private static final ReentrantReadWriteLock       lock         = new ReentrantReadWriteLock();
    public static final ReadLock                      readLock     = lock.readLock();
    public static final WriteLock                     writeLock    = lock.writeLock();

    static {
        try {
            writeLock.lock();
            JDInit.loadPluginForHost();
        } catch (Throwable e) {
            JDLogger.exception(e);
        } finally {
            writeLock.unlock();
        }
    }

    public static ArrayList<HostPluginWrapper> getHostWrapper() {
        return HOST_WRAPPER;
    }

    public static boolean hasPlugin(final String data) {
        for (HostPluginWrapper w : getHostWrapper()) {
            if (w.canHandle(data)) return true;
        }
        return false;
    }

    private static final String AGB_CHECKED = "AGB_CHECKED";

    private ImageIcon           icon        = null;

    public HostPluginWrapper(final String host, final String classNamePrefix, final String className, final String patternSupported, final int flags, final String revision) {
        super(host, classNamePrefix, className, patternSupported, flags, revision);
        try {
            writeLock.lock();
            for (HostPluginWrapper plugin : HOST_WRAPPER) {
                if (plugin.getID().equalsIgnoreCase(this.getID()) && plugin.getPattern().equals(this.getPattern())) {
                    if (JDFlags.hasNoFlags(flags, ALLOW_DUPLICATE)) {
                        logger.severe("Cannot add HostPlugin! HostPluginID " + getID() + " already exists!");
                        return;
                    }
                }
            }
            HOST_WRAPPER.add(this);
        } finally {
            writeLock.unlock();
        }
    }

    public HostPluginWrapper(final String host, final String simpleName, final String pattern, final int flags, final String revision) {
        this(host, "jd.plugins.hoster.", simpleName, pattern, flags, revision);
    }

    @Override
    public PluginForHost getPlugin() {
        return (PluginForHost) super.getPlugin();
    }

    @Override
    public PluginForHost getNewPluginInstance() {
        return (PluginForHost) super.getNewPluginInstance();
    }

    public boolean isAGBChecked() {
        return super.getPluginConfig().getBooleanProperty(AGB_CHECKED, false);
    }

    public void setAGBChecked(final Boolean value) {
        super.getPluginConfig().setProperty(AGB_CHECKED, value);
        super.getPluginConfig().save();
    }

    public boolean isPremiumEnabled() {
        return this.isLoaded() && this.getPlugin().isPremiumEnabled();
    }

    @Override
    public String toString() {
        return getHost();
    }

    public ImageIcon getIcon() {
        return getIconScaled();
    }

    public ImageIcon getIconScaled() {
        return JDImage.getScaledImageIcon(getIconUnscaled(), 16, -1);
    }

    public ImageIcon getIconUnscaled() {
        if (icon != null) return icon;
        /* try to load from disk */
        ImageIcon image = FavIconController.getFavIcon(getHost(), this, true);
        if (image != null) icon = image;
        /* use fallback icon */
        if (icon == null) icon = new ImageIcon(createDefaultFavIcon());
        return icon;
    }

    public String getLabel() {
        return toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof HostPluginWrapper)) return false;
        return this.getID().equalsIgnoreCase(((HostPluginWrapper) obj).getID());
    }

    /**
     * Creates a dummyHosterIcon
     */
    public BufferedImage createDefaultFavIcon() {
        int w = 16;
        int h = 16;
        int size = 9;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final BufferedImage image = gc.createCompatibleImage(w, h, Transparency.BITMASK);

        String classname = getClassName();
        String dummy = cleanString(classname.substring(classname.lastIndexOf('.')));
        if (dummy.length() < 2) dummy = getHost().toUpperCase();
        if (dummy.length() > 2) dummy = dummy.substring(0, 2);

        Graphics2D g = image.createGraphics();
        g.setFont(new Font("Arial", Font.BOLD, size));
        int ww = g.getFontMetrics().stringWidth(dummy);
        g.setColor(Color.WHITE);
        g.fillRect(1, 1, w - 2, h - 2);
        g.setColor(Color.BLACK);
        g.drawString(dummy, (w - ww) / 2, 2 + size);
        g.dispose();
        return image;
    }

    private String cleanString(String host) {
        return host.replaceAll("[a-z0-9\\-\\.]", "");
    }

    public void setFavIcon(ImageIcon icon) {
        this.icon = icon;
    }

}
