package org.jdownloader.extensions.eventscripter;

import java.util.ArrayList;
import java.util.HashMap;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;

public class IntervalController {

    private EventScripterExtension extension;
    private Thread                 scheduler;
    private ArrayList<ScriptEntry> intervalEntries;
    private LogSource              logger;

    public IntervalController(EventScripterExtension eventScripterExtension) {
        this.extension = eventScripterExtension;
        logger = extension.getLogger();
        update();
    }

    public synchronized void update() {
        ArrayList<ScriptEntry> ie = new ArrayList<ScriptEntry>();

        for (ScriptEntry e : extension.getEntries()) {
            if (e.getEventTrigger() == EventTrigger.INTERVAL && e.isEnabled()) {
                ie.add(e);
            }
        }
        if (ie.size() > 0) {
            this.intervalEntries = ie;
            if (scheduler == null) {
                scheduler = new Thread("EventScripterScheduler") {
                    public void run() {
                        while (true) {

                            long minwait = Long.MAX_VALUE;
                            for (ScriptEntry e : intervalEntries) {
                                if (e.getEventTrigger() == EventTrigger.INTERVAL && e.isEnabled()) {
                                    try {
                                        HashMap<String, Object> settings = e.getEventTriggerSettings();
                                        long interval = 1000;

                                        if (settings.get("interval") != null) {
                                            interval = Math.max(1000, ((Number) settings.get("interval")).longValue());
                                        }

                                        Object last = settings.get("lastFire");
                                        long lastTs = 0l;
                                        if (last != null) {
                                            lastTs = ((Number) last).longValue();
                                        }
                                        long waitFor = interval - (System.currentTimeMillis() - lastTs);

                                        if (waitFor <= 0) {
                                            fire(e, interval);
                                            settings.put("lastFire", System.currentTimeMillis());
                                            waitFor = interval;

                                        }
                                        minwait = Math.min(minwait, waitFor);

                                    } catch (Throwable e2) {
                                        logger.log(e2);
                                    }
                                }
                            }
                            try {
                                Thread.sleep(minwait);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }

                    };
                };
                scheduler.setDaemon(true);
                scheduler.start();
            }
        } else {
            if (scheduler != null) {
                scheduler.interrupt();
                scheduler = null;
            }

        }
    }

    protected void fire(ScriptEntry script, long interval) {

        if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript())) {
            try {
                HashMap<String, Object> props = new HashMap<String, Object>();
                props.put("interval", interval);

                extension.runScript(script, props);
            } catch (Throwable e) {
                logger.log(e);
            }
        }
    }

}
