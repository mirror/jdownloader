package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.Dialog.ModalityType;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.Timer;

import org.appwork.swing.components.ExtButton;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
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
        setIconKey("update");
        setEnabled(true);
        setAccelerator(KeyEvent.VK_U);

    }

    public void actionPerformed(ActionEvent e) {
        if (true) {
            /* WebUpdate is running in its own Thread */
            new Thread() {
                public void run() {
                    // runUpdateChecker is synchronized and may block
                    UpdateController.getInstance().setGuiVisible(true);
                    UpdateController.getInstance().runUpdateChecker(true);
                }
            }.start();
        } else {

            new Thread() {
                public void run() {

                    try {
                        boolean installed = UpdateController.getInstance().getHandler().isExtensionInstalled("ffmpeg");
                        // UpdateController.getInstance().getHandler().uninstallExtension("ffmpeg");
                        UpdateController.getInstance().setGuiVisible(true);
                        ProgressGetter pg = new ProgressGetter() {

                            private String label;
                            private int    progress = -1;

                            @Override
                            public void run() throws Exception {

                                UpdaterListener listener = new UpdaterListener() {

                                    @Override
                                    public void onUpdatesAvailable(boolean selfupdate, InstallLog installlog) {
                                    }

                                    @Override
                                    public void onUpdaterStatusUpdate(String lbl, Icon icon, double progress) {
                                        System.out.println(label + " - " + progress);
                                        label = lbl;
                                        progress = Math.max(99, progress * 100);

                                    }
                                };
                                try {
                                    UpdateController.getInstance().getEventSender().addListener(listener);
                                    UpdateController.getInstance().getHandler().uninstallExtension("ffmpeg", "streaming");
                                } finally {
                                    UpdateController.getInstance().getEventSender().removeListener(listener);
                                }
                            }

                            @Override
                            public String getString() {
                                return null;
                            }

                            @Override
                            public int getProgress() {
                                return progress;
                            }

                            @Override
                            public String getLabelString() {
                                return label;
                            }
                        };

                        UIOManager.I().show(null, new ProgressDialog(pg, 0, "INstall Extension", "INstall Extension\r\now2", new AbstractIcon(IconKey.ICON_EXTENSIONMANAGER, 32)) {
                            public java.awt.Dialog.ModalityType getModalityType() {
                                return ModalityType.MODELESS;
                            };

                            protected void updateText(javax.swing.JProgressBar bar, ProgressGetter getter) {
                                textField.setText(getter.getLabelString());

                            };

                            protected boolean isLabelEnabled() {
                                return false;
                            };
                        });

                        // UpdateController.getInstance().getHandler().installExtension(ids)
                        System.out.println("");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();

        }
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

        @Override
        public void onUpdaterStatusUpdate(String label, Icon icon, double progress) {
        }
    }

    public AbstractButton createButton() {

        Button bt = new Button();

        return bt;
    }

}
