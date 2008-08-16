package jd.plugins.optional.schedule;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.config.MenuItem;
import jd.event.ControlListener;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Schedule extends PluginOptional implements ControlListener {
    public static int getAddonInterfaceVersion() {
        return 1;
    }

    ScheduleControl sControl = new ScheduleControl();

    @Override
    public void actionPerformed(ActionEvent e) {
        sControl.status.start();
        sControl.status.setInitialDelay(1000);
        sControl.setVisible(true);
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(new MenuItem(JDLocale.L("addons.schedule.menu.settings", "Settings"), 0).setActionListener(this));
        return menu;
    }

    @Override
    public String getCoder() {
        return "Tudels";
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("addons.schedule.name", "Schedule");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public boolean initAddon() {

        logger.info("Schedule OK");
        JDUtilities.getController().addControlListener(this);
        return true;

    }

    @Override
    public void onExit() {

    }

}
