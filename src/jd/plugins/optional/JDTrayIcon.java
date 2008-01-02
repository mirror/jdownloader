package jd.plugins.optional;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import jd.plugins.PluginOptional;
import jd.plugins.event.PluginEvent;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import sun.misc.Service;

public class JDTrayIcon extends PluginOptional implements ActionListener {
    private TrayIcon trayIcon;

    private MenuItem showHide;

    private MenuItem configuration;

    private MenuItem startStop;

    private MenuItem clipboard;

    private MenuItem reconnect;

    private MenuItem exit;

    private boolean  uiVisible = true;

    @Override
    public String getCoder() {
        return "astaldo";
    }

    @Override
    public String getPluginID() {
        return "0.0.0.1";
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.trayIcon.name","TrayIcon");
    }

    @Override
    public String getVersion() {
        return "0.0.0.1";
    }

    @Override
    public void enable(boolean enable) throws Exception {
        if(JDUtilities.getJavaVersion()>=1.6){
        if (enable) {
            logger.info("Systemtray ok: java "+JDUtilities.getJavaVersion());
            initGUI();
        }
        else {
            if (trayIcon != null) SystemTray.getSystemTray().remove(trayIcon);
        }
        }else{
            logger.severe("Error initializing SystemTray: Tray is supported since Java 1.6. your Version: "+JDUtilities.getJavaVersion());
        }
    }

    private void initGUI() {

        try {
            SystemTray tray = SystemTray.getSystemTray();

            Service s;
            Image image = JDUtilities.getImage(JDTheme.I("gui.images.jd_logo"));
            PopupMenu popup = new PopupMenu();
            trayIcon = new TrayIcon(image, JDUtilities.getJDTitle(), popup);

            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(this);
            try {
                tray.add(trayIcon);
            }
            catch (AWTException e) {
                logger.severe("TrayIcon could not be added.");
            }
            showHide = new MenuItem(JDLocale.L("plugins.optional.trayIcon.showorhide","Show/Hide"));
            configuration = new MenuItem(JDLocale.L("plugins.optional.trayIcon.configuration","Configuration"));
            startStop = new MenuItem(JDLocale.L("plugins.optional.trayIcon.startorstop","Start/Stop"));
            clipboard = new MenuItem(JDLocale.L("plugins.optional.trayIcon.clipboard","Clipboard"));
            reconnect = new MenuItem(JDLocale.L("plugins.optional.trayIcon.reconnect","Reconnect"));
            exit = new MenuItem(JDLocale.L("plugins.optional.trayIcon.exit","Exit"));

            showHide.addActionListener(this);
            configuration.addActionListener(this);
            startStop.addActionListener(this);
            clipboard.addActionListener(this);
            reconnect.addActionListener(this);
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
        catch (Exception e) {
            logger.severe("Error initializing SystemTray " + e.getMessage());
            return;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == exit) {
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_CONTROL_EXIT, null));
        }
        else if (e.getSource() == reconnect) {
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_CONTROL_RECONNECT, null));
        }
        else if (e.getSource() == clipboard) {
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_CONTROL_DND, null));
        }
        else if (e.getSource() == startStop) {
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_CONTROL_START_STOP, null));
        }
        else if (e.getSource() == configuration) {
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_CONTROL_SHOW_CONFIG, null));
        }
        else {
            uiVisible = !uiVisible;
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_CONTROL_SHOW_UI, uiVisible));
        }
    }

    @Override
    public String getRequirements() {
        return "JRE 1.6+";
    }

    @Override
    public boolean isExecutable() {
        return false;
    }
    @Override
    public boolean execute() {
        return false;
    }
}
