package jd.plugins.optional;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import jd.gui.UIInterface;
import jd.plugins.PluginOptional;
import jd.plugins.event.PluginEvent;
import jd.utils.JDUtilities;

public class JDTrayIcon extends PluginOptional implements ActionListener{
    private TrayIcon trayIcon;
    private MenuItem showHide;
    private MenuItem configuration;
    private MenuItem startStop;
    private MenuItem clipboard;
    private MenuItem reconnect;
    private MenuItem exit;
    private boolean uiVisible = true;
    
    @Override public String getCoder()                { return "astaldo";  }
    @Override public String getPluginID()   { return "0.0.0.1";  }
    @Override public String getPluginName() { return "TrayIcon"; }
    @Override public String getVersion()    { return "0.0.0.1";  }
    @Override
    public void enable(boolean enable) {
        if(enable){
            initGUI();
        }
        else{
            if(trayIcon != null)
                SystemTray.getSystemTray().remove(trayIcon);
        }
    }
    private void initGUI(){

        SystemTray tray = SystemTray.getSystemTray();
        Image image = JDUtilities.getImage("jd_logo");
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
        showHide      = new MenuItem("Show/Hide");
        configuration = new MenuItem("Configuration");
        startStop     = new MenuItem("Start/Stop");
        clipboard     = new MenuItem("Clipboard");
        reconnect     = new MenuItem("Reconnect");
        exit          = new MenuItem("Exit");

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
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == exit) {
            firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_CONTROL_EXIT,null));        }
        else if (e.getSource() == reconnect) {
            firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_CONTROL_RECONNECT,null));
        }
        else if (e.getSource() == clipboard) {
            firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_CONTROL_DND,null));
        }
        else if (e.getSource() == startStop) {
            firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_CONTROL_START_STOP,null));
        }
        else if (e.getSource() == configuration) {
            firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_CONTROL_SHOW_CONFIG,null));
        }
        else {
            uiVisible = !uiVisible;
            firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_CONTROL_SHOW_UI,uiVisible));
        }
    }
    @Override
    public String getRequirements() {
     return "JRE 1.6+";
    }
}
