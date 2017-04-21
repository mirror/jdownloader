//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package org.jdownloader.extensions.antistandby;

import java.util.concurrent.atomic.AtomicBoolean;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.jna.windows.Kernel32;
import org.jdownloader.logging.LogController;

public class WindowsAntiStandby extends Thread implements Runnable {

    private final AtomicBoolean        lastEnabledState         = new AtomicBoolean(false);
    private final AtomicBoolean        lastDisplayRequiredState = new AtomicBoolean(false);
    private static final int           sleep                    = 5000;
    private final AntiStandbyExtension jdAntiStandby;

    public WindowsAntiStandby(final AntiStandbyExtension jdAntiStandby) {
        super();
        this.jdAntiStandby = jdAntiStandby;
        this.setDaemon(true);
        setName("WindowsAntiStandby");

    }

    @Override
    public void run() {
        final LogSource logger = LogController.CL(WindowsAntiStandby.class);
        try {
            while (jdAntiStandby.isAntiStandbyThread()) {
                enableAntiStandby(logger, jdAntiStandby.requiresAntiStandby());
                sleep(sleep);
            }
        } catch (Throwable e) {
            logger.log(e);
        } finally {
            try {
                enableAntiStandby(logger, false);
            } catch (final Throwable e) {
            } finally {
                logger.fine("JDAntiStandby: Terminated");
                logger.close();
            }
        }
    }

    private void enableAntiStandby(final LogSource logger, final boolean enabled) {
        final boolean displayRequired = jdAntiStandby.getSettings().isDisplayRequired();
        if (lastEnabledState.compareAndSet(!enabled, enabled) || lastDisplayRequiredState.compareAndSet(!displayRequired, displayRequired)) {
            if (enabled) {
                if (displayRequired) {
                    Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_CONTINUOUS | Kernel32.ES_SYSTEM_REQUIRED | Kernel32.ES_DISPLAY_REQUIRED);
                    logger.fine("JDAntiStandby: Start and Prevent Screensaver");
                } else {
                    Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_CONTINUOUS | Kernel32.ES_SYSTEM_REQUIRED);
                    logger.fine("JDAntiStandby: Start");
                }
            } else {
                Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
                logger.fine("JDAntiStandby: Stop");
            }
        }
    }

}
