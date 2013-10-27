package jd.gui.swing.jdgui;

import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.appwork.app.gui.ActiveDialogException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.swing.ExtJFrame;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.WindowManager;
import org.appwork.utils.swing.WindowManager.FrameState;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.FrameStatus;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class JDownloaderMainFrame extends ExtJFrame {

    private LogSource       logger;
    private DelayedRunnable delayedStateSaver;

    public JDownloaderMainFrame(String string, LogSource logger) {
        super(string);
        this.logger = logger;
        delayedStateSaver = new DelayedRunnable(500, 10000l) {

            @Override
            public String getID() {
                return "AddLinksDialog";
            }

            @Override
            public void delayedrun() {
                FrameStatus newState = FrameStatus.create(JDownloaderMainFrame.this, latestFrameStatus);
                if (newState.isLocationSet()) {
                    latestFrameStatus = newState;
                    System.out.println("new State " + JSonStorage.toString(latestFrameStatus));
                }
            }

        };
        addComponentListener(new ComponentListener() {

            @Override
            public void componentShown(ComponentEvent e) {
                System.out.println(e);
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
                System.out.println(e);
            }
        });
        addWindowStateListener(new WindowStateListener() {

            @Override
            public void windowStateChanged(WindowEvent e) {
                delayedStateSaver.resetAndStart();
            }
        });
    }

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
        if (i != JFrame.NORMAL && getExtendedState() == JFrame.NORMAL) {
            latestFrameStatus = FrameStatus.create(this, latestFrameStatus);

        }
        super.setExtendedState(i);
    }

    public void setVisible(boolean b) {

        if (b && !isVisible()) {
            if (CFG_GUI.PASSWORD_PROTECTION_ENABLED.isEnabled() && !StringUtils.isEmpty(CFG_GUI.PASSWORD.getValue())) {
                String password;
                if (dialogShowing) return;
                try {

                    dialogShowing = true;
                    password = Dialog.getInstance().showInputDialog(Dialog.STYLE_PASSWORD, _GUI._.JDGui_setVisible_password_(), _GUI._.JDGui_setVisible_password_msg(), null, NewTheme.I().getIcon("lock", 32), null, null);
                    String internPw = CFG_GUI.PASSWORD.getValue();
                    if (!internPw.equals(password)) {

                        Dialog.getInstance().showMessageDialog(_GUI._.JDGui_setVisible_password_wrong());

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
            latestFrameStatus = FrameStatus.create(this, latestFrameStatus);
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

    public void toFront() {

        if (!isVisible()) return;
        super.toFront();
        //

    }

}
