//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.schedule;

import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.SingletonPanel;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.plugins.optional.schedule.modules.SchedulerModuleInterface;
import jd.plugins.optional.schedule.modules.SetSpeed;
import jd.plugins.optional.schedule.modules.StartDownloads;
import jd.plugins.optional.schedule.modules.StopDownloads;

@OptionalPlugin(rev="$Revision$", id="scheduler",interfaceversion=4)
public class Schedule extends PluginOptional {
    private static Schedule instance;
    
    private ArrayList<Actions> actions;
    
    private ArrayList<SchedulerModuleInterface> modules;

	private SingletonPanel sched;

	private SimpleDateFormat time;

	private SimpleDateFormat date;

    public Schedule(PluginWrapper wrapper) {
        super(wrapper);
        sched = new SingletonPanel(MainGui.class, this.getPluginConfig());
        instance = this;
        
        actions = this.getPluginConfig().getGenericProperty("Scheduler_Actions", new ArrayList<Actions>());
        if(actions == null) {
            actions = new ArrayList<Actions>();
            save();
        }
        
        initModules();
        
        time = new SimpleDateFormat("HH:mm");
        date = new SimpleDateFormat("dd.MM.yyyy");
        
        new Schedulercheck().start();
    }
    
    protected void save() {
    	this.getPluginConfig().setProperty("Scheduler_Actions", actions);
        this.getPluginConfig().save();
    }
    
    private void initModules() {
        modules = new ArrayList<SchedulerModuleInterface>();
        modules.add(new StartDownloads());
        modules.add(new StopDownloads());
        modules.add(new SetSpeed());
    }
    
    public ArrayList<SchedulerModuleInterface> getModules() {
        return modules;
    }
    
    public static Schedule getInstance() {
        return instance;
    }
    
    public ArrayList<Actions> getActions() {
        return actions;
    }
    
    public void removeAction(int row) {
        actions.remove(row);
        save();
    }
    
    public void addAction(Actions act) {
        actions.add(act);
        this.getPluginConfig().setProperty("Scheduler_Actions", actions);
        this.getPluginConfig().save();
    }

    public String getIconKey() {
        return "gui.images.config.eventmanager";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuItem && ((MenuItem) e.getSource()).getActionID() == 0) {
        	JDGui.getInstance().setContent(sched.getPanel());
        }
    }
    
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(new MenuItem(getHost(), 0).setActionListener(this));
        return menu;
    }

    public String getCoder() {
        return "JD-Team ";
    }

    public boolean initAddon() {
        logger.info("Schedule OK");
        return true;
    }

    public void onExit() {
    }
    
    public class Schedulercheck extends Thread {
        private Date today;
        
        public Schedulercheck() {
        	super("Schedulercheck");
        }

		public void run() {
            while (true) {
            	logger.finest("Checking scheduler");
                today = new Date(System.currentTimeMillis());
                String todaydate = date.format(today);
                String todaytime = time.format(today);
                
                for(Actions a : actions) {
                	if(a.isEnabled() && todaydate.equals(date.format(a.getDate())) && todaytime.equals(time.format(a.getDate()))) {
            			for(Executions e : a.getExecutions()) {
            				e.exceute();
            			}
            			
            			Calendar newrepeat = Calendar.getInstance();
            			newrepeat.setTime(a.getDate());
            			newrepeat.add(Calendar.MINUTE, a.getRepeat());
            			
            			a.setDate(newrepeat.getTime());
            			
            			save();
                	}
                }
                
                
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {}
            }
        };
    }
}