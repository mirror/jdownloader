package jd.gui.skins.jdgui.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.ClipboardHandler;
import jd.controlling.JDController;
import jd.controlling.LinkGrabberController;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.event.ControlIDListener;
import jd.gui.UIConstants;
import jd.gui.UserIO;
import jd.gui.skins.SwingGui;
import jd.gui.skins.jdgui.components.linkgrabberview.LinkGrabberFilePackage;
import jd.gui.skins.jdgui.components.linkgrabberview.LinkGrabberPanel;
import jd.gui.skins.simple.GuiRunnable;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;

/**
 * Class to control toolbar actions
 * 
 * @author Coalado
 * 
 */
public class ActionController {
    public static final String JDL_PREFIX = "jd.gui.skins.jdgui.actions.ActionController.";
    private static ArrayList<ToolBarAction> TOOLBAR_ACTION_LIST = new ArrayList<ToolBarAction>();

    public static void register(ToolBarAction action) {
        synchronized (TOOLBAR_ACTION_LIST) {
            for (ToolBarAction tmp : TOOLBAR_ACTION_LIST) {
                if (tmp.getID().equalsIgnoreCase(action.getID())) {
                    TOOLBAR_ACTION_LIST.remove(tmp);
                    break;
                }
            }
            if (!TOOLBAR_ACTION_LIST.contains(action)) TOOLBAR_ACTION_LIST.add(action);
        }
    }

