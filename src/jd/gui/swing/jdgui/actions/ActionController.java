//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.actions;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.Action;

import jd.HostPluginWrapper;
import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.ClipboardHandler;
import jd.controlling.DownloadController;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDController;
import jd.controlling.LinkGrabberController;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ReconnecterEvent;
import jd.event.ControlEvent;
import jd.event.ControlIDListener;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.dialog.AccountDialog;
import jd.gui.swing.jdgui.components.premiumbar.PremiumStatus;
import jd.gui.swing.jdgui.views.downloads.DownloadLinksPanel;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
import jd.gui.swing.jdgui.views.settings.JDLabelListRenderer;
import jd.gui.swing.jdgui.views.settings.panels.addons.ConfigPanelAddons;
import jd.gui.swing.jdgui.views.settings.panels.passwords.PasswordList;
import jd.nutils.JDFlags;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;

import org.appwork.storage.StorageValueChangeEvent;
import org.appwork.utils.event.DefaultEventListener;

/**
 * Class to control toolbar actions
 * 
 * @author Coalado
 */
public class ActionController {
    public static final String              JDL_PREFIX          = "jd.gui.swing.jdgui.actions.ActionController.";
    private static ArrayList<ToolBarAction> TOOLBAR_ACTION_LIST = new ArrayList<ToolBarAction>();

    /**
     * returns a fresh copy of all toolbaractions
     * 
     * @return
     */
    public static ArrayList<ToolBarAction> getActions() {
        final ArrayList<ToolBarAction> ret = new ArrayList<ToolBarAction>();
        synchronized (ActionController.TOOLBAR_ACTION_LIST) {
            ret.addAll(ActionController.TOOLBAR_ACTION_LIST);
        }
        return ret;

    }

    /**
     * Returns the action for the givven key
     * 
     * @param keyid
     * @return
     */
    public static ToolBarAction getToolBarAction(final String keyid) {
        synchronized (ActionController.TOOLBAR_ACTION_LIST) {
            for (final ToolBarAction a : ActionController.TOOLBAR_ACTION_LIST) {
                if (a.getID().equals(keyid)) { return a; }
            }
            return null;
        }
    }

