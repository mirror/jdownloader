package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.Icon;

import jd.gui.swing.jdgui.Flashable;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.InstallLog;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.updatev2.UpdaterListener;

public class UpdateAction extends AbstractToolBarAction {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public UpdateAction() {
        setIconKey(IconKey.ICON_UPDATE);
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
        return _GUI.T.action_start_update_tooltip();
    }

    public class Button extends ExtButton implements UpdaterListener, Flashable, GenericConfigEventListener<Boolean> {

        private final BooleanKeyHandler booleanKeyHandler = CFG_GUI.UPDATE_BUTTON_FLASHING_ENABLED;

        public Button() {
            super(UpdateAction.this);
            setIcon(NewTheme.I().getIcon(getIconKey(), 24));
            setHideActionText(true);
            UpdateController.getInstance().getEventSender().addListener(this, true);
            booleanKeyHandler.getEventSender().addListener(this, true);
            if (booleanKeyHandler.isEnabled() && UpdateController.getInstance().hasPendingUpdates()) {
                JDGui.getInstance().getFlashController().register(this);
            } else {
                JDGui.getInstance().getFlashController().unregister(this);
            }
        }

        @Override
        public void onConfigValueModified(final KeyHandler<Boolean> keyHandler, Boolean newValue) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    if (booleanKeyHandler.isEnabled() && UpdateController.getInstance().hasPendingUpdates()) {
                        JDGui.getInstance().getFlashController().register(Button.this);
                    } else {
                        JDGui.getInstance().getFlashController().unregister(Button.this);
                    }
                }
            };
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
        public void onUpdatesAvailable(final boolean selfupdate, final InstallLog installlog) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    if (booleanKeyHandler.isEnabled() && UpdateController.getInstance().hasPendingUpdates()) {
                        JDGui.getInstance().getFlashController().register(Button.this);
                    } else {
                        JDGui.getInstance().getFlashController().unregister(Button.this);
                    }

                }
            };

        }

        @Override
        public void onUpdaterStatusUpdate(String label, Icon icon, double progress) {
        }

        @Override
        public void onFlashRegister(long counter) {
            setToolTipText(_GUI.T.UpdateAction_runInEDT_updates_pendings());
            onFlash(counter);
        }

        @Override
        public void onFlashUnRegister(long counter) {
            setToolTipText(_GUI.T.UpdateAction_runInEDT_no_updates_pendings());
            Button.this.setIcon(NewTheme.I().getIcon(getIconKey(), 24));
        }

        @Override
        public boolean onFlash(long l) {
            if (!this.isVisible() || !this.isDisplayable()) {
                // cleanup
                return false;
            }
            if (l % 2 == 0) {
                Button.this.setIcon(NewTheme.I().getIcon(IconKey.ICON_UPDATE_B, 24));
            } else {
                Button.this.setIcon(NewTheme.I().getIcon(getIconKey(), 24));
            }
            return true;
        }

        @Override
        public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
        }
    }

    public AbstractButton createButton() {
        final Button bt = new Button();
        return bt;
    }

}
