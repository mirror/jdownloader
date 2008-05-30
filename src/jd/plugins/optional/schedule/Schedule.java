package jd.plugins.optional.schedule;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.config.MenuItem;
import jd.event.ControlListener;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Schedule extends PluginOptional implements ControlListener {
    public static int getAddonInterfaceVersion(){
        return 0;
    }
    ScheduleControl b = new ScheduleControl();
    
    public void initschedule(){
        b.status.start();
    }
    
    @Override
    public boolean initAddon() {
      
               logger.info("Schedule OK");
               this.initschedule();
               JDUtilities.getController().addControlListener(this);
               return true;
          
     
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(new MenuItem(JDLocale.L("addons.schedule.menu.settings","Settings"),0).setActionListener(this));
        return menu;
    }

    @Override
    public String getCoder() {
        return "Tudels";
    }

    @Override
    public String getPluginID() {
        return "0.4";
    }

    @Override
    public String getVersion() {
        return "0.4";
    }

    public String getPluginName() {
        return JDLocale.L("addons.schedule.name","Schedule");
    }
    
    public void actionPerformed(ActionEvent e) {
        this.b.setVisible(true);
    }

    @Override
    public void onExit() {
        // TODO Auto-generated method stub
        
    }
    
}
