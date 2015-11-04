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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.DownloadLinkAggregator;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

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
 * Content (1024 bytes): version (currently 1) - integer (4 bytes) bps in bytes/s - long (8 bytes) total bytes - long (8 bytes) loaded bytes
 * - long (8 bytes) remaining bytes - long (8 bytes) eta in seconds - long (8 bytes) running downloads - long (8 bytes) open connections -
 * long (8 bytes) running packages - long (8 bytes)
 *
 */
public class SharedMemoryState {
    // singleton
    private static SharedMemoryState INSTANCE     = new SharedMemoryState();
    // shared memory version
    private static final int         VERSION      = 1;
    // update time to fill shared memory (ms)
    private static final int         SLEEP_TIME   = 1000;
    // shared memory name
    private String                   sharedName   = "JDownloader";
    // native handlers
    private HANDLE                   sharedFile   = null;
    private Pointer                  sharedMemory = null;
    // update thread
    private volatile Thread          updateThread = null;

    // returns the one and only instance
    public static SharedMemoryState getInstance() {
        return INSTANCE;
    }

    // create shared memory segment and update thread, only available in Windows OS
    public synchronized void startUpdates() {
        if (!CrossSystem.isWindows() || (sharedFile != null)) {
            return;
        }

        try {
            // create shared memory segment
            sharedFile = Kernel32.INSTANCE.CreateFileMapping(WinBase.INVALID_HANDLE_VALUE, null, WinNT.PAGE_READWRITE, 0, 1024, sharedName);
            sharedMemory = Kernel32.INSTANCE.MapViewOfFile(sharedFile, WinNT.SECTION_MAP_WRITE, 0, 0, 1024);
            sharedMemory.setInt(0, VERSION);

            // start periodically update thread
            updateThread = new Thread() {
                @Override
                public void run() {
                    while (Thread.currentThread() == updateThread) {
                        try {
                            sleep(SLEEP_TIME);
                            SharedMemoryState.getInstance().updateState();
                        } catch (Throwable e) {
                            // e.printStackTrace();
                        }
                    }
                }
            };

            updateThread.setName("SharedMemoryStateThread");
            updateThread.setDaemon(true);
            updateThread.start();

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
        } catch (Throwable th) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(th);
        }
    }

    // stop thread and clear shared memory segment (if hold open by other process)
    public synchronized void stopUpdates() {
        if (updateThread != null) {
            Thread th = updateThread;
            updateThread = null;
            th.interrupt();
        }

        try {
            if ((sharedFile != null) && !WinBase.INVALID_HANDLE_VALUE.equals(sharedFile)) {
                sharedMemory.setMemory(4, 1024, (byte) 0); // clear memory
                Kernel32.INSTANCE.CloseHandle(sharedFile);
            }
        } catch (Throwable th) {
            // th.printStackTrace();
        }
    }

    // write current state into shared memory
    public synchronized void updateState() {
        if (sharedFile == null) {
            return;
        }

        ByteBuffer buf = ByteBuffer.allocate(128);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        int bps = Math.max(0, DownloadWatchDog.getInstance().getDownloadSpeedManager().getSpeed());
        long eta = 0;
        long totalDl = 0;
        long curDl = 0;
        long remain = 0;

        if (DownloadsTable.getInstance() != null) {
            DownloadLinkAggregator dla = new DownloadLinkAggregator(DownloadsTable.getInstance().getSelectionInfo(false, false));

            totalDl = dla.getTotalBytes();
            curDl = dla.getBytesLoaded();
            remain = Math.max(0, totalDl - curDl);
            eta = Math.max(0, dla.getEta());
        }

        List<ExtractionController> jobs = ExtractionExtension.getInstance().getJobQueue().getJobs();
        for (final ExtractionController controller : jobs) {
            if (controller.getExtractionProgress() != null) {
                eta = Math.max(eta, controller.getExtractionProgress().getETA());
            }
        }

        buf.putInt(VERSION);
        buf.putLong(bps);
        buf.putLong(totalDl);
        buf.putLong(curDl);
        buf.putLong(remain);
        buf.putLong(eta);
        buf.putLong(DownloadWatchDog.getInstance().getActiveDownloads());
        buf.putLong(DownloadWatchDog.getInstance().getDownloadSpeedManager().connections());
        buf.putLong(DownloadWatchDog.getInstance().getRunningFilePackages().size());

        sharedMemory.write(0, buf.array(), 0, 128);
    }
}
