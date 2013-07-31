package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.Timer;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.toolbar.action.ToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.InstallLog;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.updatev2.UpdaterListener;

public class UpdateAction extends ToolBarAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public UpdateAction(SelectionInfo<?, ?> selection) {
        setIconKey("update");
        setEnabled(true);

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

    public class Button extends ExtButton implements UpdaterListener {
        protected Timer   timer;
        protected boolean trigger;

        public Button() {
            super(UpdateAction.this);
            setIcon(NewTheme.I().getIcon(getIconKey(), 24));
            setHideActionText(true);
            UpdateController.getInstance().getEventSender().addListener(this, true);

            if (UpdateController.getInstance().hasPendingUpdates()) {
                setFlashing(true);
                // setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK));
            } else {
                setFlashing(false);
            }
        }

        @Override
        public boolean isTooltipWithoutFocusEnabled() {
            return true;
        }

        @Override
        public int getTooltipDelay(Point mousePositionOnScreen) {
            return timer != null ? 100 : -1;
        }

        public void setFlashing(final boolean b) {

            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    if (timer != null) {
                        timer.stop();
                    }
                    if (b) {

                        if (CFG_GUI.CFG.isUpdateButtonFlashingEnabled()) {
                            setToolTipText(_GUI._.UpdateAction_runInEDT_updates_pendings());
                            trigger = true;
                            timer = new Timer(1000, new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    trigger = !trigger;
                                    if (trigger) {
                                        Button.this.setIcon(NewTheme.I().getIcon("update_b", 24));
                                    } else {
                                        Button.this.setIcon(NewTheme.I().getIcon(getIconKey(), 24));
                                    }
                                }
                            });
                            timer.setRepeats(true);
                            timer.start();
                        }
                    } else {
                        setToolTipText(_GUI._.UpdateAction_runInEDT_no_updates_pendings());
                        Button.this.setIcon(NewTheme.I().getIcon(getIconKey(), 24));
                    }
                }
            };

        }

        @Override
        public void onUpdatesAvailable(boolean selfupdate, InstallLog installlog) {
            if (UpdateController.getInstance().hasPendingUpdates()) {
                setFlashing(true);
                // setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK));
            } else {
                setFlashing(false);
            }
        }
    }

    public AbstractButton createButton() {
        Button bt = new Button();

        return bt;
    }

}
