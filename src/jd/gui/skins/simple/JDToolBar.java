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

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.Main;
import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.ClipboardHandler;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.components.SpeedMeterPanel;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import net.miginfocom.swing.MigLayout;

public class JDToolBar extends JToolBar implements ControlListener {

    private static final long serialVersionUID = 7533138014274040205L;

    // public static final int DISPLAY = 28;

    // public static final int LEFTGAP = 2;

    // public static final int IMGSIZE = 54;

    private static final String BUTTON_CONSTRAINTS = "gaptop 2, gapleft 2";

    private JButton playButton;
    private JToggleButton pauseButton;
    private JButton stopButton;
    private int pause = -1;
    private JToggleButton clipboard;
    private JToggleButton reconnect;

    private SpeedMeterPanel speedmeter;

    private boolean noTitlePainter;

    private JButton reconnectButton;

    private JToggleButton update;

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

    public JDToolBar(boolean noTitlePane, Image mainMenuIcon) {
        super(JToolBar.HORIZONTAL);

        noTitlePainter = noTitlePane;

        JDUtilities.getController().addControlListener(this);

        setRollover(true);
        setFloatable(false);
        setLayout(new MigLayout("ins 0,gap 0", "[][][][][][][][][][][][][][grow,fill]"));

        JSeparator sep;
        if (noTitlePainter) {
            add(sep = new JSeparator(JSeparator.VERTICAL), "gapleft 30,height 0,gapright 5");
        } else {
            add(sep = new JSeparator(JSeparator.VERTICAL), "gapleft 46,height 0,gapright 5");
        }
        sep.setVisible(false);
        initController();
        add(new JSeparator(JSeparator.VERTICAL), "height 32,gapleft 10,gapright 10");
        initQuickConfig();
        add(new JSeparator(JSeparator.VERTICAL), "height 32,gapleft 10,gapright 10");
        initInteractions();
        addSpeedMeter();

        setPause(false);

        // MouseAreaListener ml = new MouseAreaListener(LEFTGAP, 0, IMGSIZE +
        // LEFTGAP, DISPLAY);
        // addMouseMotionListener(ml);
        // addMouseListener(ml);
    }

