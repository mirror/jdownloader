package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.InteractionTrigger;
import jd.event.ControlEvent;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Replacer;;


public class JDGrowlNotification extends PluginOptional {
    public static int getAddonInterfaceVersion() {
        return 2;
    }
    public JDGrowlNotification(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    private static final String PROPERTY_ENABLED = "PROPERTY_ENABLED";



    
    @Override
    public String getCoder() {
        return "jD-Team";
    }
    
    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }
    
    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.jdgrowlnotification.name", "JDGrowlNotification");
    }
    
   
    @Override
    public boolean initAddon() {
        JDUtilities.getController().addControlListener(this);
        logger.info("Growl OK");
        return true; 
    }

    public void actionPerformed(ActionEvent e) {
        MenuItem mi = (MenuItem) e.getSource();
        if (mi.getActionID() == 0) {
            getPluginConfig().setProperty(PROPERTY_ENABLED, true);
            getPluginConfig().save();
            JDUtilities.getGUI().showMessageDialog(JDLocale.L("addons.jdgrowlnotification.statusmessage.enabled", "Notifications An"));
        } else {
            getPluginConfig().setProperty(PROPERTY_ENABLED, false);
            getPluginConfig().save();
            JDUtilities.getGUI().showMessageDialog(JDLocale.L("addons.jdgrowlnotification.statusmessage.disabled", "´" + "Notifications Aus"));
        }
    }
    
    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        MenuItem m;
        if (!JDUtilities.getSubConfig("ADDONS_JDGROWLNOTIFICATION").getBooleanProperty(PROPERTY_ENABLED, false)) {

            menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("addons.jdgrowlnotification.menu.enable", "Meldungen aktivieren"), 0).setActionListener(this));
            m.setSelected(false);
        } else {
            menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("addons.jdgrowlnotification.menu.disable", "Meldungen deaktivieren"), 1).setActionListener(this));
            m.setSelected(true);
        }
        return menu;
    }

    public void controlEvent(ControlEvent event) {
        
        super.controlEvent(event);
        if (getPluginConfig().getBooleanProperty(PROPERTY_ENABLED, false)) {
            if (event.getID() == ControlEvent.CONTROL_INTERACTION_CALL) {
                if (((InteractionTrigger) event.getSource()) == Interaction.INTERACTION_APPSTART) {
                    growlNotification("jDownloader gestartet...", 
                                        "Am " + Replacer.getReplacement("SYSTEM.DATE") + "um " + Replacer.getReplacement("SYSTEM.TIME"), 
                                        "Programmstart");
                                    
                }
            }
        }
        
    }


    public void growlNotification(String headline, String message, String title) {
        
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.indexOf("mac") >= 0) {
            JDUtilities.runCommand("/usr/bin/osascript", new String[] { JDUtilities.getResourceFile("jd/osx/growlNotification.scpt").getAbsolutePath(), headline, message, title  }, null, 0);
        }
        
    }

    @Override
    public void onExit() {
        // TODO Auto-generated method stub
        
    }
    
    
    
}
