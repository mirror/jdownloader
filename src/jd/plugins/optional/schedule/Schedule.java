package jd.plugins.optional.schedule;

import java.util.ArrayList;
import java.awt.event.ActionEvent;
import jd.config.MenuItem;
import jd.event.ControlListener;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import java.util.*;

public class Schedule extends PluginOptional implements ControlListener {
    
    Vector v = new Vector();
    
    public void initschedule() {
        for(int i = 0; i < 4; i++){
            v.add(i,new ScheduleFrame());
            ScheduleFrame s = (ScheduleFrame) v.elementAt(i);
            s.setTitle(this.getPluginName()+" by "+this.getCoder());
        }
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
        menu.add(new MenuItem("Schedule 1",0).setActionListener(this));
        menu.add(new MenuItem("Schedule 2",1).setActionListener(this));
        menu.add(new MenuItem("Schedule 3",2).setActionListener(this));
        menu.add(new MenuItem("Schedule 4",3).setActionListener(this));
        return menu;
    }

    @Override
    public String getCoder() {
        return "Tudels";
    }

    @Override
    public String getPluginID() {
        return "0.1";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    public String getPluginName() {
        return JDLocale.L("plugins.optional.JDschedule.name", "Schedule");
    }
    
    public void actionPerformed(ActionEvent e) {

        for (int i = 0; i < 4 ; ++i){
        if (e.getID() == i){
            ScheduleFrame s = (ScheduleFrame) v.elementAt(i);
            s.setVisible(true);
            s.repaint();
        }
        }
        
    }
    
}
