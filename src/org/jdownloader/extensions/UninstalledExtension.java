package org.jdownloader.extensions;

import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.JProgressBar;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.updatev2.InstallLog;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.updatev2.UpdaterListener;

public class UninstalledExtension {

    private String              iconKey;
    private String              name;
    private String              id;
    private String              description;
    private AbstractConfigPanel panel;

    public String getIconKey() {
        return iconKey;
    }

    public String getName() {
        return name;
    }

    public UninstalledExtension(String id, String iconKey, String name, String description) {
        this.iconKey = iconKey;
        this.name = name;
        this.id = id;
        this.description = description;

    }

    public SwitchPanel getPanel() {
        if (panel != null) {
            return panel;
        }
        panel = new AbstractConfigPanel() {

            private ExtButton    install;
            private JProgressBar progressbar;

            {

                final Header header = new Header(UninstalledExtension.this.getName(), getIcon());

                add(header, "spanx,growx,pushx");

                addDescription(UninstalledExtension.this.getDescription());
                progressbar = new JProgressBar();
                progressbar.setIndeterminate(true);
                progressbar.setString(_GUI._.UninstalledExtension_getPanel_install_in_progress());
                progressbar.setVisible(false);
                progressbar.setMaximum(100);
                progressbar.setStringPainted(true);
                install = new ExtButton(new AppAction() {
                    {
                        // UpdateController.getInstance().s
                        setName(_GUI._.UninstalledExtension_getPanel_());
                        setIconKey(IconKey.ICON_DOWNLOAD);
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        progressbar.setVisible(true);
                        ((ExtButton) e.getSource()).setVisible(false);
                        new Thread("Install Extension ") {
                            public void run() {
                                try {

                                    UpdateController.getInstance().setGuiVisible(true);

                                    UpdaterListener listener;
                                    UpdateController.getInstance().getEventSender().addListener(listener = new UpdaterListener() {

                                        @Override
                                        public void onUpdatesAvailable(boolean selfupdate, InstallLog installlog) {
                                            System.out.println();
                                        }

                                        @Override
                                        public void onUpdaterStatusUpdate(final String label, Icon icon, final double p) {
                                            System.out.println();
                                            new EDTRunner() {

                                                @Override
                                                protected void runInEDT() {

                                                    progressbar.setValue((int) (p));
                                                    progressbar.setIndeterminate(p <= 0);
                                                    progressbar.setString(label);
                                                }
                                            };
                                        }
                                    });
                                    UpdateController.getInstance().runExtensionInstallation(id);
                                    try {
                                        while (true) {
                                            Thread.sleep(500);
                                            if (!UpdateController.getInstance().isRunning()) {
                                                break;
                                            }

                                            UpdateController.getInstance().waitForUpdate();

                                        }
                                        // boolean installed = UpdateController.getInstance().isExtensionInstalled(id);
                                        final boolean pending = UpdateController.getInstance().hasPendingUpdates();
                                        // System.out.println(1);
                                        new EDTRunner() {

                                            @Override
                                            protected void runInEDT() {
                                                progressbar.setIndeterminate(false);
                                                if (pending) {
                                                    progressbar.setValue(100);
                                                    progressbar.setMaximum(100);
                                                    progressbar.setString(_GUI._.UninstalledExtension_waiting_for_restart());
                                                } else {
                                                    progressbar.setVisible(false);
                                                    install.setVisible(true);

                                                }
                                            }
                                        };
                                    } finally {
                                        UpdateController.getInstance().getEventSender().removeListener(listener);
                                    }

                                } catch (Exception e) {
                                    Log.exception(e);
                                } finally {

                                }
                            }
                        }.start();

                    }
                });
                add(install, "gapleft" + getLeftGap() + ",spanx,growx,pushx,hidemode 3");
                add(progressbar, "gapleft" + getLeftGap() + ",spanx,growx,pushx,hidemode 3,height n:24:n");
            }

            @Override
            public void updateContents() {
            }

            @Override
            public void save() {
            }

            @Override
            public String getTitle() {
                return getName();
            }

            @Override
            public Icon getIcon() {
                return UninstalledExtension.this.getIcon(32);
            }
        };
        return panel;
    }

    public Icon getIcon(int size) {
        return new AbstractIcon(iconKey, size);
    }

    //
    // @Override
    // public boolean _isEnabled() {
    // return false;
    // }

    // @Override
    public String getDescription() {
        return _GUI._.UninstalledExtension_getDescription_object_(description);
    }

}
