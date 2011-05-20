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

package org.jdownloader.extensions.schedule;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import jd.controlling.JDLogger;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.ClassFinder;
import jd.pluginloader.VirtualClass;
import jd.plugins.AddonPanel;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.schedule.translate.T;

public class ScheduleExtension extends AbstractExtension<ScheduleConfig> implements ActionListener {

    private ArrayList<Actions>                  actions;

    private ArrayList<SchedulerModuleInterface> modules;

    private SchedulerView                       view;

    private MainGui                             gui;

    private Schedulercheck                      sc      = null;

    private boolean                             running = false;

    private MenuAction                          activateAction;

    public static final Object                  LOCK    = new Object();

    public ExtensionConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public ScheduleExtension() throws StartException {
        super(T._.jd_plugins_optional_schedule_schedule());
    }

    private void initModules() {
        modules = new ArrayList<SchedulerModuleInterface>();
        try {
            ArrayList<String> added = new ArrayList<String>();
            for (final VirtualClass vc : ClassFinder.getClasses("jd.plugins.optional.schedule.modules", getClass().getClassLoader())) {
                try {
                    // uncached plugin loading
                    Class<?> c = vc.loadClass();

                    final SchedulerModule help = c.getAnnotation(SchedulerModule.class);
                    if (help == null) {
                        logger.info("Scheduler: Skipped " + c + " due to missing annotation!");
                        continue;
                    } else if (added.contains(c.toString())) {
                        logger.info("Scheduler: Skipped " + c + " because its already loaded!");
                        continue;
                    }
                    added.add(c.toString());

                    modules.add((SchedulerModuleInterface) c.getConstructor().newInstance());
                } catch (Throwable e) {
                    JDLogger.exception(e);
                }
            }
            Collections.sort(modules, new Comparator<SchedulerModuleInterface>() {
                public int compare(SchedulerModuleInterface o1, SchedulerModuleInterface o2) {
                    return o1.getTranslation().compareToIgnoreCase(o2.getTranslation());
                }
            });
        } catch (Throwable e) {
            JDLogger.exception(e);
        }
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
        return "event";
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
                    if (event.getEventID() == SwitchPanelEvent.ON_REMOVE) activateAction.setSelected(false);
                }

            });
            view.setContent(gui = new MainGui(this));

        }
        activateAction.setSelected(true);
        JDGui.getInstance().setContent(view, true);
    }

    public void setGuiEnable(boolean b) {
        if (b) {
            showGui();
        } else {
            if (view != null) view.close();
        }
    }

    public class Schedulercheck extends Thread {
        private Date                  today;
        private SimpleDateFormat      time;
        private SimpleDateFormat      date;
        private ArrayList<Actions>    tmpactions = null;
        private ArrayList<Executions> tmpexe     = null;
        private boolean               sleeping   = false;

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

    @Override
    protected void stop() throws StopException {
        saveActions();
        running = false;
        if (sc != null && sc.isSleeping()) sc.interrupt();
        sc = null;
    }

    @Override
    protected void start() throws StartException {
        actions = this.getPluginConfig().getGenericProperty("Scheduler_Actions", new ArrayList<Actions>());
        if (actions == null) {
            actions = new ArrayList<Actions>();
            saveActions();
        }

        initModules();

        activateAction = new MenuAction("scheduler", 0);
        activateAction.setActionListener(this);
        activateAction.setIcon(this.getIconKey());
        activateAction.setSelected(false);

        logger.info("Schedule Init: OK");
        running = true;
        sc = new Schedulercheck();

        if (actions.size() > 0) {
            sc.start();
        }

    }

    @Override
    public String getConfigID() {
        return "scheduler";
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return T._.jd_plugins_optional_schedule_schedule_description();
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        menu.add(activateAction);
        return menu;
    }

    @Override
    protected void initExtension() throws StartException {
    }
}