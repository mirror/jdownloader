package jd.plugins.optional;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JWindow;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.gui.skins.simple.JDAction;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class TrayIconPopup extends JWindow implements MouseListener, MouseMotionListener, ChangeListener {

    private static final Color BACKGROUNDCOLOR = Color.WHITE;
    private static final int MARGIN = 2; 
    private static final Insets INSETS = new Insets(1, 1, 1, 1);
    private static final int FILL_NONE = GridBagConstraints.NONE;
    private static final int ANCHOR_NORTH_WEST = GridBagConstraints.NORTHWEST;
    private static final int FILL_BOTH = GridBagConstraints.BOTH;
    private static final int MENUENTRY_HEIGHT = 16;
    private static final int MENUENTRY_LABEL_WIDTH = 220;
    private static final int MENUENTRY_ICON_WIDTH = MENUENTRY_HEIGHT+12;
    private static final Color HIGHLIGHT_COLOR = Color.BLUE;
    private static final Color DISABLED_COLOR = Color.GRAY;
    private static final int ACTION_LOAD = 0;
    private static final int ACTION_START = 1;
    private static final int ACTION_STOP = 2;
    private static final int ACTION_PAUSE = 3;
    private static final int ACTION_ADD = 4;
    private static final int ACTION_UPDATE = 5;
    private static final int ACTION_CONFIG = 6;
    private static final int ACTION_LOG = 7;
    private static final int ACTION_RECONNECT = 8;
    private static final int ACTION_TOGGLE_CLIPBOARD = 9;
    private static final int ACTION_TOGGLE_RECONNECT = 10;
    private static final int ACTION_EXIT = 11;
    private static final int ANCHOR_WEST = GridBagConstraints.WEST;
    private JPanel topPanel;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JPanel bottomPanel;
    private boolean enteredPopup;
    private JDLightTray owner;
    private int midPanelCounter = 0;
    private Point point;
    private int mouseOverRow;
    private ArrayList<Integer> entries = new ArrayList<Integer>();
    private JSpinner spMax;
    private JSpinner spMaxDls;

    TrayIconPopup(JDLightTray tracIcon) {
        this.owner = tracIcon;
        setLayout(new GridBagLayout());
        addMouseMotionListener(this);
        addMouseListener(this);
        toFront();
        setAlwaysOnTop(true);

        init();

        initTopPanel();
        addMenuEntry(ACTION_LOAD, JDTheme.I("gui.images.load"), JDLocale.L("plugins.trayicon.popup.menu.load", "Container laden"));
        switch (JDUtilities.getController().getDownloadStatus()) {
        case JDController.DOWNLOAD_NOT_RUNNING:
            addMenuEntry(ACTION_START, JDTheme.I("gui.images.next"), JDLocale.L("plugins.trayicon.popup.menu.start", "Download Starten"));

            break;
        case JDController.DOWNLOAD_RUNNING:
            addMenuEntry(ACTION_STOP, JDTheme.I("gui.images.stop"), JDLocale.L("plugins.trayicon.popup.menu.stop", "Download anhalten"));

            break;
        default:
            addDisabledMenuEntry(JDTheme.I("gui.images.next"), JDLocale.L("plugins.trayicon.popup.menu.start", "Download Starten"));

        }
        if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_RUNNING) addMenuEntry(ACTION_PAUSE, JDTheme.I("gui.images.stop_after"), JDLocale.L("plugins.trayicon.popup.menu.pause", "Nach diesem Download anhalten"));
        addMenuEntry(ACTION_ADD, JDTheme.I("gui.images.add"), JDLocale.L("plugins.trayicon.popup.menu.add", "Downloads hinzufügen"));

        addMenuEntry(ACTION_UPDATE, JDTheme.I("gui.images.update_manager"), JDLocale.L("plugins.trayicon.popup.menu.update", "JD aktualisieren"));

        addMenuEntry(ACTION_CONFIG, JDTheme.I("gui.images.configuration"), JDLocale.L("plugins.trayicon.popup.menu.config", "Konfiguration"));

        addMenuEntry(ACTION_LOG, JDTheme.I("gui.images.terminal"), JDLocale.L("plugins.trayicon.popup.menu.log", "Log anzeigen"));

        addMenuEntry(ACTION_RECONNECT, JDTheme.I("gui.images.reconnect"), JDLocale.L("plugins.trayicon.popup.menu.reconnect", "Reconnect durchführen"));

        addMenuEntry(ACTION_TOGGLE_CLIPBOARD, getClipBoardImage(), JDLocale.L("plugins.trayicon.popup.menu.toggleClipboard", "Zwischenablage an/aus"));
        addMenuEntry(ACTION_TOGGLE_RECONNECT, getReconnectImage(), JDLocale.L("plugins.trayicon.popup.menu.toggleReconnect", "Reconnect an/aus"));
        addMenuEntry(ACTION_EXIT, JDTheme.I("gui.images.exit"), JDLocale.L("plugins.trayicon.popup.menu.exit", "Beenden"));

        initBottomPanel();
        setVisible(true);
        pack();

    }

    private void initBottomPanel() {
        int maxspeed = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);

        spMax = new JSpinner();
        spMax.setModel(new SpinnerNumberModel(maxspeed, 0, Integer.MAX_VALUE, 50));
        spMax.setPreferredSize(new Dimension(60, 20));
        spMax.setToolTipText(JDLocale.L("gui.tooltip.statusbar.speedlimiter", "Geschwindigkeitsbegrenzung festlegen(kb/s) [0:unendlich]"));
        spMax.addChangeListener(this);

        spMaxDls = new JSpinner();
        spMaxDls.setModel(new SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 3), 1, 20, 1));
        spMaxDls.setPreferredSize(new Dimension(60, 20));
        spMaxDls.setToolTipText(JDLocale.L("gui.tooltip.statusbar.simultan_downloads", "Max. gleichzeitige Downloads"));
        spMaxDls.addChangeListener(this);

        JDUtilities.addToGridBag(bottomPanel, new JLabel(JDLocale.L("plugins.trayicon.popup.bottom.speed", "Geschwindigkeitsbegrenzung")), 0, 0, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_WEST);
        JDUtilities.addToGridBag(bottomPanel, spMax, 1, 0, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_NORTH_WEST);
        JDUtilities.addToGridBag(bottomPanel, new JLabel(JDLocale.L("plugins.trayicon.popup.bottom.simDls", "Gleichzeitige Downloads")), 0, 1, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_WEST);
        JDUtilities.addToGridBag(bottomPanel, spMaxDls, 1, 1, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_NORTH_WEST);

    }

    private ImageIcon getClipBoardImage() {
        if (JDUtilities.getController().getClipboard().isEnabled())
            return JDTheme.I("gui.images.clipboardon");
        else
            return JDTheme.I("gui.images.clipboardoff");

    }

    private ImageIcon getReconnectImage() {

        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false))
            return JDTheme.I("gui.images.reconnect_ok");
        else
            return JDTheme.I("gui.images.reconnect_bad");

    }

    private void addMenuEntry(Integer id, ImageIcon i, String l) {
        JLabel b;
        JLabel icon;
        JDUtilities.addToGridBag(leftPanel, icon = new JLabel(new ImageIcon(i.getImage().getScaledInstance(MENUENTRY_HEIGHT, MENUENTRY_HEIGHT, Image.SCALE_SMOOTH))), 0, midPanelCounter, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_NORTH_WEST);

        JDUtilities.addToGridBag(rightPanel, b = new JLabel(l), 0, midPanelCounter, 1, 1, 0, 1, new Insets(1, 4, 1, 1), FILL_NONE, ANCHOR_NORTH_WEST);
        entries.add(id);
        this.midPanelCounter++;
        b.setHorizontalAlignment(SwingConstants.LEFT);

        b.setOpaque(false);
        icon.setOpaque(false);
        b.setPreferredSize(new Dimension(MENUENTRY_LABEL_WIDTH, MENUENTRY_HEIGHT));
        icon.setPreferredSize(new Dimension(MENUENTRY_ICON_WIDTH, MENUENTRY_HEIGHT));

    }

    private void addDisabledMenuEntry(ImageIcon i, String l) {
        JLabel b;
        JLabel icon;
        JDUtilities.addToGridBag(leftPanel, icon = new JLabel(new ImageIcon(i.getImage().getScaledInstance(MENUENTRY_HEIGHT, MENUENTRY_HEIGHT, Image.SCALE_SMOOTH))), 0, midPanelCounter, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_NORTH_WEST);

        JDUtilities.addToGridBag(rightPanel, b = new JLabel(l), 0, midPanelCounter, 1, 1, 0, 1, new Insets(1, 4, 1, 1), FILL_NONE, ANCHOR_NORTH_WEST);
        entries.add(null);
        this.midPanelCounter++;
        b.setHorizontalAlignment(SwingConstants.LEFT);

        b.setOpaque(false);
        icon.setOpaque(false);
        b.setPreferredSize(new Dimension(MENUENTRY_LABEL_WIDTH, MENUENTRY_HEIGHT));
        icon.setPreferredSize(new Dimension(MENUENTRY_ICON_WIDTH, MENUENTRY_HEIGHT));
        icon.setForeground(Color.GRAY);
        b.setForeground(Color.GRAY);
        icon.setEnabled(false);
        b.setEnabled(false);

    }

    private void initTopPanel() {
        ImageIcon logo = new ImageIcon(JDTheme.I("gui.images.jd_logo").getImage().getScaledInstance(MENUENTRY_HEIGHT, MENUENTRY_HEIGHT, Image.SCALE_SMOOTH));
        JDUtilities.addToGridBag(topPanel, new JLabel(JDLocale.L("plugins.trayicon.popup.title", "JDownloader") + " 0." + JDUtilities.getRevision(), logo, SwingConstants.LEFT), 0, 0, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_NORTH_WEST);
        // JDUtilities.addToGridBag(topPanel,new JSeparator(), 0, 1, 1, 1, 0,
        // 0,INSETS, FILL_BOTH, ANCHOR_NORTH_WEST);
        // JDUtilities.addToGridBag(topPanel,new JLabel(""), 0, 1, 1, 1, 0,
        // 0,INSETS, FILL_BOTH, ANCHOR_NORTH_WEST);
        //      
    }

    private void init() {
        JPanel p;
        p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JDUtilities.addToGridBag(this, p, 0, 0, 1, 1, 1, 1, new Insets(0, 0, 0, 0), GridBagConstraints.BOTH, GridBagConstraints.SOUTHEAST);

        topPanel = new JPanel(new GridBagLayout());
        leftPanel = new JPanel(new GridBagLayout());
        rightPanel = new JPanel(new GridBagLayout());
        bottomPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(BACKGROUNDCOLOR);
        leftPanel.setBackground(BACKGROUNDCOLOR);
        rightPanel.setBackground(BACKGROUNDCOLOR);
        bottomPanel.setBackground(BACKGROUNDCOLOR);

        topPanel.setOpaque(false);
        leftPanel.setOpaque(false);
        rightPanel.setOpaque(false);
        bottomPanel.setOpaque(false);
        JDUtilities.addToGridBag(p, topPanel, 0, 0, 2, 1, 0, 0, new Insets(MARGIN, MARGIN, 0, MARGIN), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(p, leftPanel, 0, 1, 1, 1, 0, 0, new Insets(0, MARGIN, 0, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(p, rightPanel, 1, 1, 1, 1, 0, 0, new Insets(0, 0, 0, MARGIN), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(p, bottomPanel, 0, 2, 2, 1, 0, 0, new Insets(0, MARGIN, MARGIN, MARGIN), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.LIGHT_GRAY));
    }

    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseEntered(MouseEvent e) {
        this.enteredPopup = true;

    }

    public void mouseExited(MouseEvent e) {
        if (e.getSource() == this && enteredPopup && !this.contains(e.getPoint())) {
          
            dispose();

        }

    }

  

    public void mousePressed(MouseEvent e) {
        this.point = e.getPoint();
        checkUpdate(point);

    }

    public void mouseReleased(MouseEvent e) {
        this.point = e.getPoint();
        checkUpdate(point);
        if (mouseOverRow < 0 || entries.get(mouseOverRow) != null) onAction(mouseOverRow);
    }

    private void onAction(int row) {
        SimpleGUI simplegui = SimpleGUI.CURRENTGUI;
        if (row < 0) {
            simplegui.getFrame().setVisible(!simplegui.getFrame().isVisible());
            return;
        }
        JDUtilities.getLogger().info(" ACTIOn " + entries.get(row));
        switch (entries.get(row)) {

        case TrayIconPopup.ACTION_ADD:
            simplegui.actionPerformed(new ActionEvent(this, JDAction.ITEMS_ADD, null));
            break;
        case TrayIconPopup.ACTION_CONFIG:
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_CONFIGURATION, null));
            break;
        case TrayIconPopup.ACTION_LOAD:
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_LOAD_DLC, null));
            break;
        case TrayIconPopup.ACTION_LOG:
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_LOG, null));

            break;
        case TrayIconPopup.ACTION_PAUSE:
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_PAUSE_DOWNLOADS, null));
            break;
        case TrayIconPopup.ACTION_RECONNECT:
            simplegui.doReconnect();
            break;
        case TrayIconPopup.ACTION_START:
        case TrayIconPopup.ACTION_STOP:
            JDUtilities.getController().toggleStartStop();
            break;
        case TrayIconPopup.ACTION_TOGGLE_CLIPBOARD:
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_CLIPBOARD, null));
            break;
        case TrayIconPopup.ACTION_TOGGLE_RECONNECT:
            simplegui.toggleReconnect(false);
            break;
        case TrayIconPopup.ACTION_UPDATE:
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_UPDATE, null));
            break;
        case TrayIconPopup.ACTION_EXIT:
            JDUtilities.getController().exit();
            break;
        }
        dispose();

    }

    public void mouseDragged(MouseEvent e) {
        this.point = e.getPoint();
        checkUpdate(point);

    }

    public void mouseMoved(MouseEvent e) {
        this.point = e.getPoint();
        checkUpdate(point);

    }

    private int getRow(Point e) {
        int y = e.y;
        y -= rightPanel.getY();
        if (y < 0) { return -1; }
        y /= (rightPanel.getHeight() / midPanelCounter);

        return y;

    }

    private boolean checkUpdate(Point p) {
        if (this.mouseOverRow != getRow(p)) {
            mouseOverRow = getRow(p);
            paint();
            return true;
        }

        return false;
    }

    private void paint() {
        Graphics g = this.getContentPane().getParent().getGraphics();
        getContentPane().setBackground(BACKGROUNDCOLOR);
        Point p = rightPanel.getLocation();

        int y = 0;
        // g.clearRect(0, p.y, getWidth(), rightPanel.getHeight());
        for (int i = 0; i < midPanelCounter; i++) {
            y = i * (rightPanel.getHeight() / midPanelCounter) + p.y;

            if (mouseOverRow >= 0 && point.y >= y && point.y < ((i + 1) * (rightPanel.getHeight() / midPanelCounter) + p.y)) {
                if (entries.get(mouseOverRow) != null) {
                    g.setColor(HIGHLIGHT_COLOR);
                } else {
                    g.setColor(DISABLED_COLOR);
                }

            } else {
                g.setColor(BACKGROUNDCOLOR);
            }
            g.drawRect(2, y, getWidth() - 4, (rightPanel.getHeight() / midPanelCounter) - 1);
            // if (i % 2 == 0) {
            // g.setColor(Color.RED);
            // } else {
            // g.setColor(Color.BLUE);
            // }

        }

    }

    public void stateChanged(ChangeEvent e) {
        int max = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);

        if (e.getSource() == spMax) {
            int value = (Integer) spMax.getValue();

            if (max != value) {
                JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, value);
                JDUtilities.getSubConfig("DOWNLOAD").save();
            }

        }
        if (e.getSource() == this.spMaxDls) {
            int value = (Integer) spMaxDls.getValue();

            if (max != value) {
                JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, value);
                JDUtilities.getSubConfig("DOWNLOAD").save();
            }

        }
        SimpleGUI.CURRENTGUI.updateStatusBar();

    }

}