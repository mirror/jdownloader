package jd.plugins.optional.schedule;

import java.util.ArrayList;
import java.awt.event.ActionEvent;
import javax.swing.event.*;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.event.ControlListener;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;


public class Schedule extends PluginOptional implements ControlListener {
    
    ScheduleFrame main = new ScheduleFrame();
    
    public void initschedule() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), "PORT", JDLocale.L("plugins.optional.getter.port", "Port:"), 1000, 65500));
        cfg.setDefaultValue(10000);
        
        main.setSize(300, 150);
        main.setResizable(false);
        main.setTitle(this.getPluginName()+" by "+this.getCoder());
        
    }

    @Override
    public void enable(boolean enable) throws Exception {
        if (JDUtilities.getJavaVersion() >= 1.6) {
            if (enable) {
               logger.info("Schedule OK");
               this.initschedule();
               JDUtilities.getController().addControlListener(this);
            }
        } else {
            logger.severe("Error initializing Schedule");
        }
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(new MenuItem("Settings",0).setActionListener(this));
        menu.add(new MenuItem("Start / Stop",1).setActionListener(this));
        return menu;
    }

    @Override
    public String getCoder() {
        return "Tudels";
    }

    @Override
    public String getPluginID() {
        return "0.1a";
    }

    @Override
    public String getVersion() {
        return "0.1a";
    }

    public String getPluginName() {
        return JDLocale.L("plugins.optional.JDschedule.name", "Schedule");
    }
    
    public void actionPerformed(ActionEvent e) {
        
        if (e.getID() == 0){
            main.setVisible(true); 
        }
        if (e.getID() == 1){
            if(main.t.isRunning() == false){
                main.t.start();
                JDUtilities.getGUI().showMessageDialog(this.getPluginName() + " started");
            }
            else{
                main.t.stop();
                JDUtilities.getGUI().showMessageDialog(this.getPluginName() + " stopped");
            }
        }
        
    }
    
}
