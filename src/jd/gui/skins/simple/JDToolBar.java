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

package jd.gui.skins.simple;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.ClipboardHandler;
import jd.controlling.JDController;
import jd.controlling.LinkGrabberController;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.jdgui.components.linkgrabberview.LinkGrabberFilePackage;
import jd.gui.skins.jdgui.components.linkgrabberview.LinkGrabberPanel;
import jd.gui.skins.simple.components.SpeedMeterPanel;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class JDToolBar extends JToolBar implements ControlListener {

    private static final long serialVersionUID = 7533138014274040205L;

    private static final String BUTTON_CONSTRAINTS = "gaptop 2, gapleft 2";

    private JButton playButton;
    private JToggleButton pauseButton;
    private JButton stopButton;
    private JToggleButton clipboard;
    private JToggleButton reconnect;

    private SpeedMeterPanel speedmeter;

//    private boolean noTitlePainter;

    private JButton reconnectButton;

    private JButton update;

    public static final int ENTRY_PAUSE = 1 << 0;
    public static final int ENTRY_RECONNECT = 1 << 1;
    public static final int ENTRY_STOP = 1 << 2;
    public static final int ENTRY_START = 1 << 3;
    public static final int ENTRY_CLIPBOARD = 1 << 4;
    public static final int ENTRY_UPDATE = 1 << 5;
    public static final int ENTRY_MAN_RECONNECT = 1 << 6;

    public static final int ENTRY_CONTROL = ENTRY_PAUSE | ENTRY_START | ENTRY_STOP;
    public static final int ENTRY_CONFIG = ENTRY_CLIPBOARD | ENTRY_RECONNECT;
    public static final int ENTRY_INTERACTION = ENTRY_UPDATE | ENTRY_MAN_RECONNECT;

    public static final int ENTRY_ALL = ENTRY_CONTROL | ENTRY_CONFIG | ENTRY_INTERACTION;

    public JDToolBar() {
        super(JToolBar.HORIZONTAL);

//        noTitlePainter = noTitlePane;

        JDUtilities.getController().addControlListener(this);

        setRollover(true);
        setFloatable(false);
        setLayout(new MigLayout("ins 0,gap 0", "[][][][][][][][][][][][][][grow,fill]"));

//        JSeparator sep;
//        if (noTitlePainter) {
//            add(sep = new JSeparator(JSeparator.VERTICAL), "gapleft 30,height 0,gapright 5");
//        } else {
//            add(sep = new JSeparator(JSeparator.VERTICAL), "gapleft 46,height 0,gapright 5");
//        }
//        sep.setVisible(false);
        initController();
        add(new JSeparator(JSeparator.VERTICAL), "height 32,gapleft 10,gapright 10");
        initQuickConfig();
        add(new JSeparator(JSeparator.VERTICAL), "height 32,gapleft 10,gapright 10");
        initInteractions();
        addSpeedMeter();

        updateReconnectButtons();
        updateClipboardButton();
        updateReconnectButtonIcon();

        initListeners();
    }

    /**
     * TODO: Fixen! Bis dahin ohne Funktion
     */
    public void setEnabled(int flag, boolean b, String tt) {
        if (true) return;

        if ((flag & JDToolBar.ENTRY_CLIPBOARD) > 0) {
            clipboard.setEnabled(b);
            clipboard.setToolTipText(tt);
        }
        if ((flag & JDToolBar.ENTRY_MAN_RECONNECT) > 0) {
            reconnectButton.setEnabled(b);
            reconnectButton.setToolTipText(tt);
        }
        if ((flag & JDToolBar.ENTRY_PAUSE) > 0) {
            pauseButton.setEnabled(b);
            pauseButton.setToolTipText(tt);
        }
        if ((flag & JDToolBar.ENTRY_RECONNECT) > 0) {
            reconnect.setEnabled(b);
            reconnect.setToolTipText(tt);
        }
        if ((flag & JDToolBar.ENTRY_START) > 0) {
            playButton.setEnabled(b);
            playButton.setToolTipText(tt);
        }
        if ((flag & JDToolBar.ENTRY_STOP) > 0) {
            stopButton.setEnabled(b);
            stopButton.setToolTipText(tt);
        }
        if ((flag & JDToolBar.ENTRY_UPDATE) > 0) {
            update.setEnabled(b);
            update.setToolTipText(tt);
        }
    }

    private void initListeners() {
        JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_LATEST_RECONNECT_RESULT, Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, Configuration.PARAM_ALLOW_RECONNECT, Configuration.PARAM_DOWNLOAD_PAUSE_SPEED) {
            @Override
            public void onPropertyChanged(Property source, final String key) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (key == Configuration.PARAM_LATEST_RECONNECT_RESULT) {
                            updateReconnectButtons();
                        } else if (key == Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE) {
                            updateClipboardButton();
                        } else if (key == Configuration.PARAM_ALLOW_RECONNECT) {
                            updateReconnectButtonIcon();
                        } else if (key == Configuration.PARAM_DOWNLOAD_PAUSE_SPEED) {
                            updatePauseButton();
                        }
                    }
                });
            }
        });
    }

    private void initInteractions() {
        add(reconnectButton = new JButton(JDTheme.II("gui.images.reconnect", 24, 24)), BUTTON_CONSTRAINTS);

        reconnectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    public void run() {
                        //TODO
//                        SwingGui.getInstance().doManualReconnect();
                    }
                }.start();
            }
        });

        add(update = new JButton(JDTheme.II("gui.images.update", 24, 24)), BUTTON_CONSTRAINTS);
        update.setToolTipText(JDL.L("gui.menu.action.update.desc", "Check for new updates"));
        update.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    public void run() {
                        new WebUpdate().doUpdateCheck(true, true);
                    }
                }.start();
            }
        });
    }

    private void initQuickConfig() {
        /* Clipboard */
        add(clipboard = new JToggleButton(), BUTTON_CONSTRAINTS);
        clipboard.setToolTipText(JDL.L("gui.menu.action.clipboard.desc", null));
        clipboard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ClipboardHandler.getClipboard().setEnabled(clipboard.isSelected());
            }
        });
        clipboard.setName(JDL.L("quickhelp.toolbar.clipboard", "Toolbar clipboard observer"));

        /* reconnect */
        add(reconnect = new JToggleButton(JDTheme.II("gui.images.reconnect_disabled", 24, 24)), BUTTON_CONSTRAINTS);
        reconnect.setName(JDL.L("quickhelp.toolbar.reconnect", "Reconnect Toolbar"));
        reconnect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Reconnecter.toggleReconnect();
            }

        });
    }

    private void updateClipboardButton() {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true)) {
            clipboard.setSelected(true);
            clipboard.setIcon(JDTheme.II("gui.images.clipboard_enabled", 24, 24));
        } else {
            clipboard.setSelected(false);
            clipboard.setIcon(JDTheme.II("gui.images.clipboard_disabled", 24, 24));
        }
    }

    private void updateReconnectButtons() {
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_LATEST_RECONNECT_RESULT, true)) {
            reconnectButton.setIcon(JDTheme.II("gui.images.reconnect_warning", 24, 24));
            reconnectButton.setToolTipText(JDL.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
            reconnect.setEnabled(true);
            reconnect.setToolTipText(JDL.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
        } else {
            reconnectButton.setToolTipText(JDL.L("gui.menu.action.reconnectman.desc", "Manual reconnect. Get a new IP by resetting your internet connection"));
            reconnectButton.setIcon(JDTheme.II("gui.images.reconnect", 24, 24));
            reconnect.setEnabled(true);
            reconnect.setToolTipText(JDL.L("gui.menu.action.reconnectauto.desc", "Auto reconnect. Get a new IP by resetting your internet connection"));
        }
    }

    private void updateReconnectButtonIcon() {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
            reconnect.setSelected(true);
            reconnect.setIcon(JDTheme.II("gui.images.reconnect_enabled", 24, 24));
        } else {
            reconnect.setSelected(false);
            reconnect.setIcon(JDTheme.II("gui.images.reconnect_disabled", 24, 24));
        }
    }

    private void updatePauseButton() {
        pauseButton.setToolTipText(JDL.LF("gui.menu.action.break2.desc", "Pause downloads. Limits global speed to %s kb/s", SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10) + ""));
    }

    private void initController() {
        add(playButton = new JButton(JDTheme.II("gui.images.next", 24, 24)), BUTTON_CONSTRAINTS);
        playButton.setToolTipText(JDL.L("gui.menu.action.start.desc", null));
        playButton.setName("playButton");
        playButton.addActionListener(new ActionListener() {
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
                            //TODO
//                            SwingGui.getInstance().getTaskPane().switcher(SwingGui.getInstance().getDlTskPane());
                        }
                        setPause(false);
                        JDUtilities.getController().startDownloads();
                    }
                }.start();
            }
        });

        add(pauseButton = new JToggleButton(JDTheme.II("gui.images.break", 24, 24)), BUTTON_CONSTRAINTS);
        updatePauseButton();
        pauseButton.setName("pauseButton");
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setPause(pauseButton.isSelected());
            }
        });

        add(stopButton = new JButton(JDTheme.II("gui.images.stop", 24, 24)), BUTTON_CONSTRAINTS);
        stopButton.setToolTipText(JDL.L("gui.menu.action.stop.desc", null));
        stopButton.setName("stopButton");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    public void run() {
                        setPause(false);
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
        });
        stopButton.setEnabled(false);
        pauseButton.setEnabled(false);
        playButton.setEnabled(true);
    }

    private void addSpeedMeter() {
        speedmeter = new SpeedMeterPanel();
        add(speedmeter, "cell 0 13,dock east,hidemode 3,height 30!,width 30:200:300");
    }

    private void setPause(final boolean b) {
        new GuiRunnable<Object>() {
            public Object runSave() {
                pauseButton.setSelected(b);
                JDUtilities.getController().pauseDownloads(b);
                return null;
            }
        }.waitForEDT();
    }

    public void controlEvent(final ControlEvent event) {
        new GuiRunnable<Object>() {
            public Object runSave() {
                switch (event.getID()) {
                case ControlEvent.CONTROL_DOWNLOAD_START:
                    stopButton.setEnabled(true);
                    pauseButton.setEnabled(true);
                    playButton.setEnabled(false);
                    speedmeter.start();
                    break;
                case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                case ControlEvent.CONTROL_DOWNLOAD_STOP:
                    stopButton.setEnabled(false);
                    setPause(false);
                    pauseButton.setEnabled(false);
                    playButton.setEnabled(true);
                    speedmeter.stop();
                    break;
                }
                return null;
            }
        }.start();
    }
}