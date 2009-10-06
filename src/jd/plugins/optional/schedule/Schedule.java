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
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.plugins.optional.schedule.modules.DisablePremium;
import jd.plugins.optional.schedule.modules.DisableReconnect;
import jd.plugins.optional.schedule.modules.DoReconnect;
import jd.plugins.optional.schedule.modules.EnablePremium;
import jd.plugins.optional.schedule.modules.EnableReconnect;
import jd.plugins.optional.schedule.modules.PauseDownloads;
import jd.plugins.optional.schedule.modules.SchedulerModuleInterface;
import jd.plugins.optional.schedule.modules.SetChunck;
import jd.plugins.optional.schedule.modules.SetMaxDownloads;
import jd.plugins.optional.schedule.modules.SetSpeed;
import jd.plugins.optional.schedule.modules.StartDownloads;
import jd.plugins.optional.schedule.modules.StopDownloads;
import jd.plugins.optional.schedule.modules.UnPauseDownloads;

@OptionalPlugin(rev = "$Revision$", id = "scheduler", hasGui = true, interfaceversion = 5)
public class Schedule extends PluginOptional {

    private ArrayList<Actions> actions;

    private ArrayList<SchedulerModuleInterface> modules;

    private SimpleDateFormat time;

    private SimpleDateFormat date;

    private SchedulerView view;

    private Schedulercheck sc;

    private boolean running = false;

    private MenuAction activateAction;

    public Schedule(PluginWrapper wrapper) {
        super(wrapper);

        actions = this.getPluginConfig().getGenericProperty("Scheduler_Actions", new ArrayList<Actions>());
        if (actions == null) {
            actions = new ArrayList<Actions>();
            save();
        }

        initModules();

        time = new SimpleDateFormat("HH:mm");
        date = new SimpleDateFormat("dd.MM.yyyy");

        startCheck();
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
        modules.add(new SetChunck());
        modules.add(new SetMaxDownloads());
        modules.add(new PauseDownloads());
        modules.add(new UnPauseDownloads());
        modules.add(new EnablePremium());
        modules.add(new DisablePremium());
        modules.add(new EnableReconnect());
        modules.add(new DisableReconnect());
        modules.add(new DoReconnect());
    }

    public ArrayList<SchedulerModuleInterface> getModules() {
        return modules;
    }

    public ArrayList<Actions> getActions() {
        return actions;
    }

    public void removeAction(int row) {
        if (row < 0) return;
        actions.remove(row);
        saveActions();
        stopCheck();
    }

    public void addAction(Actions act) {
        actions.add(act);
        saveActions();
        startCheck();
    }

    private void saveActions() {
        this.getPluginConfig().setProperty("Scheduler_Actions", actions);
        this.getPluginConfig().save();
    }

    public String getIconKey() {
        return "gui.images.config.eventmanager";
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == activateAction) {
            if (((MenuAction) e.getSource()).isSelected()) {
                showGui();
            } else {
                view.close();
            }
        }
    }

    private void showGui() {
        if (view == null) {
            view = new SchedulerView();
            view.getBroadcaster().addListener(new SwitchPanelListener() {

                @Override
                public void onPanelEvent(SwitchPanelEvent event) {
                    if (event.getID() == SwitchPanelEvent.ON_REMOVE) activateAction.setSelected(false);
                }

            });
            view.setContent(new MainGui(this));

        }
        activateAction.setSelected(true);
        JDGui.getInstance().setContent(view);
    }

    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        menu.add(activateAction);
        return menu;
    }

    public boolean initAddon() {
        logger.info("Schedule OK");
        activateAction = new MenuAction(getWrapper().getID(), 0);
        activateAction.setActionListener(this);
        activateAction.setTitle(getHost());
        activateAction.setIcon(this.getIconKey());
        activateAction.setSelected(false);
        return true;
    }

    public void setGuiEnable(boolean b) {
        if (b) {
            showGui();
        } else {
            if (view != null) view.close();
        }
    }

    public void onExit() {
    }

    public void startCheck() {
        if (sc == null) sc = new Schedulercheck();

        if (sc.isAlive() || !shouldStart()) return;

        logger.info("Starting scheduler");
        running = true;
        sc.start();
    }

    public void stopCheck() {
        if (sc == null || !sc.isAlive() || shouldStart()) return;

        logger.info("Stoping scheduler");
        running = false;
    }

    private boolean shouldStart() {
        if (actions.size() == 0) return false;

        for (Actions a : actions) {
            if (a.isEnabled()) return true;
        }

        return false;
    }

    public class Schedulercheck extends Thread {
        private Date today;

        public Schedulercheck() {
            super("Schedulercheck");
        }

        public void run() {
            while (running) {
                logger.finest("Checking scheduler");
                today = new Date(System.currentTimeMillis());
                String todaydate = date.format(today);
                String todaytime = time.format(today);

                for (Actions a : actions) {
                    if (a.isEnabled() && todaydate.equals(date.format(a.getDate())) && todaytime.equals(time.format(a.getDate()))) {
                        for (Executions e : a.getExecutions()) {
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
                } catch (InterruptedException e) {
                }
            }
        }
    }
}