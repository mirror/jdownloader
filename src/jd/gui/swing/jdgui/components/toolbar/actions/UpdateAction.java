package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.Icon;

import jd.gui.swing.jdgui.Flashable;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.InstallLog;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.updatev2.UpdaterListener;

public class UpdateAction extends AbstractToolBarAction {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public UpdateAction() {
        setIconKey("update");
        setEnabled(true);
        setAccelerator(KeyEvent.VK_U);

    }

    public void actionPerformed(ActionEvent e) {

        /* WebUpdate is running in its own Thread */
        new Thread() {
            public void run() {
                // runUpdateChecker is synchronized and may block
                UpdateController.getInstance().setGuiVisible(true);
                UpdateController.getInstance().runUpdateChecker(true);
            }
        }.start();

    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_start_update_tooltip();
    }

    public class Button extends ExtButton implements UpdaterListener, Flashable {

        public Button() {
            super(UpdateAction.this);
            setIcon(NewTheme.I().getIcon(getIconKey(), 24));
            setHideActionText(true);
            UpdateController.getInstance().getEventSender().addListener(this, true);

            if (UpdateController.getInstance().hasPendingUpdates()) {
                JDGui.getInstance().getFlashController().register(this);
            }
        }

        @Override
        public boolean isTooltipWithoutFocusEnabled() {
            return true;
        }

        @Override
        public int getTooltipDelay(Point mousePositionOnScreen) {
            return JDGui.getInstance().getFlashController().isRegistered(this) ? 100 : -1;
        }

        @Override
        public void onUpdatesAvailable(boolean selfupdate, InstallLog installlog) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    if (UpdateController.getInstance().hasPendingUpdates()) {
                        JDGui.getInstance().getFlashController().register(Button.this);
                        // setFlashing(true);
                        // setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK));
                    } else {
                        JDGui.getInstance().getFlashController().unregister(Button.this);
                        // setFlashing(false);
                    }

                }
            };

        }

        @Override
        public void onUpdaterStatusUpdate(String label, Icon icon, double progress) {
        }

        @Override
        public void onFlashRegister() {
            setToolTipText(_GUI._.UpdateAction_runInEDT_updates_pendings());
        }

        @Override
        public void onFlashUnRegister() {
            setToolTipText(_GUI._.UpdateAction_runInEDT_no_updates_pendings());
            Button.this.setIcon(NewTheme.I().getIcon(getIconKey(), 24));
        }

        @Override
        public boolean onFlash(long l) {
            if (!this.isVisible() || !this.isDisplayable()) {
                // cleanup
                return false;
            }
            if (l % 2 == 0) {
                Button.this.setIcon(NewTheme.I().getIcon("update_b", 24));
            } else {
                Button.this.setIcon(NewTheme.I().getIcon(getIconKey(), 24));
            }

            return true;
        }
    }

    public AbstractButton createButton() {

        Button bt = new Button();

        return bt;
    }

}
