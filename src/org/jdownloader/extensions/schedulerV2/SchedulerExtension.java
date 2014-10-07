//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

package org.jdownloader.extensions.schedulerV2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jd.plugins.AddonPanel;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper.TIME_OPTIONS;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper.WEEKDAY;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntry;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntryStorable;
import org.jdownloader.extensions.schedulerV2.translate.SchedulerTranslation;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;

public class SchedulerExtension extends AbstractExtension<SchedulerConfig, SchedulerTranslation> implements MenuExtenderHandler, Runnable {

    private SchedulerConfigPanel                configPanel;
    private ScheduledExecutorService            scheduler;
    private Object                              lock            = new Object();
    private CopyOnWriteArrayList<ScheduleEntry> scheduleEntries = new CopyOnWriteArrayList<ScheduleEntry>();
    private ShutdownEvent                       shutDownEvent   = new ShutdownEvent() {

                                                                    @Override
                                                                    public void onShutdown(ShutdownRequest shutdownRequest) {
                                                                        saveScheduleEntries();
                                                                    }
                                                                };

    @Override
    public boolean isHeadlessRunnable() {
        return false;
    }

    public void saveScheduleEntries() {
        List<ScheduleEntryStorable> scheduleStorables = new ArrayList<ScheduleEntryStorable>();
        for (ScheduleEntry entry : getScheduleEntries()) {
            scheduleStorables.add(entry.getStorable());
        }
        CFG_SCHEDULER.CFG.setEntryList(scheduleStorables);
    }

    public SchedulerConfigPanel getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public SchedulerExtension() throws StartException {
        setTitle(_.title());
    }

    public List<ScheduleEntry> getScheduleEntries() {
        return scheduleEntries;
    }

    @Override
    public String getIconKey() {
        return IconKey.ICON_WAIT;
    }

    @Override
    protected void stop() throws StopException {
        saveScheduleEntries();
        MenuManagerMainToolbar.getInstance().unregisterExtender(this);
        MenuManagerMainmenu.getInstance().unregisterExtender(this);
        ShutdownController.getInstance().removeShutdownEvent(shutDownEvent);
        synchronized (lock) {
            if (scheduler != null) {
                scheduler.shutdown();
                scheduler = null;
            }
        }
    }

    @Deprecated
    private void fixOldDailyWeekly() {
        // TODO remove in near future
        List<ScheduleEntryStorable> scheduleStorables = new LinkedList<ScheduleEntryStorable>(CFG_SCHEDULER.CFG.getEntryList());
        for (ScheduleEntryStorable entry : scheduleStorables) {
            if (entry._getTimeType().equals(TIME_OPTIONS.DAILY)) {
                entry._setTimeType(TIME_OPTIONS.SPECIFICDAYS);
                entry._setSelectedDays(new ArrayList<WEEKDAY>(ActionHelper.dayMap.values()));
            } else if (entry._getTimeType().equals(TIME_OPTIONS.WEEKLY)) {
                entry._setTimeType(TIME_OPTIONS.SPECIFICDAYS);
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(entry.getTimestamp() * 1000l);
                ArrayList<WEEKDAY> day = new ArrayList<WEEKDAY>();
                day.add(ActionHelper.dayMap.get(c.get(Calendar.DAY_OF_WEEK)));
                entry._setSelectedDays(day);
            }
        }
        CFG_SCHEDULER.CFG.setEntryList(scheduleStorables);
    }

