//jDownloader - Downloadmanager
//Copyright (C) 2011 JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.folderwatch.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import jd.controlling.JDLogger;

public class MonitoringScheduler extends Thread {

    private class ScheduleEntry {

        private String  filename;

        private long    milliseconds;

        private boolean isExpired = false;

        public boolean isExpired() {
            return isExpired;
        }

        public void setExpired(boolean isExpired) {
            this.isExpired = isExpired;
        }

        public ScheduleEntry(String filename, long milliseconds) {
            setFilename(filename);
            setMilliseconds(milliseconds);
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public long getMilliseconds() {
            return milliseconds;
        }

        public void setMilliseconds(long milliseconds) {
            this.milliseconds = milliseconds;
        }
    }

    private ArrayList<ScheduleEntry>          entries   = new ArrayList<ScheduleEntry>();

    private Logger                            logger    = JDLogger.getLogger();

    private final String                      LOGGER_PREFIX;

    private boolean                           done      = false;

    private ArrayList<FileMonitoringListener> listeners = new ArrayList<FileMonitoringListener>();

    private int                               timeout   = 5;

    public MonitoringScheduler(ArrayList<FileMonitoringListener> listeners) {
        this(listeners, "");
    }

    public MonitoringScheduler(ArrayList<FileMonitoringListener> listeners, String loggerprefix) {
        this.listeners = listeners;
        this.LOGGER_PREFIX = loggerprefix;
    }

    public synchronized void schedule(String filename) {
        if (!allExpired()) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }

        ScheduleEntry entry = findByFilename(filename);

        if (entry != null) {
            entry.setMilliseconds(new Date().getTime());
        } else {
            entry = new ScheduleEntry(filename, new Date().getTime());
        }

        entries.add(entry);

        notify();
    }

    private ScheduleEntry findByFilename(String filename) {
        for (ScheduleEntry entry : entries) {
            if (entry.getFilename().equals(filename)) { return entry; }
        }

        return null;
    }

    public void stopScheduling() {
        this.done = true;
    }

    public void startScheduling() {
        this.done = false;
        this.start();
    }

    public boolean allExpired() {
        for (ScheduleEntry entry : entries) {
            if (!entry.isExpired()) { return false; }
        }

        return true;
    }

    public void run() {
        logger.info(LOGGER_PREFIX + "Monitoring Scheduler started");

        while (!done) {
            synchronized (this) {
                if (allExpired()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }

                for (int i = 0; i < entries.size(); i++) {
                    ScheduleEntry entry = entries.get(i);

                    if (!entry.isExpired() && ((entry.getMilliseconds() + (long) (timeout * 1000)) < new Date().getTime())) {
                        entry.setExpired(true);

                        String filename = entry.getFilename();

                        logger.info(LOGGER_PREFIX + filename + " ready");

                        for (FileMonitoringListener listener : listeners) {
                            listener.onMonitoringFileCreate(filename);
                        }
                    }
                }

                notify();
            }

        }

        logger.info(LOGGER_PREFIX + "Monitoring Scheduler stopped");
    }
}
