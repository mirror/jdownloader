package org.jdownloader.settings;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JFrame;

import jd.gui.swing.jdgui.JDownloaderMainFrame;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.Storable;
import org.appwork.utils.swing.EDTHelper;

public class FrameStatus implements Storable {
    public static enum ExtendedState {
        /**
         * Frame is in the "normal" state. This symbolic constant names a frame state with all state bits cleared.
         * 
         * @see #setExtendedState(int)
         * @see #getExtendedState
         */
        NORMAL(Frame.NORMAL),

        /**
         * This state bit indicates that frame is iconified.
         * 
         * @see #setExtendedState(int)
         * @see #getExtendedState
         */
        ICONIFIED(Frame.ICONIFIED),

        /**
         * This state bit indicates that frame is maximized in the horizontal direction.
         * 
         * @see #setExtendedState(int)
         * @see #getExtendedState
         * @since 1.4
         */
        MAXIMIZED_HORIZ(Frame.MAXIMIZED_HORIZ),

        /**
         * This state bit indicates that frame is maximized in the vertical direction.
         * 
         * @see #setExtendedState(int)
         * @see #getExtendedState
         * @since 1.4
         */
        MAXIMIZED_VERT(Frame.MAXIMIZED_VERT),

        /**
         * This state bit mask indicates that frame is fully maximized (that is both horizontally and vertically). It is just a convenience
         * alias for <code>MAXIMIZED_VERT&nbsp;|&nbsp;MAXIMIZED_HORIZ</code>.
         * 
         * <p>
         * Note that the correct test for frame being fully maximized is
         * 
         * <pre>
         * (state &amp; Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH
         * </pre>
         * 
         * <p>
         * To test is frame is maximized in <em>some</em> direction use
         * 
         * <pre>
         * (state &amp; Frame.MAXIMIZED_BOTH) != 0
         * </pre>
         * 
         * @see #setExtendedState(int)
         * @see #getExtendedState
         * @since 1.4
         */
        MAXIMIZED_BOTH(Frame.MAXIMIZED_BOTH);

        private int id;

        private ExtendedState(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    private ExtendedState extendedState  = ExtendedState.NORMAL;
    private int           width          = -1;
    private int           height         = -1;
    private int           x              = -1;
    private boolean       visible        = true;
    private int           y              = -1;
    private boolean       silentShutdown = false;
    private String        screenID       = null;

    public ExtendedState getExtendedState() {
        return extendedState;
    }

    public void setExtendedState(ExtendedState extendedState) {
        this.extendedState = extendedState;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    private boolean locationSet = false;

    public boolean isLocationSet() {
        return locationSet;
    }

    public void setLocationSet(boolean locationSet) {
        this.locationSet = locationSet;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isSilentShutdown() {
        return silentShutdown;
    }

    public void setSilentShutdown(boolean silentShutdown) {
        this.silentShutdown = silentShutdown;
    }

    public boolean isFocus() {
        return focus;
    }

    public void setFocus(boolean focus) {
        this.focus = focus;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private boolean focus  = true;
    private boolean active = false;

    public FrameStatus(/* storable */) {

    }

    public static FrameStatus create(JFrame mainFrame, FrameStatus ret) {
        if (ret == null) ret = new FrameStatus();

        try {
            ret.setScreenID(mainFrame.getGraphicsConfiguration().getDevice().getIDstring());
        } catch (final Throwable e) {
            e.printStackTrace();
        }

        Rectangle jdBounds = fetchBoundsFromEDT(mainFrame);
        if (jdBounds == null) return null;
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] screens = ge.getScreenDevices();
        Rectangle jdRectange = new Rectangle(jdBounds);
        boolean isok = false;
        for (final GraphicsDevice screen : screens) {
            final Rectangle bounds = screen.getDefaultConfiguration().getBounds();

            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen.getDefaultConfiguration());

            bounds.x += insets.left;
            bounds.y += insets.top;
            bounds.width -= (insets.left + insets.right);
            bounds.height -= (insets.top + insets.bottom);

            jdRectange.height = 30;
            Rectangle inter = jdRectange.intersection(bounds);

            if (inter.width > 50 && inter.height >= 30) {
                // ok. Titlebar is in screen.
                isok = true;

                break;

            }

        }

        if (isok) {

            if (jdBounds.width > 100 || ret.width < 100) ret.width = jdBounds.width;
            if (jdBounds.height > 100 || ret.height < 100) ret.height = jdBounds.height;
            ret.x = jdBounds.x;
            ret.y = jdBounds.y;
            ret.locationSet = true;
        }

        ret.visible = mainFrame.isVisible();
        ShutdownRequest requ = ShutdownController.getInstance().getShutdownRequest();
        ret.silentShutdown = requ != null && requ.isSilent();
        ret.focus = mainFrame.hasFocus();
        ret.active = mainFrame.isActive();

        for (ExtendedState es : ExtendedState.values()) {
            if (es.getId() == mainFrame.getExtendedState()) {
                ret.extendedState = es;
                break;
            }
        }

        return ret;
    }

    private static Rectangle fetchBoundsFromEDT(final JFrame mainFrame) {
        return new EDTHelper<Rectangle>() {

            @Override
            public Rectangle edtRun() {
                try {
                    if (!mainFrame.isShowing() && !(mainFrame instanceof JDownloaderMainFrame)) {

                        return null;
                    } else if (!mainFrame.isShowing() && (mainFrame instanceof JDownloaderMainFrame)) {
                        FrameStatus fs = ((JDownloaderMainFrame) mainFrame).getLatestFrameStatus();
                        if (fs == null || !fs.isVisible()) {
                            return null;
                        } else {
                            return new Rectangle(fs.x, fs.y, fs.width, fs.height);
                        }
                    }
                    Rectangle ret = new Rectangle();

                    ret.width = 30;
                    ret.height = 30;
                    if (mainFrame.getExtendedState() == Frame.NORMAL) {
                        ret.width = mainFrame.getSize().width;
                        ret.height = mainFrame.getSize().height;
                    } else if (mainFrame instanceof JDownloaderMainFrame) {
                        FrameStatus fs = ((JDownloaderMainFrame) mainFrame).getLatestFrameStatus();
                        if (fs != null && fs.isVisible()) {
                            ret.width = fs.width;
                            ret.height = fs.height;
                        }
                    }
                    if (mainFrame.isVisible() && mainFrame.getExtendedState() == JFrame.NORMAL) {
                        /* we also have to save location in other modes! */
                        ret.x = mainFrame.getLocationOnScreen().x;
                        ret.y = mainFrame.getLocationOnScreen().y;
                    } else if (mainFrame instanceof JDownloaderMainFrame) {
                        FrameStatus fs = ((JDownloaderMainFrame) mainFrame).getLatestFrameStatus();
                        if (fs != null && fs.isVisible()) {
                            ret.x = fs.x;
                            ret.y = fs.y;
                        }
                    }
                    return ret;
                } catch (Exception e) {
                    return null;
                }
            }
        }.getReturnValue();
    }

    /**
     * @return the screenID
     */
    public String getScreenID() {
        return screenID;
    }

    /**
     * @param screenID
     *            the screenID to set
     */
    public void setScreenID(String screenID) {
        this.screenID = screenID;
    }
}
