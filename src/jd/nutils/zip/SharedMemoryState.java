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

package jd.nutils.zip;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.nutils.Formatter;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.DownloadLinkAggregator;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.ExtractionProgress;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;

/**
 * Creates a shared memory area that holds the current JDownloader state, like B/s, ETA, ...
 *
 * Shared memory name: JDownloader Update interval: 1000 ms
 *
 * Content (1024 bytes): version (currently 2) - integer (4 bytes) bps in bytes/s - long (8 bytes) total bytes - long (8 bytes) loaded bytes
 * - long (8 bytes) remaining bytes - long (8 bytes) eta in seconds - long (8 bytes) running downloads - long (8 bytes) open connections -
 * long (8 bytes) running packages - long (8 bytes) length of eta string - integer (4 bytes) eta string - variable length (not 0 terminated)
 *
 * @author jadevwin
 */
public class SharedMemoryState implements GenericConfigEventListener<Boolean> {
    // singleton
    private final static SharedMemoryState INSTANCE     = new SharedMemoryState();
    // shared memory version
    private static final int               VERSION      = 2;
    // update time to fill shared memory (ms)
    private static final int               SLEEP_TIME   = 1000;
    // shared memory name
    private static final String            sharedName   = "JDownloader";

    // update thread
    private final AtomicReference<Thread>  updateThread = new AtomicReference<Thread>(null);
    private final LogSource                logger;

    private SharedMemoryState() {
        logger = LogController.CL(false);
        // register shutdown handler to stop thread and clear shared memory
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public void setHookPriority(int priority) {
                super.setHookPriority(Integer.MAX_VALUE);
            }

            @Override
            public String toString() {
                return "ShutdownEvent: SharedMemoryState";
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                SharedMemoryState.getInstance().stopUpdates();
            }
        });
        CFG_GENERAL.SHARED_MEMORY_STATE_ENABLED.getEventSender().addListener(this);
    }

    // returns the one and only instance
    public static SharedMemoryState getInstance() {
        return INSTANCE;
    }

    // create shared memory segment and update thread, only available in Windows OS
    public synchronized void startUpdates() {
        final Thread currentThread = updateThread.get();
        if (CrossSystem.isWindows() && (currentThread == null || !currentThread.isAlive())) {
            HANDLE sharedFile = null;
            try {
                // create shared memory segment
                sharedFile = Kernel32.INSTANCE.CreateFileMapping(WinBase.INVALID_HANDLE_VALUE, null, WinNT.PAGE_READWRITE, 0, 1024, sharedName);
                final HANDLE finalSharedFile = sharedFile;
                final Pointer finalSharedMemory = Kernel32.INSTANCE.MapViewOfFile(sharedFile, WinNT.SECTION_MAP_WRITE, 0, 0, 1024);
                finalSharedMemory.setInt(0, VERSION);

                // start periodically update thread
                final Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            final ByteBuffer byteBuffer = ByteBuffer.allocate(128);
                            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                            while (Thread.currentThread() == updateThread.get()) {
                                try {
                                    sleep(SLEEP_TIME);
                                    SharedMemoryState.getInstance().updateState(finalSharedMemory, byteBuffer);
                                } catch (InterruptedException th) {
                                    break;
                                }
                            }
                        } catch (final Throwable th) {
                            logger.log(th);
                        } finally {
                            closeSharedFile(finalSharedFile, finalSharedMemory);
                            updateThread.compareAndSet(Thread.currentThread(), null);
                        }
                    }
                };
                thread.setName("SharedMemoryStateThread");
                thread.setDaemon(true);
                updateThread.set(thread);
                thread.start();
            } catch (Throwable th) {
                logger.log(th);
                closeSharedFile(sharedFile, null);
            }
        }
    }

    protected void closeSharedFile(final HANDLE sharedFile, final Pointer sharedMemory) {
        try {
            if ((sharedFile != null) && !WinBase.INVALID_HANDLE_VALUE.equals(sharedFile)) {
                if (sharedMemory != null) {
                    sharedMemory.setMemory(4, 1024, (byte) 0); // clear memory
                }
                Kernel32.INSTANCE.CloseHandle(sharedFile);
            }
        } catch (Throwable th) {
            logger.log(th);
        }
    }

    // stop thread and clear shared memory segment (if hold open by other process)
    public void stopUpdates() {
        final Thread thread = updateThread.getAndSet(null);
        if (thread != null) {
            thread.interrupt();
        }
    }

    // write current state into shared memory
    protected void updateState(Pointer sharedMemory, ByteBuffer buf) {
        final int bps = Math.max(0, DownloadWatchDog.getInstance().getDownloadSpeedManager().getSpeed());
        final DownloadLinkAggregator dla = new DownloadLinkAggregator(DownloadController.getInstance().getSelectionInfo());
        final long totalDl = dla.getTotalBytes();
        final long curDl = dla.getBytesLoaded();
        final long remain = Math.max(0, totalDl - curDl);
        long eta = dla.getEta();

        final List<ExtractionController> jobs = ExtractionExtension.getInstance().getJobQueue().getJobs();
        for (final ExtractionController controller : jobs) {
            final ExtractionProgress progress = controller.getExtractionProgress();
            if (progress != null) {
                eta = Math.max(eta, progress.getETA() / 1000);
            }
        }
        buf.clear();
        buf.putInt(VERSION);
        buf.putLong(bps);
        buf.putLong(totalDl);
        buf.putLong(curDl);
        buf.putLong(remain);
        buf.putLong(Math.max(0, eta));
        buf.putLong(DownloadWatchDog.getInstance().getActiveDownloads());
        buf.putLong(DownloadWatchDog.getInstance().getDownloadSpeedManager().connections());
        buf.putLong(DownloadWatchDog.getInstance().getRunningFilePackages().size());

        // formatted eta string
        byte[] etas;
        try {
            etas = Formatter.formatSeconds(eta).replace(":", " ").getBytes("ISO-8859-1"); // use standard charset
            buf.putInt(etas.length);
            buf.put(etas);
        } catch (UnsupportedEncodingException e) {
            logger.log(e);
            buf.putInt(0);
        }

        sharedMemory.write(0, buf.array(), 0, 128);
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (Boolean.TRUE.equals(newValue)) {
            startUpdates();
        } else {
            stopUpdates();
        }
    }
}
