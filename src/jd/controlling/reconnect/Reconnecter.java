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
package jd.controlling.reconnect;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.reconnect.ipcheck.IPController;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public final class Reconnecter {
    public static enum ReconnectResult {
        VALIDIP,
        FAILED,
        SUCCESSFUL,
        RUNNING
    }

    private static final Reconnecter INSTANCE             = new Reconnecter();
    private static final AtomicLong  lastReconnect        = new AtomicLong(0);
    private final AtomicBoolean      running              = new AtomicBoolean(false);
    private final boolean            debugNoRealReconnect = false;

    public static long getLastReconnect() {
        return lastReconnect.get();
    }

    public static Reconnecter getInstance() {
        return Reconnecter.INSTANCE;
    }

    private final ReconnectEventSender eventSender;
    private final ReconnectConfig      storage;

    private Reconnecter() {
        this.eventSender = new ReconnectEventSender();
        this.storage = JsonConfig.create(ReconnectConfig.class);
    }

    public static int getFailedCounter() {
        return getInstance().storage.getFailedCounter();
    }

    public ReconnectResult doReconnect() {
        return doReconnect(false);
    }

    public ReconnectResult doReconnect(final boolean forceReconnect) {
        if (!forceReconnect && !IPController.getInstance().isInvalidated()) {
            return ReconnectResult.VALIDIP;
        }
        if (!running.compareAndSet(false, true)) {
            return ReconnectResult.RUNNING;
        }
        ReconnectResult result = null;
        try {
            LogSource logger = LogController.CL(false);
            RouterPlugin plugin = null;
            final long startTime = System.currentTimeMillis();
            try {
                if (debugNoRealReconnect) {
                    result = ReconnectResult.SUCCESSFUL;
                } else {
                    logger.setAllowTimeoutFlush(false);
                    logger.info("Perform reconnect");
                    plugin = ReconnectPluginController.getInstance().getActivePlugin();
                    if (plugin == DummyRouterPlugin.getInstance()) {
                        throw new ReconnectException("Invalid Plugin");
                    }
                    this.eventSender.fireEvent(new ReconnecterEvent(ReconnecterEvent.Type.BEFORE, plugin));
                    logger.info("Try to reconnect: " + plugin);
                    int maxretries = storage.getMaxReconnectRetryNum();
                    if (maxretries < 0) {
                        maxretries = Integer.MAX_VALUE;
                    } else if (maxretries == 0) {
                        maxretries = 1;
                    }
                    for (int retry = 0; retry < maxretries; retry++) {
                        logger.info("Starting \"" + plugin + "\" #" + (retry + 1) + "/" + maxretries);
                        if (ReconnectPluginController.getInstance().doReconnect(plugin, logger)) {
                            result = ReconnectResult.SUCCESSFUL;
                            break;
                        }
                    }
                }
            } catch (Throwable e) {
                logger.log(e);
            } finally {
                if (result == null) {
                    result = ReconnectResult.FAILED;
                }
                switch (result) {
                case SUCCESSFUL:
                    logger.clear();
                    logger.info("Reconnect successful: " + plugin);
                    lastReconnect.set(System.currentTimeMillis());
                    counterSuccess(+1);
                    counterGlobalSuccess(+1);
                    storage.setFailedCounter(0);
                    break;
                case FAILED:
                    counterFailed(+1);
                    counterGlobalFailed(+1);
                    storage.setSuccessCounter(0);
                    break;
                }
                // reconnect takes at least 1000ms
                try {
                    Thread.sleep((Math.max(10, 1000 - (System.currentTimeMillis() - startTime))));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("Reconnect: " + result.name() + " with " + plugin);
                this.eventSender.fireEvent(new ReconnecterEvent(ReconnecterEvent.Type.AFTER, plugin, result));
            }
        } finally {
            running.set(false);
        }
        return result;
    }

    private void counterGlobalSuccess(int i) {
        storage.setGlobalSuccessCounter(storage.getGlobalSuccessCounter() + i);
    }

    private void counterSuccess(int i) {
        storage.setSuccessCounter(storage.getSuccessCounter() + i);
    }

    private void counterGlobalFailed(int i) {
        storage.setGlobalFailedCounter(storage.getGlobalFailedCounter() + i);
    }

    private void counterFailed(int i) {
        storage.setFailedCounter(storage.getFailedCounter() + i);
    }

    public ReconnectEventSender getEventSender() {
        return this.eventSender;
    }
}