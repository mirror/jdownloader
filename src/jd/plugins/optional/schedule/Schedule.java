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
import jd.controlling.JDLogger;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.plugins.optional.schedule.modules.DisableClipboard;
import jd.plugins.optional.schedule.modules.DisableHost;
import jd.plugins.optional.schedule.modules.DisablePremium;
import jd.plugins.optional.schedule.modules.DisablePremiumForHost;
import jd.plugins.optional.schedule.modules.DisableReconnect;
import jd.plugins.optional.schedule.modules.DoHibernate;
import jd.plugins.optional.schedule.modules.DoReconnect;
import jd.plugins.optional.schedule.modules.DoShutdown;
import jd.plugins.optional.schedule.modules.DoSleep;
import jd.plugins.optional.schedule.modules.EnableClipboard;
import jd.plugins.optional.schedule.modules.EnableHost;
import jd.plugins.optional.schedule.modules.EnablePremium;
import jd.plugins.optional.schedule.modules.EnablePremiumForHost;
import jd.plugins.optional.schedule.modules.EnableReconnect;
import jd.plugins.optional.schedule.modules.PauseDownloads;
import jd.plugins.optional.schedule.modules.SchedulerModuleInterface;
import jd.plugins.optional.schedule.modules.SetChunck;
import jd.plugins.optional.schedule.modules.SetMaxDownloads;
import jd.plugins.optional.schedule.modules.SetSpeed;
import jd.plugins.optional.schedule.modules.SetStopMark;
import jd.plugins.optional.schedule.modules.StartDownloads;
import jd.plugins.optional.schedule.modules.StopDownloads;
import jd.plugins.optional.schedule.modules.UnPauseDownloads;
import jd.plugins.optional.schedule.modules.UnSetStopMark;

@OptionalPlugin(rev = "$Revision$", id = "scheduler", hasGui = true, interfaceversion = 5)
public class Schedule extends PluginOptional {

    private ArrayList<Actions> actions;

    private ArrayList<SchedulerModuleInterface> modules;

    private SchedulerView view;

    private MainGui gui;

    private Schedulercheck sc = null;

    private boolean running = false;

    private MenuAction activateAction;

    public static final Object LOCK = new Object();

    public Schedule(PluginWrapper wrapper) {
        super(wrapper);
        actions = this.getPluginConfig().getGenericProperty("Scheduler_Actions", new ArrayList<Actions>());
        if (actions == null) {
            actions = new ArrayList<Actions>();
            saveActions();
        }
        initModules();
        activateAction = new MenuAction("scheduler", 0);
        activateAction.setActionListener(this);
        activateAction.setTitle(getHost());
        activateAction.setIcon(this.getIconKey());
        activateAction.setSelected(false);
    }

    /**
     * TODO: Maybe refactor to use Annotations?
     */
    private void initModules() {
        modules = new ArrayList<SchedulerModuleInterface>();
        modules.add(new StartDownloads());
        modules.add(new StopDownloads());
        modules.add(new SetSpeed());
        modules.add(new SetChunck());
        modules.add(new SetMaxDownloads());
        modules.add(new PauseDownloads());
        modules.add(new UnPauseDownloads());
        modules.add(new EnableHost());
        modules.add(new DisableHost());
        modules.add(new EnablePremium());
        modules.add(new DisablePremium());
        modules.add(new EnablePremiumForHost());
        modules.add(new DisablePremiumForHost());
        modules.add(new EnableReconnect());
        modules.add(new DisableReconnect());
        modules.add(new DoReconnect());
        modules.add(new DoShutdown());
        modules.add(new DoSleep());
        modules.add(new DoHibernate());
        modules.add(new SetStopMark());
        modules.add(new UnSetStopMark());
        modules.add(new EnableClipboard());
        modules.add(new DisableClipboard());

    }

    public ArrayList<SchedulerModuleInterface> getModules() {
        return modules;
    }

    public ArrayList<Actions> getActions() {
        return actions;
    }

    public void removeAction(int row) {
        if (row < 0) return;
        synchronized (LOCK) {
            actions.remove(row);
            saveActions();
        }
        updateTable();
    }

    public void addAction(Actions act) {
        synchronized (LOCK) {
            actions.add(act);
            saveActions();
        }
        updateTable();
    }

    public void saveActions() {
        synchronized (LOCK) {
            this.getPluginConfig().setProperty("Scheduler_Actions", actions);
            this.getPluginConfig().save();

            if (actions.size() == 0) {
                running = false;
            } else if (actions.size() > 0 && !sc.isAlive()) {
                running = true;
                sc = new Schedulercheck();
                sc.start();
            }
        }
    }