    public void setEnabled(int flag, boolean b, String tt) {
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
            if (Main.isBeta()) {
                update.setEnabled(false);
                update.setToolTipText("This is a BETA version. Updates for betaversions are only available at jdownloader.org");
            } else {
                update.setEnabled(b);
                update.setToolTipText(tt);
            }
        }

    }

    private void initInteractions() {
        add(reconnectButton = new JButton(JDTheme.II("gui.images.reconnect", 24, 24)), BUTTON_CONSTRAINTS);

        updateManReconnectButton();

        JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_LATEST_RECONNECT_RESULT) {

            @Override
            public void onPropertyChanged(Property source, String valid) {
                updateManReconnectButton();
            }

        });
        reconnectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SimpleGUI.CURRENTGUI.doManualReconnect();
            }

        });

        add(update = new JToggleButton(JDTheme.II("gui.images.update", 24, 24)), BUTTON_CONSTRAINTS);
        update.setToolTipText(JDLocale.L("gui.menu.action.update.desc", "Check for new updates"));

        if (Main.isBeta()) {
            update.setEnabled(false);
            update.setToolTipText("This is a BETA version. Updates for betaversions are only available at jdownloader.org");

        }
        update.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new WebUpdate().doWebupdate(false);
            }

        });

    }

    // public void paintComponent(Graphics g) {
    // super.paintComponent(g);
    // if (noTitlePainter) {
    // ((Graphics2D) g).drawImage(logo, LEFTGAP, 0, 32 + LEFTGAP, 32, 0, 0, 32,
    // 32, null);
    // } else {
    // ((Graphics2D) g).drawImage(logo, LEFTGAP, 0, IMGSIZE + LEFTGAP, DISPLAY,
    // 0, IMGSIZE - DISPLAY, IMGSIZE, IMGSIZE, null);
    // }
    // }

    private void initQuickConfig() {
        /* Clipboard */

        add(clipboard = new JToggleButton(JDTheme.II("gui.images.clipboard_disabled", 24, 24)), BUTTON_CONSTRAINTS);
        clipboard.setToolTipText(JDLocale.L("gui.menu.action.clipboard.desc", null));
        clipboard.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {

                if (clipboard.isSelected()) {
                    clipboard.setIcon(JDTheme.II("gui.images.clipboard_enabled", 24, 24));
                } else {
                    clipboard.setIcon(JDTheme.II("gui.images.clipboard_disabled", 24, 24));
                }
                // if (JDUtilities.getConfiguration().isChanges()) {
                // JDUtilities.getConfiguration().save();
                // }
                ClipboardHandler.getClipboard().setEnabled(clipboard.isSelected());
            }

        });
        clipboard.setName(JDLocale.L("quickhelp.toolbar.clipboard", "Toolbar clipboard observer"));
        clipboard.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true));

        /* reconect */
        add(reconnect = new JToggleButton(JDTheme.II("gui.images.reconnect_disabled", 24, 24)), BUTTON_CONSTRAINTS);
        reconnect.setName(JDLocale.L("quickhelp.toolbar.reconnect", "Reconnect Toolbar"));
        reconnect.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (reconnect.isSelected()) {
                    reconnect.setIcon(JDTheme.II("gui.images.reconnect_enabled", 24, 24));
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                } else {
                    reconnect.setIcon(JDTheme.II("gui.images.reconnect_disabled", 24, 24));
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, true);

                }
                if (JDUtilities.getConfiguration().isChanges()) {
                    JDUtilities.getConfiguration().save();
                }

            }

        });
        reconnect.setSelected(!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, true));
        JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_LATEST_RECONNECT_RESULT) {

            @Override
            public void onPropertyChanged(Property source, String valid) {
                updateReconnectButton();
            }

        });
        updateReconnectButton();

    }

    private void updateManReconnectButton() {
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_LATEST_RECONNECT_RESULT, true)) {
            reconnectButton.setIcon(JDTheme.II("gui.images.reconnect_warning", 24, 24));
            reconnectButton.setToolTipText(JDLocale.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
        } else {
            reconnectButton.setToolTipText(JDLocale.L("gui.menu.action.reconnectman.desc", "Manual reconnect. Get a new IP by resetting your internet connection"));
            reconnectButton.setIcon(JDTheme.II("gui.images.reconnect", 24, 24));
        }
    }

    private void updateReconnectButton() {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_LATEST_RECONNECT_RESULT, true)) {
            reconnect.setEnabled(true);
            reconnect.setToolTipText(JDLocale.L("gui.menu.action.doreconnect.desc", null));
        } else {
            reconnect.setEnabled(true);
            reconnect.setToolTipText(JDLocale.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
        }
    }

    private void initController() {
        add(playButton = new JButton(JDTheme.II("gui.images.next", 24, 24)), BUTTON_CONSTRAINTS);
        playButton.setToolTipText(JDLocale.L("gui.menu.action.start.desc", null));

        playButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JDUtilities.getController().startDownloads();
                if (JDToolBar.this.pause > 0) {
                    setPause(false);
                }
                pauseButton.setSelected(false);
            }

        });

        add(pauseButton = new JToggleButton(JDTheme.II("gui.images.break", 24, 24)), BUTTON_CONSTRAINTS);
        pauseButton.setToolTipText(JDLocale.L("gui.menu.action.break.desc", null));

        pauseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (pause >= 0) {
                    setPause(false);
                } else {
                    setPause(true);
                }
            }

        });
        add(stopButton = new JButton(JDTheme.II("gui.images.stop", 24, 24)), BUTTON_CONSTRAINTS);
        stopButton.setToolTipText(JDLocale.L("gui.menu.action.stop.desc", null));

        stopButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (pause > 0) {
                    setPause(false);
                }
                JDUtilities.getController().stopDownloads();
            }

        });
        stopButton.setEnabled(false);
        pauseButton.setEnabled(false);
        playButton.setEnabled(true);

    }

    private void addSpeedMeter() {
        speedmeter = new SpeedMeterPanel();
        speedmeter.setPreferredSize(new Dimension(100, 30));
        if (SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getBooleanProperty(SimpleGuiConstants.PARAM_SHOW_SPEEDMETER, true)) {
            add(speedmeter, "cell 0 13,dock east,hidemode 3,height 30,width 30:200:300");
        }
    }

    private void setPause(boolean b) {
        if (b) {
            pauseButton.setSelected(true);
            pause = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);
            SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 1);
        } else {
            pauseButton.setSelected(false);
            if (pause >= 0) SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, pause);
            pause = -1;
        }
        SubConfiguration.getConfig("DOWNLOAD").save();
    }

    public void controlEvent(final ControlEvent event) {
        new GuiRunnable<Object>() {

            // @Override
            public Object runSave() {
                switch (event.getID()) {
                case ControlEvent.CONTROL_DOWNLOAD_START:
                    stopButton.setEnabled(true);
                    pauseButton.setEnabled(true);
                    playButton.setEnabled(false);
                    if (speedmeter != null) {
                        speedmeter.start();
                    }
                    break;
                case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                case ControlEvent.CONTROL_DOWNLOAD_STOP:
                    stopButton.setEnabled(false);
                    pauseButton.setEnabled(false);
                    playButton.setEnabled(true);
                    if (speedmeter != null) speedmeter.stop();

                    break;
                case ControlEvent.CONTROL_JDPROPERTY_CHANGED:
                    if (event.getParameter() == Configuration.PARAM_LATEST_RECONNECT_RESULT) {
                        if (((Configuration) event.getSource()).getBooleanProperty(Configuration.PARAM_LATEST_RECONNECT_RESULT, true)) {
                            reconnect.setEnabled(true);
                            reconnect.setToolTipText(JDLocale.L("gui.menu.action.reconnect.desc", null));
                        } else {
                            reconnect.setEnabled(true);
                            reconnect.setToolTipText(JDLocale.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
                        }

                    }
                    if (event.getParameter() == Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE) {
                        clipboard.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, false));
                    }

                }
                return null;
            }

        }.start();

    }

}
