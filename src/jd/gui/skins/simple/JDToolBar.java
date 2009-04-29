package jd.gui.skins.simple;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.ClipboardHandler;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.components.SpeedMeterPanel;
import jd.gui.skins.simple.listener.MouseAreaListener;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class JDToolBar extends JToolBar implements ControlListener {

    private static final long serialVersionUID = 7533138014274040205L;

    private static final Object BUTTON_CONSTRAINTS = "gaptop 2";

    public static final int DISPLAY = 28;

    public static final int LEFTGAP = 2;

    public static final int IMGSIZE = 54;

    private JButton playButton;
    private JToggleButton pauseButton;
    private JButton stopButton;
    protected int pause = -1;
    private JToggleButton clipboard;
    // private JToggleButton premium;
    private JToggleButton reconnect;

    private SpeedMeterPanel speedmeter;

    private Image logo;

    private boolean noTitlePainter;

    private JToggleButton reconnectButton;

    public JDToolBar(boolean noTitlePane, Image mainMenuIcon) {
        super(JToolBar.HORIZONTAL);

        setRollover(true);
        JDUtilities.getController().addControlListener(this);
        this.setFloatable(false);
        this.setLayout(new MigLayout("ins 0,gap 0", "[][][][][][][][][][][][][][grow,fill]"));

        // IconMenuBar mb = new IconMenuBar();
        // JMenu menu = new JMenu("");
        // // menu.setSize(50,50);
        //
        // menu.setPreferredSize(mb.getMinimumSize());
        // menu.setMinimumSize(mb.getMinimumSize());
        // menu.setOpaque(false);
        // menu.setBackground(null);
        //
        // JDStartMenu.createMenu(menu);
        // mb.add(menu);

        // add(mb, "gapright 15");
        // mb.setVisible(false);
        JPanel bt = new JPanel();
        bt.setVisible(false);

        // bt.setContentAreaFilled(false);
        // bt.setBorderPainted(false);

        noTitlePainter = noTitlePane;
        if (noTitlePainter) {
            add(new JSeparator(JSeparator.VERTICAL), "gapleft 32,height 0,gapright 5");
        } else {
            add(new JSeparator(JSeparator.VERTICAL), "gapleft 48,height 0,gapright 5");
        }
        initController();
        add(new JSeparator(JSeparator.VERTICAL), "height 32,gapleft 10,gapright 10");
        initQuickConfig();
        add(new JSeparator(JSeparator.VERTICAL), "height 32,gapleft 10,gapright 10");
        initInteractions();

        addSpeedMeter();
        setPause(false);
        logo = mainMenuIcon;
        MouseAreaListener ml;
        this.addMouseMotionListener(ml = new MouseAreaListener(LEFTGAP, 0, IMGSIZE + LEFTGAP, IMGSIZE));
        this.addMouseListener(ml);
    }

    private void initInteractions() {
        add(reconnectButton = new JToggleButton(JDTheme.II("gui.images.reconnect", 24, 24)), BUTTON_CONSTRAINTS);
        reconnectButton.setToolTipText(JDLocale.L("gui.menu.action.reconnect.desc", "Manual reconnect. Get a new IP by resetting your internet connection"));
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_LATEST_RECONNECT_RESULT, true)) {

            reconnectButton.setIcon(JDTheme.II("gui.images.reconnect_warning", 24, 24));
            reconnectButton.setToolTipText(JDLocale.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
        } else {
            reconnectButton.setToolTipText(JDLocale.L("gui.menu.action.reconnectman.desc", "Manual reconnect. Get a new IP by resetting your internet connection"));
            reconnectButton.setIcon(JDTheme.II("gui.images.reconnect", 24, 24));
        }

        JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_LATEST_RECONNECT_RESULT) {

            @Override
            public void onPropertyChanged(Property source, String valid) {
                if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_LATEST_RECONNECT_RESULT, true)) {

                    reconnectButton.setIcon(JDTheme.II("gui.images.reconnect_warning", 24, 24));
                    reconnectButton.setToolTipText(JDLocale.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
                } else {
                    reconnectButton.setToolTipText(JDLocale.L("gui.menu.action.reconnectman.desc", "Manual reconnect. Get a new IP by resetting your internet connection"));
                    reconnectButton.setIcon(JDTheme.II("gui.images.reconnect", 24, 24));
                }
            }

        });
        reconnectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SimpleGUI.CURRENTGUI.doManualReconnect();

            }

        });

    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (noTitlePainter) {
            ((Graphics2D) g).drawImage(logo, LEFTGAP, 0, 32 + LEFTGAP, 32, 0, 0, 32, 32, null);

        } else {
            ((Graphics2D) g).drawImage(logo, LEFTGAP, 0, IMGSIZE + LEFTGAP, DISPLAY, 0, IMGSIZE - DISPLAY, IMGSIZE, IMGSIZE, null);
        }
    }

    private void initQuickConfig() {
        /* Clipboard */

        add(clipboard = new JToggleButton(JDTheme.II("gui.images.clipboard_disabled", 24, 24)), BUTTON_CONSTRAINTS);
        clipboard.setToolTipText(JDLocale.L("gui.menu.action.clipboard.desc", null));

        clipboard.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (clipboard.isSelected()) {
                    clipboard.setIcon(JDTheme.II("gui.images.clipboard_enabled", 24, 24));
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true);
                } else {
                    clipboard.setIcon(JDTheme.II("gui.images.clipboard_disabled", 24, 24));
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, false);
                }
                JDUtilities.getConfiguration().save();
                ClipboardHandler.getClipboard().setEnabled(clipboard.isSelected());
            }

        });
        clipboard.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true));
        /* Premium */
        // add(premium = new
        // JToggleButton(JDTheme.II("gui.images.premium_disabled", 24, 24)),
        // BUTTON_CONSTRAINTS);
        // premium.setToolTipText(JDLocale.L("gui.menu.action.premium.desc",
        // "Enable Premiumusage globaly"));
        //
        // premium.addChangeListener(new ChangeListener() {
        //
        // public void stateChanged(ChangeEvent e) {
        // if (premium.isSelected()) {
        // premium.setIcon(JDTheme.II("gui.images.premium_enabled", 24, 24));
        // JDUtilities.getConfiguration().setProperty(Configuration.
        // PARAM_USE_GLOBAL_PREMIUM,
        // true);
        // } else {
        // premium.setIcon(JDTheme.II("gui.images.premium_disabled", 24, 24));
        // JDUtilities.getConfiguration().setProperty(Configuration.
        // PARAM_USE_GLOBAL_PREMIUM,
        // false);
        //
        // }
        // JDUtilities.getConfiguration().save();
        //
        // }
        //
        // });
        //
        //premium.setSelected(JDUtilities.getConfiguration().getBooleanProperty(
        // Configuration.PARAM_USE_GLOBAL_PREMIUM,
        // true));
        /* reconect */
        add(reconnect = new JToggleButton(JDTheme.II("gui.images.reconnect_disabled", 24, 24)), BUTTON_CONSTRAINTS);

        reconnect.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (reconnect.isSelected()) {
                    reconnect.setIcon(JDTheme.II("gui.images.reconnect_enabled", 24, 24));
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                } else {
                    reconnect.setIcon(JDTheme.II("gui.images.reconnect_disabled", 24, 24));
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, true);

                }
                JDUtilities.getConfiguration().save();

            }

        });
        reconnect.setSelected(!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));
        JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_LATEST_RECONNECT_RESULT) {

            @Override
            public void onPropertyChanged(Property source, String valid) {
                if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_LATEST_RECONNECT_RESULT, true)) {
                    reconnect.setEnabled(true);
                    reconnect.setToolTipText(JDLocale.L("gui.menu.action.doreconnect.desc", null));
                } else {
                    reconnect.setEnabled(true);
                    reconnect.setToolTipText(JDLocale.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
                }
            }

        });
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

                if (JDToolBar.this.pause >= 0) {
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
                if (JDToolBar.this.pause > 0) {
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
            JDToolBar.this.pause = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);
            SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 1);

        } else {
            pauseButton.setSelected(false);
            SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, Math.max(JDToolBar.this.pause, 0));
            JDToolBar.this.pause = -1;

        }

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

    public void setMainMenuIcon(Image mainMenuIcon) {
        this.logo = mainMenuIcon;
        this.repaint(0, 0, 48, 48);
        // this.revalidate();

    }
}
