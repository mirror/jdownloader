package org.jdownloader.settings;

import java.awt.Frame;

import javax.swing.JFrame;

import org.appwork.shutdown.ShutdownController;
import org.appwork.storage.Storable;

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
         * This state bit mask indicates that frame is fully maximized (that is both horizontally and vertically). It is just a convenience alias for
         * <code>MAXIMIZED_VERT&nbsp;|&nbsp;MAXIMIZED_HORIZ</code>.
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
        if (mainFrame.isShowing()) {
            try {
                ret.setScreenID(mainFrame.getGraphicsConfiguration().getDevice().getIDstring());
            } catch (final Throwable e) {
                e.printStackTrace();
            }
            if (mainFrame.getExtendedState() == Frame.NORMAL) {
                ret.width = mainFrame.getSize().width;
                ret.height = mainFrame.getSize().height;
            }
            /* we also have to save location in other modes! */
            ret.x = mainFrame.getLocationOnScreen().x;
            ret.y = mainFrame.getLocationOnScreen().y;
            ret.locationSet = true;
        }
        ret.visible = mainFrame.isVisible();
        ret.silentShutdown = ShutdownController.getInstance().isSilentShutDown();
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