    public void updateTable() {
        if (sc != null && sc.isSleeping()) sc.interrupt();
        if (gui != null) gui.updateTable();
    }

    @Override
    public String getIconKey() {
        return "gui.images.config.eventmanager";
    }

    @Override
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
            view.setContent(gui = new MainGui(this));

        }
        activateAction.setSelected(true);
        JDGui.getInstance().setContent(view);
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        menu.add(activateAction);
        return menu;
    }

    @Override
    public boolean initAddon() {
        logger.info("Schedule Init: OK");
        running = true;
        sc = new Schedulercheck();

        if (actions.size() > 0) {
            sc.start();
        }

        return true;
    }

    @Override
    public void setGuiEnable(boolean b) {
        if (b) {
            showGui();
        } else {
            if (view != null) view.close();
        }
    }

    @Override
    public void onExit() {
        saveActions();
        running = false;
        if (sc != null && sc.isSleeping()) sc.interrupt();
        sc = null;
    }

    public class Schedulercheck extends Thread {
        private Date today;
        private SimpleDateFormat time;
        private SimpleDateFormat date;
        private ArrayList<Actions> tmpactions = null;
        private ArrayList<Executions> tmpexe = null;
        private boolean sleeping = false;

        public Schedulercheck() {
            super("Schedulercheck");
            time = new SimpleDateFormat("HH:mm");
            date = new SimpleDateFormat("dd.MM.yyyy");
            tmpactions = new ArrayList<Actions>();
            tmpexe = new ArrayList<Executions>();
        }

        public boolean isSleeping() {
            synchronized (this) {
                return sleeping;
            }
        }

        private boolean updateTimer(Actions a, long curtime) {
            /* update timer of the action */
            if (a.getRepeat() == 0) {
                /* we do not have to update timers for disabled repeats */
            } else {
                /* we have to update timer */
                long timestamp = a.getDate().getTime();
                long currenttime = curtime;
                if (timestamp <= currenttime) {
                    a.setAlreadyHandled(false);
                    /* remove secs and milisecs */
                    currenttime = (currenttime / (60 * 1000));
                    currenttime = currenttime * (60 * 1000);
                    long add = a.getRepeat() * 60 * 1000l;
                    /* timestamp expired , set timestamp */
                    while (timestamp <= currenttime) {
                        timestamp += add;
                    }
                    Calendar newrepeat = Calendar.getInstance();
                    newrepeat.setTimeInMillis(timestamp);
                    a.setDate(newrepeat.getTime());
                    return true;
                }
            }
            return false;
        }

        @Override
        public void run() {
            try {
                logger.finest("Scheduler: start");
                while (running) {
                    logger.finest("Scheduler: checking");
                    /* getting current date and time */
                    long currenttime = System.currentTimeMillis();
                    today = new Date(currenttime);
                    String todaydate = date.format(today);
                    String todaytime = time.format(today);
                    boolean savechanges = false;
                    /* check all scheduler actions */
                    synchronized (LOCK) {
                        tmpactions.clear();
                        tmpactions.addAll(actions);
                    }
                    for (Actions a : tmpactions) {
                        /* check if we have to start the scheduler action */
                        if (a.isEnabled() && todaydate.equals(date.format(a.getDate())) && todaytime.equals(time.format(a.getDate()))) {
                            if (!a.wasAlreadyHandled()) {
                                a.setAlreadyHandled(true);
                                /* lets execute the action */
                                synchronized (LOCK) {
                                    tmpexe.clear();
                                    tmpexe.addAll(a.getExecutions());
                                }
                                for (Executions e : tmpexe) {
                                    logger.finest("Execute: " + e.getModule().getTranslation());
                                    e.exceute();
                                }
                            }
                        }
                        /* update timer */
                        if (updateTimer(a, currenttime)) savechanges = true;
                    }
                    if (savechanges) {
                        saveActions();
                    } else {
                        updateTable();
                    }
                    /* wait a minute and check again */
                    synchronized (this) {
                        sleeping = true;
                    }
                    try {
                        sleep(60000);
                    } catch (InterruptedException e) {
                    }
                    synchronized (this) {
                        sleeping = false;
                    }
                }
                logger.finest("Scheduler: stop");
            } catch (Exception e) {
                logger.severe("Scheduler: died!!");
                JDLogger.exception(e);
            }
        }
    }
}