    /**
     * Defines all possible actions
     */
    public static void initActions() {

        new ToolBarAction("toolbar.separator", "-") {
            private static final long serialVersionUID = -4628452328096482738L;

            @Override
            public void initDefaults() {
                this.setType(ToolBarAction.Types.SEPARATOR);
            }

            @Override
            public void onAction(final ActionEvent e) {
            }

        };

        new ThreadedAction("toolbar.control.start", "gui.images.next") {
            private static final long serialVersionUID = 1683169623090750199L;

            @Override
            public void initAction() {
                JDController.getInstance().addControlListener(new ControlIDListener(ControlEvent.CONTROL_DOWNLOAD_START, ControlEvent.CONTROL_DOWNLOAD_STOP) {
                    @Override
                    public void controlIDEvent(final ControlEvent event) {
                        switch (event.getEventID()) {
                        case ControlEvent.CONTROL_DOWNLOAD_START:
                            setEnabled(false);
                            break;
                        case ControlEvent.CONTROL_DOWNLOAD_STOP:
                            setEnabled(true);
                            break;
                        }
                    }
                });
            }

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) {
                    ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>(LinkGrabberController.getInstance().getPackages());
                    synchronized (LinkGrabberController.ControllerLock) {
                        synchronized (LinkGrabberPanel.getLinkGrabber()) {
                            for (final LinkGrabberFilePackage fp : fps) {
                                LinkGrabberPanel.getLinkGrabber().confirmPackage(fp, null, -1);
                            }
                        }
                    }
                    fps = null;
                    UserIF.getInstance().requestPanel(UserIF.Panels.DOWNLOADLIST, null);
                }
                DownloadWatchDog.getInstance().startDownloads();
            }

        };
        new ToolBarAction("toolbar.control.pause", "gui.images.break") {
            private static final long serialVersionUID = 7153300370492212502L;

            @Override
            public void initAction() {
                JDController.getInstance().addControlListener(new ControlIDListener(ControlEvent.CONTROL_DOWNLOAD_START, ControlEvent.CONTROL_DOWNLOAD_STOP) {
                    @Override
                    public void controlIDEvent(final ControlEvent event) {
                        switch (event.getEventID()) {
                        case ControlEvent.CONTROL_DOWNLOAD_START:
                            setEnabled(true);
                            setSelected(false);
                            break;
                        case ControlEvent.CONTROL_DOWNLOAD_STOP:
                            setEnabled(false);
                            setSelected(false);
                            break;
                        }
                    }
                });
                JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED) {
                    @Override
                    public void onPropertyChanged(final Property source, final String key) {
                        setToolTipText(JDL.LF("gui.menu.action.break2.desc", "Pause downloads. Limits global speed to %s KiB/s", SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10) + ""));
                    }
                });
            }

            @Override
            public void initDefaults() {
                this.setEnabled(false);
                this.setType(ToolBarAction.Types.TOGGLE);
                this.setToolTipText(JDL.LF("gui.menu.action.break2.desc", "Pause downloads. Limits global speed to %s KiB/s", SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10) + ""));
            }

            @Override
            public void onAction(final ActionEvent e) {
                final boolean b = ActionController.getToolBarAction("toolbar.control.pause").isSelected();
                DownloadWatchDog.getInstance().pauseDownloads(b);
            }

        };

        new ThreadedAction("toolbar.control.stop", "gui.images.stop") {
            private static final long serialVersionUID = 1409143759105090751L;

            @Override
            public void initAction() {
                JDController.getInstance().addControlListener(new ControlIDListener(ControlEvent.CONTROL_DOWNLOAD_START, ControlEvent.CONTROL_DOWNLOAD_STOP) {
                    @Override
                    public void controlIDEvent(final ControlEvent event) {
                        switch (event.getEventID()) {
                        case ControlEvent.CONTROL_DOWNLOAD_START:
                            setEnabled(true);
                            break;
                        case ControlEvent.CONTROL_DOWNLOAD_STOP:
                            setEnabled(false);
                            break;
                        }
                    }
                });
            }

            @Override
            public void initDefaults() {
                this.setEnabled(false);
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                final ProgressController pc = new ProgressController(JDL.L("gui.downloadstop", "Stopping current downloads..."), null);
                final Thread test = new Thread() {
                    @Override
                    public void run() {
                        while (true) {
                            pc.increase(1);
                            try {
                                Thread.sleep(1000);
                            } catch (final InterruptedException e) {
                                break;
                            }
                            if (DownloadWatchDog.getInstance().getDownloadStatus() == DownloadWatchDog.STATE.NOT_RUNNING) {
                                break;
                            }
                        }
                    }
                };
                test.start();
                DownloadWatchDog.getInstance().stopDownloads();
                test.interrupt();
                pc.doFinalize();
            }

        };

        new ThreadedAction("toolbar.interaction.reconnect", "gui.images.reconnect") {
            private static final long serialVersionUID = -1295253607970814759L;

            @Override
            public void initAction() {
                Reconnecter.getInstance().getEventSender().addListener(new DefaultEventListener<ReconnecterEvent>() {
                    // TODO: test
                    public void onEvent(final ReconnecterEvent event) {
                        if (event.getEventID() == ReconnecterEvent.SETTINGS_CHANGED) {
                            final StorageValueChangeEvent<?> storageEvent = (StorageValueChangeEvent<?>) event.getParameter();
                            if (storageEvent.getKey() == Reconnecter.RECONNECT_FAILED_COUNTER) {

                                if ((Integer) storageEvent.getNewValue() > 5) {
                                    setIcon("gui.images.reconnect_warning");
                                    setToolTipText(JDL.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
                                    ActionController.getToolBarAction("toolbar.quickconfig.reconnecttoggle").setToolTipText(JDL.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
                                } else {
                                    setToolTipText(JDL.L("gui.menu.action.reconnectman.desc", "Manual reconnect. Get a new IP by resetting your internet connection"));
                                    setIcon("gui.images.reconnect");
                                    ActionController.getToolBarAction("toolbar.quickconfig.reconnecttoggle").setToolTipText(JDL.L("gui.menu.action.reconnectauto.desc", "Auto reconnect. Get a new IP by resetting your internet connection"));
                                }
                            }

                        }
                    }

                });

            }

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0, JDL.L("gui.reconnect.confirm", "Do you want to reconnect your internet connection?")), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                    new Thread(new Runnable() {
                        public void run() {
                            Reconnecter.getInstance().forceReconnect();
                        }
                    }).start();
                }
            }

        };

        new ThreadedAction("toolbar.interaction.update", "gui.images.update") {
            private static final long serialVersionUID = 4359802245569811800L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                WebUpdate.doUpdateCheck(true);
            }

        };

        new ToolBarAction("toolbar.quickconfig.clipboardoberserver", "gui.images.clipboard_enabled") {
            private static final long serialVersionUID = -6442494647304101403L;

            @Override
            public void initAction() {
                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(final PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == Action.SELECTED_KEY) {
                            setIcon((Boolean) evt.getNewValue() ? "gui.images.clipboard_enabled" : "gui.images.clipboard_disabled");
                        }
                    }
                });

                JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE) {
                    @Override
                    public void onPropertyChanged(final Property source, final String key) {
                        setSelected(source.getBooleanProperty(key, true));
                    }
                });
            }

            @Override
            public void initDefaults() {
                this.setType(ToolBarAction.Types.TOGGLE);
                final boolean b = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true);
                this.setSelected(b);
                this.setIcon(b ? "gui.images.clipboard_enabled" : "gui.images.clipboard_disabled");
            }

            @Override
            public void onAction(final ActionEvent e) {
                ClipboardHandler.getClipboard().setEnabled(this.isSelected());
            }
        };

        new ToolBarAction("toolbar.quickconfig.reconnecttoggle", "gui.images.reconnect_disabled") {
            private static final long serialVersionUID = -2942320816429047941L;

            @Override
            public void initAction() {
                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(final PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == Action.SELECTED_KEY) {
                            setIcon((Boolean) evt.getNewValue() ? "gui.images.reconnect_enabled" : "gui.images.reconnect_disabled");
                        }
                    }
                });

                JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_ALLOW_RECONNECT) {
                    @Override
                    public void onPropertyChanged(final Property source, final String key) {
                        setSelected(source.getBooleanProperty(key, true));
                    }
                });
            }

            @Override
            public void initDefaults() {
                this.setType(ToolBarAction.Types.TOGGLE);
                final boolean b = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true);
                this.setSelected(b);
                this.setIcon(b ? "gui.images.reconnect_enabled" : "gui.images.reconnect_disabled");
            }

            @Override
            public void onAction(final ActionEvent e) {
                Reconnecter.getInstance().setAutoReconnectEnabled(!Reconnecter.getInstance().isAutoReconnectEnabled());
            }

        };

        new ToolBarAction("action.opendlfolder", "gui.images.package_opened") {
            private static final long serialVersionUID = -60944746807335951L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void onAction(final ActionEvent e) {
                final String dlDir = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, null);
                if (dlDir == null) { return; }
                JDUtilities.openExplorer(new File(dlDir));
            }

        };

        new ThreadedAction("toolbar.control.stopmark", "gui.images.stopmark.disabled") {
            private static final long serialVersionUID = 4359802245569811800L;

            @Override
            protected void initAction() {
                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(final PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == Action.SELECTED_KEY) {
                            setIcon((Boolean) evt.getNewValue() ? "gui.images.stopmark.enabled" : "gui.images.stopmark.disabled");
                        }
                    }
                });
                JDController.getInstance().addControlListener(new ControlIDListener(ControlEvent.CONTROL_DOWNLOAD_START, ControlEvent.CONTROL_DOWNLOAD_STOP) {
                    @Override
                    public void controlIDEvent(final ControlEvent event) {
                        switch (event.getEventID()) {
                        case ControlEvent.CONTROL_DOWNLOAD_START:
                            setEnabled(true);
                            break;
                        case ControlEvent.CONTROL_DOWNLOAD_STOP:
                            setEnabled(false);
                            break;
                        }
                    }
                });
            }

            @Override
            public void initDefaults() {
                this.setToolTipText(JDL.L(ActionController.JDL_PREFIX + "toolbar.control.stopmark.tooltip", "Stop after current Downloads"));
                this.setEnabled(false);
                this.setType(ToolBarAction.Types.TOGGLE);
                this.setSelected(false);
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                if (DownloadWatchDog.getInstance().isStopMarkSet()) {
                    DownloadWatchDog.getInstance().setStopMark(null);
                } else if (DownloadWatchDog.getInstance().getActiveDownloads() > 0) {
                    final Object obj = DownloadWatchDog.getInstance().getRunningDownloads().get(0);
                    DownloadWatchDog.getInstance().setStopMark(obj);
                } else {
                    this.setSelected(false);
                }
                if (DownloadWatchDog.getInstance().getDownloadStatus() != DownloadWatchDog.STATE.RUNNING && !DownloadWatchDog.getInstance().isStopMarkSet()) {
                    this.setEnabled(false);
                }
            }

        };

        new ThreadedAction("action.downloadview.movetobottom", "gui.images.go_bottom") {
            private static final long serialVersionUID = 6181260839200699153L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) {
                    LinkGrabberPanel.getLinkGrabber().move(LinkGrabberController.MOVE_BOTTOM);
                    LinkGrabberController.getInstance().throwRefresh();
                } else if (!DownloadLinksPanel.getDownloadLinksPanel().isNotVisible()) {
                    DownloadLinksPanel.getDownloadLinksPanel().move(DownloadController.MOVE_BOTTOM);
                }
            }
        };
        new ThreadedAction("action.downloadview.movetotop", "gui.images.go_top") {
            private static final long serialVersionUID = 6181260839200699153L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) {
                    LinkGrabberPanel.getLinkGrabber().move(LinkGrabberController.MOVE_TOP);
                    LinkGrabberController.getInstance().throwRefresh();
                } else if (!DownloadLinksPanel.getDownloadLinksPanel().isNotVisible()) {
                    DownloadLinksPanel.getDownloadLinksPanel().move(DownloadController.MOVE_TOP);
                }
            }
        };

        new ThreadedAction("action.downloadview.moveup", "gui.images.up") {
            private static final long serialVersionUID = 6181260839200699153L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) {
                    LinkGrabberPanel.getLinkGrabber().move(LinkGrabberController.MOVE_UP);
                    LinkGrabberController.getInstance().throwRefresh();
                } else if (!DownloadLinksPanel.getDownloadLinksPanel().isNotVisible()) {
                    DownloadLinksPanel.getDownloadLinksPanel().move(DownloadController.MOVE_UP);
                }
            }
        };
        new ThreadedAction("action.downloadview.movedown", "gui.images.down") {
            private static final long serialVersionUID = 6181260839200699153L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) {
                    LinkGrabberPanel.getLinkGrabber().move(LinkGrabberController.MOVE_DOWN);
                    LinkGrabberController.getInstance().throwRefresh();
                } else if (!DownloadLinksPanel.getDownloadLinksPanel().isNotVisible()) {
                    DownloadLinksPanel.getDownloadLinksPanel().move(DownloadController.MOVE_DOWN);
                }
            }
        };

        new ThreadedAction("action.premiumview.addacc", "gui.images.newlogins") {

            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                if (e.getSource() instanceof PluginForHost) {
                    AccountDialog.showDialog((PluginForHost) e.getSource());
                } else {
                    AccountDialog.showDialog(null);
                }
            }
        };
        new ThreadedAction("action.premium.buy", "gui.images.buy") {

            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                int index = 0;
                final ArrayList<HostPluginWrapper> plugins = JDUtilities.getPremiumPluginsForHost();
                Collections.sort(plugins);
                final HostPluginWrapper[] data = plugins.toArray(new HostPluginWrapper[plugins.size()]);
                if (e.getSource() instanceof HostPluginWrapper) {
                    for (int i = 0; i < data.length; i++) {
                        final HostPluginWrapper w = data[i];
                        if (e.getSource() == w) {
                            index = i;
                            break;
                        }
                    }
                }
                final int i = index;
                final int selection = new GuiRunnable<Integer>() {

                    @Override
                    public Integer runSave() {
                        return UserIO.getInstance().requestComboDialog(UserIO.NO_COUNTDOWN, JDL.L(ActionController.JDL_PREFIX + "buy.title", "Buy Premium"), JDL.L(ActionController.JDL_PREFIX + "buy.message", "Which hoster are you interested in?"), data, i, null, JDL.L(ActionController.JDL_PREFIX + "continue", "Continue"), null, new JDLabelListRenderer());
                    }
                }.getReturnValue();
                if (selection < 0) { return; }

                try {
                    JLink.openURL(data[selection].getPlugin().getBuyPremiumUrl());
                } catch (final Exception ex) {
                }
            }
        };

        new ToolBarAction("addonsMenu.configuration", "gui.images.config.packagemanager") {
            private static final long serialVersionUID = -3613887193435347389L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void onAction(final ActionEvent e) {
                SwingGui.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, ConfigPanelAddons.class);
            }
        };
        new ToolBarAction("premiumMenu.toggle", "gui.images.premium_enabled") {

            private static final long serialVersionUID = 4276436625882302179L;

            @Override
            public void initDefaults() {
                this.setType(ToolBarAction.Types.TOGGLE);
                final boolean b = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);
                this.setSelected(b);
                this.setIcon(b ? "gui.images.premium_enabled" : "gui.images.premium_disabled");

                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(final PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == Action.SELECTED_KEY) {
                            setIcon((Boolean) evt.getNewValue() ? "gui.images.premium_enabled" : "gui.images.premium_disabled");
                        }
                    }
                });
                JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_USE_GLOBAL_PREMIUM) {
                    @Override
                    public void onPropertyChanged(final Property source, final String key) {
                        final boolean b = source.getBooleanProperty(key, true);
                        setSelected(b);
                        PremiumStatus.getInstance().updateGUI(b);
                    }
                });
            }

            @Override
            public void onAction(final ActionEvent e) {
                if (!this.isSelected()) {
                    final int answer = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("dialogs.premiumstatus.global.title", "Disable Premium?"), JDL.L("dialogs.premiumstatus.global.message", "Do you really want to disable all premium accounts?"), UserIO.getInstance().getIcon(UserIO.ICON_WARNING), JDL.L("gui.btn_yes", "Yes"), JDL.L("gui.btn_no", "No"));
                    if (JDFlags.hasAllFlags(answer, UserIO.RETURN_CANCEL)) {
                        this.setSelected(true);
                        return;
                    }
                }
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, this.isSelected());
                JDUtilities.getConfiguration().save();
            }

        };
        new ToolBarAction("premiumMenu.configuration", "gui.images.config.premium") {
            private static final long serialVersionUID = -3613887193435347389L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void onAction(final ActionEvent e) {
                SwingGui.getInstance().requestPanel(UserIF.Panels.PREMIUMCONFIG, null);
            }
        };

        new ToolBarAction("action.passwordlist", PasswordList.getIconKey()) {
            private static final long serialVersionUID = -4111402172655120550L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void onAction(final ActionEvent e) {
                SwingGui.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, PasswordList.class);
            }
        };
    }

    public static void register(final ToolBarAction action) {
        synchronized (ActionController.TOOLBAR_ACTION_LIST) {
            if (ActionController.TOOLBAR_ACTION_LIST.contains(action)) { return; }
            for (final ToolBarAction act : ActionController.TOOLBAR_ACTION_LIST) {
                if (act.getID().equalsIgnoreCase(action.getID())) { return; }
            }
            ActionController.TOOLBAR_ACTION_LIST.add(action);
        }
    }

    public static void unRegister(final ToolBarAction action) {
        synchronized (ActionController.TOOLBAR_ACTION_LIST) {
            if (!ActionController.TOOLBAR_ACTION_LIST.contains(action)) { return; }

            ActionController.TOOLBAR_ACTION_LIST.remove(action);
        }
    }

}