    @Override
    protected void start() throws StartException {
        fixOldDailyWeekly();

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                getConfigPanel().updateLayout();
            }
        };

        // The extension can add items to the main toolbar and the main menu.
        MenuManagerMainToolbar.getInstance().registerExtender(this);
        MenuManagerMainmenu.getInstance().registerExtender(this);

        loadScheduleEntries();
        getConfigPanel().getTableModel().updateDataModel();

        ShutdownController.getInstance().addShutdownEvent(shutDownEvent);
        synchronized (lock) {
            scheduler = Executors.newScheduledThreadPool(1);
            // start scheduler and align to second = 0
            scheduler.scheduleAtFixedRate(this, 60 - Calendar.getInstance().get(Calendar.SECOND), 60l, TimeUnit.SECONDS);
        }

    }

    public void replaceScheduleEntry(long oldID, ScheduleEntry newEntry) {
        if (newEntry != null) {
            int pos = -1;
            for (int i = 0; i < scheduleEntries.size(); i++) {
                if (oldID == scheduleEntries.get(i).getID()) {
                    pos = i;
                    break;
                }
            }

            if (pos > -1) {
                scheduleEntries.remove(pos);
                scheduleEntries.add(pos, newEntry);
                getConfigPanel().getTableModel().updateDataModel();
            }
        }
    }

    public void removeScheduleEntry(ScheduleEntry entry) {
        if (entry != null) {
            scheduleEntries.remove(entry);
            getConfigPanel().getTableModel().updateDataModel();
        }
    }

    public void addScheduleEntry(ScheduleEntry entry) {
        if (entry != null) {
            scheduleEntries.addIfAbsent(entry);
            getConfigPanel().getTableModel().updateDataModel();
        }
    }

    private void loadScheduleEntries() {
        List<ScheduleEntryStorable> scheduleStorables = CFG_SCHEDULER.CFG.getEntryList();
        CopyOnWriteArrayList<ScheduleEntry> scheduleEntries = new CopyOnWriteArrayList<ScheduleEntry>();
        for (ScheduleEntryStorable storable : scheduleStorables) {
            try {
                scheduleEntries.add(new ScheduleEntry(storable));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.scheduleEntries = scheduleEntries;
    }

    @Override
    public String getDescription() {
        return _.description();
    }

    @Override
    public AddonPanel<SchedulerExtension> getGUI() {
        // if you want an own t
        return null;
    }

    @Override
    protected void initExtension() throws StartException {

        if (!Application.isHeadless()) {
            configPanel = new SchedulerConfigPanel(this);
        }
    }

    @Override
    public boolean isQuickToggleEnabled() {
        return true;
    }

    @Override
    public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {

        return null;
    }

    private boolean needsRun(ScheduleEntry plan) {
        if (!plan.isEnabled()) {
            return false;
        }
        TIME_OPTIONS timeType = plan.getStorable()._getTimeType();
        switch (timeType) {
        case ONLYONCE: {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(plan.getTimestamp() * 1000l);
            if (Math.abs(c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) < 30 * 1000l) {
                return true;
            }
        }
            break;
        case SPECIFICDAYS: {
            Calendar c = Calendar.getInstance();
            if (!plan.getSelectedDays().contains(ActionHelper.dayMap.get(c.get(Calendar.DAY_OF_WEEK)))) {
                return false;
            }
            // check time
            c.setTimeInMillis(plan.getTimestamp() * 1000l);
            if (Math.abs(c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) < 30 * 1000l) {
                return true;
            }
        }
            break;

        case HOURLY: {
            Calendar event = Calendar.getInstance();
            event.setTimeInMillis(plan.getTimestamp() * 1000l);
            Calendar c = Calendar.getInstance();
            c.set(Calendar.MINUTE, event.get(Calendar.MINUTE));
            if (Math.abs(c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) < 30 * 1000l - 1) {
                return true;
            }
        }
            break;
        case DAILY: {
            // TODO remove me
            Calendar event = Calendar.getInstance();
            event.setTimeInMillis(plan.getTimestamp() * 1000l);
            Calendar c = Calendar.getInstance();
            c.set(Calendar.MINUTE, event.get(Calendar.MINUTE));
            c.set(Calendar.HOUR_OF_DAY, event.get(Calendar.HOUR_OF_DAY));
            if (Math.abs(c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) < 30 * 1000l - 1) {
                return true;
            }
        }
            break;
        case WEEKLY: {
            // TODO remove me
            Calendar event = Calendar.getInstance();
            event.setTimeInMillis(plan.getTimestamp() * 1000l);
            Calendar c = Calendar.getInstance();
            c.set(Calendar.MINUTE, event.get(Calendar.MINUTE));
            c.set(Calendar.HOUR_OF_DAY, event.get(Calendar.HOUR_OF_DAY));
            c.set(Calendar.DAY_OF_WEEK, event.get(Calendar.DAY_OF_WEEK));
            if (Math.abs(c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) < 30 * 1000l - 1) {
                return true;
            }
        }
            break;
        case CHOOSEINTERVAL: {
            long nowMin = Calendar.getInstance().getTimeInMillis() / (60 * 1000);
            long startMin = plan.getTimestamp() / 60;
            long intervalMin = 60 * plan.getIntervalHour() + plan.getIntervalMinunte();
            if ((nowMin - startMin) % intervalMin == 0) {
                return true;
            }
        }
            break;
        }
        return false;
    }

    @Override
    public void run() {
        for (ScheduleEntry entry : getScheduleEntries()) {
            if (needsRun(entry)) {
                try {
                    entry.getAction().execute();
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

}