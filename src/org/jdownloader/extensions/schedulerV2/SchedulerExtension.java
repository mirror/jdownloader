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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jd.plugins.AddonPanel;

import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntry;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntryStorable;
import org.jdownloader.extensions.schedulerV2.translate.SchedulerTranslation;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;

public class SchedulerExtension extends AbstractExtension<SchedulerConfig, SchedulerTranslation> implements MenuExtenderHandler, Runnable {

    private SchedulerConfigPanel     configPanel;
    private ScheduledExecutorService scheduler;
    private Object                   lock = new Object();

    @Override
    public boolean isHeadlessRunnable() {
        return false;
    }

    public SchedulerConfigPanel getConfigPanel() {
        boolean b = getSettings().isBlablablaEnabled();
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public SchedulerExtension() throws StartException {
        setTitle(_.title());

    }

    @Override
    public String getIconKey() {
        return IconKey.ICON_WAIT;
    }

    @Override
    protected void stop() throws StopException {

        MenuManagerMainToolbar.getInstance().unregisterExtender(this);
        MenuManagerMainmenu.getInstance().unregisterExtender(this);

        synchronized (lock) {
            if (scheduler != null) {
                scheduler.shutdown();
                scheduler = null;
            }
        }
    }

    @Override
    protected void start() throws StartException {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                getConfigPanel().updateLayout();
            }
        };
        // The extension can add items to the main toolbar and the main menu.
        MenuManagerMainToolbar.getInstance().registerExtender(this);
        MenuManagerMainmenu.getInstance().registerExtender(this);

        synchronized (lock) {
            scheduler = Executors.newScheduledThreadPool(1);
            // start scheduler and align to second = 0
            scheduler.scheduleAtFixedRate(this, 60 - Calendar.getInstance().get(Calendar.SECOND), 60l, TimeUnit.SECONDS);
        }

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

    private boolean needsRun(ScheduleEntryStorable plan) {
        String timeType = plan.getTimeType();
        if (timeType.equals("ONLYONCE")) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(plan.getTimestamp() * 1000l);
            if (Math.abs(c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) < 30 * 1000l - 1) {
                return true;
            }
        } else if (timeType.equals("HOURLY")) {
            Calendar event = Calendar.getInstance();
            event.setTimeInMillis(plan.getTimestamp() * 1000l);
            Calendar c = Calendar.getInstance();
            c.set(Calendar.MINUTE, event.get(Calendar.MINUTE));
            if (Math.abs(c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) < 30 * 1000l - 1) {
                return true;
            }
        } else if (timeType.equals("DAILY")) {
            Calendar event = Calendar.getInstance();
            event.setTimeInMillis(plan.getTimestamp() * 1000l);
            Calendar c = Calendar.getInstance();
            c.set(Calendar.MINUTE, event.get(Calendar.MINUTE));
            c.set(Calendar.HOUR_OF_DAY, event.get(Calendar.HOUR_OF_DAY));
            if (Math.abs(c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) < 30 * 1000l - 1) {
                return true;
            }
        } else if (timeType.equals("WEEKLY")) {
            Calendar event = Calendar.getInstance();
            event.setTimeInMillis(plan.getTimestamp() * 1000l);
            Calendar c = Calendar.getInstance();
            c.set(Calendar.MINUTE, event.get(Calendar.MINUTE));
            c.set(Calendar.HOUR_OF_DAY, event.get(Calendar.HOUR_OF_DAY));
            c.set(Calendar.DAY_OF_WEEK, event.get(Calendar.DAY_OF_WEEK));
            if (Math.abs(c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) < 30 * 1000l - 1) {
                return true;
            }
        } else if (timeType.equals("CHOOSEINTERVAL")) {
            long nowMin = Calendar.getInstance().getTimeInMillis() / (60 * 1000);
            long startMin = plan.getTimestamp() / 60;
            long intervalMin = 60 * plan.getIntervalHour() + plan.getIntervalMin();
            if ((nowMin - startMin) % intervalMin == 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run() {
        ArrayList<ScheduleEntryStorable> plans = CFG_SCHEDULER.CFG.getEntryList();
        for (ScheduleEntryStorable storableEntry : plans) {
            if (needsRun(storableEntry)) {
                ScheduleEntry entry = new ScheduleEntry(storableEntry);
                entry.getAction().execute(entry.getActionParameter());
            }
        }
    }

}