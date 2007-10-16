package jd.gui.skins.simple;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import jd.event.UIEvent;
import jd.gui.skins.simple.config.ConfigurationDialog;
import jd.plugins.Plugin;
import jd.utils.JDUtilities;

public class SimpleTrayIcon implements ActionListener {

    private SimpleGUI ui;

    private TrayIcon  trayIcon;

    private Logger    logger = Plugin.getLogger();

    private MenuItem  showHide;

    private MenuItem  configuration;

    private Menu      actions;

    private MenuItem  startStop;

    private MenuItem  stopAfter;

    private MenuItem  clipboard;

    private MenuItem  reconnect;

    private MenuItem  exit;

    public SimpleTrayIcon(SimpleGUI ui) {
        this.ui = ui;

        SystemTray tray = SystemTray.getSystemTray();
        Image image = JDUtilities.getImage("jd_logo");
        PopupMenu popup = new PopupMenu();
        trayIcon = new TrayIcon(image, ui.getFrame().getTitle(), popup);

        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(this);
        try {
            tray.add(trayIcon);
        }
        catch (AWTException e) {
            logger.severe("TrayIcon could not be added.");
        }

        showHide = new MenuItem("Show/Hide");
        showHide.addActionListener(this);
        configuration = new MenuItem("Configuration");
        configuration.addActionListener(this);
        startStop = new MenuItem("Start/Stop");
        startStop.addActionListener(this);
        clipboard = new MenuItem("Clipboard");
        clipboard.addActionListener(this);
        reconnect = new MenuItem("Reconnect");
        reconnect.addActionListener(this);
        exit = new MenuItem("Exit");
        exit.addActionListener(this);

        popup.add(showHide);
        popup.addSeparator();
        popup.add(startStop);
        popup.addSeparator();
        popup.add(clipboard);
        popup.add(configuration);
        popup.add(reconnect);
        popup.addSeparator();
        popup.add(exit);

    }

    public void showTip(String caption, String message) {
        trayIcon.displayMessage(caption, message, TrayIcon.MessageType.INFO);

    }

    public void setTooltip(String tooltip) {
        if (tooltip == null) tooltip = ui.getFrame().getTitle();
        trayIcon.setToolTip(tooltip);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == exit) {
            ui.getFrame().setVisible(false);
            ui.getFrame().dispose();
            ui.fireUIEvent(new UIEvent(ui, UIEvent.UI_EXIT));
        }
        else if (e.getSource() == reconnect) {
            ui.doReconnect();
        }
        else if (e.getSource() == clipboard) {
     
            ui.getFrame().setVisible(true);

            ui.toggleDnD();
        }
        else if (e.getSource() == startStop) {
           ui.btnStartStop.setSelected(!ui.btnStartStop.isSelected());
            ui.startStopDownloads();
        }
        else if (e.getSource() == configuration) {
            ui.getFrame().setVisible(true);
            boolean configChanged = ConfigurationDialog.showConfig(ui.getFrame(), ui);
            if (configChanged) ui.fireUIEvent(new UIEvent(ui, UIEvent.UI_SAVE_CONFIG));
            ui.getFrame().toFront();
        }
        else {

            ui.getFrame().setVisible(!ui.getFrame().isVisible());
            ui.getFrame().toFront();
        }

    }

}