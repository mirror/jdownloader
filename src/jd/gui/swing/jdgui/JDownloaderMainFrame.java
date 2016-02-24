package jd.gui.swing.jdgui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;

import javax.swing.JDialog;

import org.appwork.app.gui.ActiveDialogException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.swing.ExtJFrame;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.FrameStatus;
import org.jdownloader.settings.FrameStatus.ExtendedState;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class JDownloaderMainFrame extends ExtJFrame {
    private FrameStatus     latestNormalState;

    private LogSource       logger;
    private DelayedRunnable delayedStateSaver;

    public JDownloaderMainFrame(String string, final LogSource logger) {
        super(string);
        this.logger = logger;

        delayedStateSaver = new DelayedRunnable(250, 1000l) {
            @Override
            public void resetAndStart() {
                super.resetAndStart();
            }

            @Override
            public String getID() {
                return "AddLinksDialog";
            }

            @Override
            public void delayedrun() {
                // new EDTRunner() {
                //
                // @Override
                // protected void runInEDT() {
                updateFrameStatus();
                // }
                // };

            }

        };
        addComponentListener(new ComponentListener() {

            @Override
            public void componentShown(ComponentEvent e) {
                delayedStateSaver.resetAndStart();
            }

            @Override
            public void componentResized(ComponentEvent e) {

                delayedStateSaver.resetAndStart();

            }

            @Override
            public void componentMoved(ComponentEvent e) {
                delayedStateSaver.resetAndStart();

            }

            @Override
            public void componentHidden(ComponentEvent e) {
                delayedStateSaver.resetAndStart();
            }
        });
        addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {
                delayedStateSaver.resetAndStart();
            }

            @Override
            public void windowIconified(WindowEvent e) {
                delayedStateSaver.resetAndStart();
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                delayedStateSaver.resetAndStart();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                // delayedStateSaver.resetAndStart();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                delayedStateSaver.resetAndStart();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                delayedStateSaver.resetAndStart();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                // delayedStateSaver.resetAndStart();
            }
        });
        addWindowStateListener(new WindowStateListener() {

            @Override
            public void windowStateChanged(WindowEvent e) {
                delayedStateSaver.resetAndStart();

            }
        });
    }

    public FrameStatus getLatestNormalState() {
        return latestNormalState;
    }

    public void setLatestNormalState(FrameStatus latestNormalState) {
        this.latestNormalState = latestNormalState;
    }

    @Override
    public void setSize(Dimension d) {
        super.setSize(d);
    }

    @Override
    public void setSize(int width, int height) {
        System.out.println("Set Frame size: " + width + "x" + height);
        super.setSize(width, height);
    }

    @Override
    public Point getLocation() {
        return super.getLocation();
    }

    @Override
    public Point getLocation(Point rv) {
        return super.getLocation(rv);
    }

    @Override
    public Point getLocationOnScreen() {
        return super.getLocationOnScreen();
    }

    @Override
    public void setLocation(int x, int y) {
        super.setLocation(x, y);
    }

    @Override
    public Dimension getPreferredSize() {
        // Dimension su = super.getPreferredSize();
        // ;
        //
        // FrameStatus lfs = latestFrameStatus;
        // if (lfs == null) {
        // lfs = JsonConfig.create(GraphicalUserInterfaceSettings.class).getLastFrameStatus();
        // }
        //
        // if (lfs != null && lfs.getWidth() > 100 && lfs.getHeight() > 100) {
        //
        // return new Dimension(lfs.getWidth(), lfs.getHeight());
        // }
        return super.getPreferredSize();
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {

        super.setPreferredSize(preferredSize);
    }

    // public ExtendedState getLastKnownVisibleExtendedState() {
    // return lastKnownVisibleExtendedState;
    // }

    /**
     *
     */
    private static final long serialVersionUID = -4218493713632551975L;

    public void dispose() {

        super.dispose();
    }

    private volatile boolean dialogShowing = false;
    private FrameStatus      latestFrameStatus;

    public void setExtendedState(final int i) {
        // updateFrameStatus();
        // if (i != JFrame.NORMAL && getExtendedState() == JFrame.NORMAL) {
        // latestFrameStatus = FrameStatus.create(this, latestFrameStatus);
        //
        // }
        super.setExtendedState(i);
    }

    public void setVisible(boolean b) {

        if (b && !isVisible()) {
            if (CFG_GUI.PASSWORD_PROTECTION_ENABLED.isEnabled() && !StringUtils.isEmpty(CFG_GUI.PASSWORD.getValue())) {
                String password;
                if (dialogShowing) {
                    return;
                }
                try {

                    dialogShowing = true;
                    password = Dialog.getInstance().showInputDialog(Dialog.STYLE_PASSWORD, _GUI.T.JDGui_setVisible_password_(), _GUI.T.JDGui_setVisible_password_msg(), null, new AbstractIcon(IconKey.ICON_LOCK, 32), null, null);
                    String internPw = CFG_GUI.PASSWORD.getValue();
                    if (!internPw.equals(password)) {

                        Dialog.getInstance().showMessageDialog(_GUI.T.JDGui_setVisible_password_wrong());

                        return;
                    }
                } catch (DialogNoAnswerException e) {
                    return;
                } finally {
                    dialogShowing = false;
                }
            }
        }
        // if we hide a frame which is locked by an active modal dialog,
        // we get in problems. avoid this!
        if (!b) {
            // latestFrameStatus = FrameStatus.create(this, latestFrameStatus);
            for (Window w : getOwnedWindows()) {
                if (w instanceof JDialog) {
                    boolean mod = ((JDialog) w).isModal();
                    boolean v = w.isVisible();

                    if (mod && v) {
                        Toolkit.getDefaultToolkit().beep();
                        logger.log(new ActiveDialogException(((JDialog) w)));
                        WindowManager.getInstance().setZState(w, FrameState.TO_FRONT_FOCUSED);

                        return;
                    }
                }
            }
        }
        super.setVisible(b);
    }

    public FrameStatus getLatestFrameStatus() {
        return latestFrameStatus;
    }

    public void setLatestFrameStatus(FrameStatus latestFrameStatus) {
        this.latestFrameStatus = latestFrameStatus;
    }

    public void toFront() {

        if (!isVisible()) {
            return;
        }
        super.toFront();
        //

    }

    private void updateFrameStatus() {
        FrameStatus newState = FrameStatus.create(JDownloaderMainFrame.this, latestFrameStatus);

        if (newState.isLocationSet()) {
            latestFrameStatus = newState;
            if (newState.getExtendedState() == ExtendedState.NORMAL) {
                latestNormalState = newState.clone();

                System.out.println("New Window State (Normal): " + JSonStorage.toString(latestFrameStatus));
            } else {
                System.out.println("New Window State: " + JSonStorage.toString(latestFrameStatus));
            }

        }

        // Rectangle bounds = getGraphicsConfiguration().getBounds();
        // String dev = getGraphicsConfiguration().getDevice().getIDstring();
        // Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        // bounds.x += insets.left;
        // bounds.y += insets.top;
        // bounds.width -= insets.left + insets.right;
        // bounds.height -= insets.top + insets.bottom;
        // Rectangle usable = SunGraphicsEnvironment.getUsableBounds(getGraphicsConfiguration().getDevice());
        // setMaximizedBounds(bounds);
        // System.out.println("Set max bounds " + bounds + " " + dev + " " + usable);
    }

}