    /**
     * Defines all possible actions
     */
    public static void initActions() {

        new ToolBarAction("toolbar.separator", null) {
            public void actionPerformed(ActionEvent e) {
            }

            @Override
            public void initDefaults() {
                this.type = ToolBarAction.Types.SEPARATOR;

            }

            @Override
            public void init() {

            }

        };

        new ToolBarAction("toolbar.control.start", "gui.images.next") {
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    public void run() {
                        if (LinkGrabberPanel.getLinkGrabber().isVisible()) {
                            ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>(LinkGrabberController.getInstance().getPackages());
                            synchronized (LinkGrabberController.ControllerLock) {
                                synchronized (LinkGrabberPanel.getLinkGrabber()) {
                                    for (LinkGrabberFilePackage fp : fps) {
                                        LinkGrabberPanel.getLinkGrabber().confirmPackage(fp, null, -1);
                                    }
                                }
                            }
                            fps = null;
                            SwingGui.getInstance().requestPanel(UIConstants.PANEL_ID_DOWNLOADLIST);

                        }
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                ActionController.getToolBarAction("toolbar.control.pause").setSelected(false);
                                JDUtilities.getController().pauseDownloads(false);
                                return null;
                            }
                        }.waitForEDT();
                        JDUtilities.getController().startDownloads();
                    }
                }.start();
            }

            @Override
            public void initDefaults() {
                setPriority(1000);
                this.setEnabled(true);
                this.type = ToolBarAction.Types.NORMAL;
                this.setToolTipText(JDL.L(JDL_PREFIX + ".toolbar.control.start.tooltip", "Start downloads in list"));
            }

            @Override
            public void init() {
                JDUtilities.getController().addControlListener(new ControlIDListener(ControlEvent.CONTROL_DOWNLOAD_START, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, ControlEvent.CONTROL_DOWNLOAD_STOP) {
                    public void controlIDEvent(ControlEvent event) {
                        switch (event.getID()) {
                        case ControlEvent.CONTROL_DOWNLOAD_START:

                            setEnabled(false);

                            break;
                        case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                        case ControlEvent.CONTROL_DOWNLOAD_STOP:
                            setEnabled(true);

                            break;
                        }
                    }
                });
            }

        };
        new ToolBarAction("toolbar.control.pause", "gui.images.break") {

            public void actionPerformed(ActionEvent e) {

                boolean b = !ActionController.getToolBarAction("toolbar.control.pause").isSelected();
                ActionController.getToolBarAction("toolbar.control.pause").setSelected(b);
                JDUtilities.getController().pauseDownloads(b);

            }

            @Override
            public void initDefaults() {
                setPriority(999);
                this.setEnabled(false);
                this.type = ToolBarAction.Types.TOGGLE;
                this.setToolTipText(JDL.L(JDL_PREFIX + ".toolbar.control.pause.tooltip", "Pause active transfer (decrease speed to 10 kb/s)"));
            }

            @Override
            public void init() {
                JDUtilities.getController().addControlListener(new ControlIDListener(ControlEvent.CONTROL_DOWNLOAD_START, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, ControlEvent.CONTROL_DOWNLOAD_STOP) {
                    public void controlIDEvent(ControlEvent event) {
                        switch (event.getID()) {
                        case ControlEvent.CONTROL_DOWNLOAD_START:

                            setEnabled(true);
                            setSelected(false);

                            break;
                        case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                        case ControlEvent.CONTROL_DOWNLOAD_STOP:
                            setEnabled(false);
                            setSelected(false);

                            break;
                        }
                    }
                });
                //
                // JDController.getInstance().addControlListener(new
                // ConfigPropertyListener(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED)
                // {
                // @Override
                // public void onPropertyChanged(Property source, final String
                // key) {
                // if (source.getBooleanProperty(key, false)) {
                // setToolTipText(JDL.LF("gui.menu.action.break2.desc",
                // "Pause downloads. Limits global speed to %s kb/s",
                // SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED,
                // 10) + ""));
                // } else {
                // setToolTipText(JDL.L("gui.menu.action.pause.desc", null));
                // }
                // }
                // });
            }

        };

        new ToolBarAction("toolbar.control.stop", "gui.images.stop") {

            public void actionPerformed(ActionEvent e) {

                new Thread() {
                    public void run() {
                        ActionController.getToolBarAction("toolbar.control.pause").setSelected(false);
                        JDUtilities.getController().pauseDownloads(false);
                        final ProgressController pc = new ProgressController(JDL.L("gui.downloadstop", "Stopping current downloads..."));
                        Thread test = new Thread() {
                            public void run() {
                                while (true) {
                                    pc.increase(1);
                                    try {
                                        sleep(1000);
                                    } catch (InterruptedException e) {
                                        break;
                                    }
                                }
                            }
                        };
                        test.start();
                        JDUtilities.getController().stopDownloads();
                        test.interrupt();
                        pc.finalize();
                    }
                }.start();
            }

            @Override
            public void initDefaults() {
                setPriority(998);
                this.setEnabled(false);
                this.setToolTipText(JDL.L(JDL_PREFIX + ".toolbar.control.stop.tooltip", "Stop all running downloads"));
            }

            @Override
            public void init() {
                JDUtilities.getController().addControlListener(new ControlIDListener(ControlEvent.CONTROL_DOWNLOAD_START, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, ControlEvent.CONTROL_DOWNLOAD_STOP) {
                    public void controlIDEvent(ControlEvent event) {
                        switch (event.getID()) {
                        case ControlEvent.CONTROL_DOWNLOAD_START:
                            setEnabled(true);
                            break;
                        case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                        case ControlEvent.CONTROL_DOWNLOAD_STOP:
                            setEnabled(false);
                            break;
                        }
                    }
                });

            }

        };

        new ToolBarAction("toolbar.interaction.reconnect", "gui.images.reconnect") {
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    public void run() {
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                if (UserIO.RETURN_OK == UserIO.getInstance().requestConfirmDialog(0, JDL.L("gui.reconnect.confirm", "Wollen Sie sicher eine neue Verbindung aufbauen?"))) {
                                    new Thread(new Runnable() {
                                        public void run() {
                                            Reconnecter.doManualReconnect();
                                        }
                                    }).start();
                                }
                                return null;
                            }
                        }.start();

                    }
                }.start();
            }

            @Override
            public void initDefaults() {
                setPriority(800);
                this.setEnabled(true);

                this.setToolTipText(JDL.L(JDL_PREFIX + ".toolbar.interaction.reconnect.tooltip", "Get a new IP be resetting your internet connection"));
            }

            @Override
            public void init() {

                JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_LATEST_RECONNECT_RESULT) {
                    @Override
                    public void onPropertyChanged(Property source, final String key) {
                        if (!source.getBooleanProperty(key, true)) {
                            setIcon("gui.images.reconnect_warning");
                            setToolTipText(JDL.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
                            getToolBarAction("toolbar.quickconfig.reconnecttoggle").setToolTipText(JDL.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
                        } else {
                            setToolTipText(JDL.L("gui.menu.action.reconnectman.desc", "Manual reconnect. Get a new IP by resetting your internet connection"));
                            setIcon("gui.images.reconnect");
                            getToolBarAction("toolbar.quickconfig.reconnecttoggle").setToolTipText(JDL.L("gui.menu.action.reconnectauto.desc", "Auto reconnect. Get a new IP by resetting your internet connection"));
                        }
                    }
                });
            }

        };

        new ToolBarAction("toolbar.interaction.update", "gui.images.update") {
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    public void run() {
                        new Thread() {
                            public void run() {
                                new WebUpdate().doUpdateCheck(true, true);
                            }
                        }.start();
                    }
                }.start();
            }

            @Override
            public void initDefaults() {
                setPriority(800);
                this.setEnabled(true);
                this.setToolTipText(JDL.L(JDL_PREFIX + ".toolbar.interaction.update.tooltip", "Check for new updates"));
            }

            @Override
            public void init() {

            }

        };

        new ToolBarAction("toolbar.quickconfig.clipboardoberserver", "gui.images.clipboard_enabled") {
            public void actionPerformed(ActionEvent e) {
                ClipboardHandler.getClipboard().setEnabled(!this.isSelected());
            }

            @Override
            public void initDefaults() {
                setPriority(900);
                this.setEnabled(true);
                this.type = ToolBarAction.Types.TOGGLE;
                this.setToolTipText(JDL.L("gui.menu.action.clipboard.desc", null));
            }

            @Override
            public void init() {
                setSelected(JDUtilities.getConfiguration().getGenericProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true));
                JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE) {
                    @Override
                    public void onPropertyChanged(Property source, final String key) {
                        if (source.getBooleanProperty(key, true)) {
                            setSelected(true);
                            setIcon("gui.images.clipboard_enabled");
                        } else {
                            setSelected(false);
                            setIcon("gui.images.clipboard_disabled");
                        }
                    }
                });
            }
        };

        new ToolBarAction("toolbar.quickconfig.reconnecttoggle", "gui.images.reconnect_disabled") {
            public void actionPerformed(ActionEvent e) {
                Reconnecter.toggleReconnect();
            }

            @Override
            public void initDefaults() {
                setPriority(899);
                this.setEnabled(true);
                this.type = ToolBarAction.Types.TOGGLE;
                this.setToolTipText(JDL.L("gui.menu.action.reconnect.desc", null));
            }

            @Override
            public void init() {
                setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true));
                JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_ALLOW_RECONNECT) {
                    @Override
                    public void onPropertyChanged(Property source, final String key) {
                        if (source.getBooleanProperty(key, true)) {

                            setSelected(true);
                            setIcon("gui.images.reconnect_enabled");
                        } else {
                            setSelected(false);
                            setIcon("gui.images.reconnect_disabled");
                        }
                    }
                });
            }

        };  

        

    }

    /**
     * Returns the action for the givven key
     * 
     * @param keyid
     * @return
     */
    public static ToolBarAction getToolBarAction(String keyid) {
        synchronized (TOOLBAR_ACTION_LIST) {
            for (ToolBarAction a : TOOLBAR_ACTION_LIST) {
                if (a.getID().equals(keyid)) return a;

            }
            return null;
        }
    }

}
