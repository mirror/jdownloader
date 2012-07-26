/*
 * Copyright (C) 2012 Shashank Tulsyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jdownloader.extensions.neembuu;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import javax.swing.JPanel;

import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.download.RAFDownload;
import jpfm.FormatterEvent;
import jpfm.MountListener;
import jpfm.mount.Mount;
import jpfm.mount.MountParams.ParamType;
import jpfm.mount.MountParamsBuilder;
import jpfm.mount.Mounts;
import neembuu.diskmanager.DiskManager;
import neembuu.diskmanager.DiskManagerParams;
import neembuu.diskmanager.DiskManagers;
import neembuu.diskmanager.RegionStorageManager;
import neembuu.diskmanager.ResumeStateCallback;
import neembuu.rangearray.UnsyncRangeArrayCopy;
import neembuu.vfs.file.MonitoredHttpFile;
import neembuu.vfs.file.SeekableConnectionFile;
import neembuu.vfs.file.SeekableConnectionFileParams;
import neembuu.vfs.file.TroubleHandler;
import neembuu.vfs.progresscontrol.ThrottleFactory;
import neembuu.vfs.readmanager.ReadRequestState;
import neembuu.vfs.readmanager.impl.SeekableConnectionFileImplBuilder;

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.neembuu.gui.HttpFilePanel;
import org.jdownloader.extensions.neembuu.gui.VirtualFilesPanel;
import org.jdownloader.extensions.neembuu.newconnectionprovider.JD_HTTP_Download_Manager;

/**
 * 
 * @author Shashank Tulsyan
 * @author Coalado
 */
final class WatchAsYouDownloadSessionImpl implements WatchAsYouDownloadSession {
    // private final SeekableConnectionFile file;// not used
    private final MonitoredHttpFile    httpFile;
    private JPanel                     filePanel;
    private final HttpFilePanel        monitoredSeekableHttpFilePanel;
    private final NB_VirtualFileSystem virtualFileSystem;
    private final DownloadSession      jdds;
    private final Object               lock            = new Object();
    private volatile long              totalDownloaded = 0;

    static WatchAsYouDownloadSessionImpl makeNew(DownloadSession jdds) throws Exception {
        NB_VirtualFileSystem fileSystem = null;
        try {
            // fileSystem = (NB_VirtualFileSystem)
            // jdds.getDownloadLink().getFilePackage().getProperty("NB_VirtualFileSystem");
            fileSystem = NeembuuExtension.getInstance().getVirtualFileSystems().get(jdds.getDownloadLink().getFilePackage());
        } catch (ClassCastException cce) {

        }
        if (fileSystem == null) {
            fileSystem = NB_VirtualFileSystem.newInstance();
            // jdds.getDownloadLink().getFilePackage().setProperty("NB_VirtualFileSystem",
            // fileSystem);
            NeembuuExtension.getInstance().getVirtualFileSystems().put(jdds.getDownloadLink().getFilePackage(), fileSystem);
            jdds.getDownloadLink().getLivePlugin().getLogger().info("Using new filesystem " + fileSystem);
        } else {
            jdds.getDownloadLink().getLivePlugin().getLogger().info("Using previous created filesystem " + fileSystem);
        }
        // read the javadoc to know what this does
        // Briefly : sometimes the filesystem is hung because of big sized
        // requests or because a new connection cannot
        // be made. In such cases it decides
        // what must be done.
        TroubleHandler troubleHandler = new NBTroubleHandler(jdds);

        // Is used to create new connections at some arbitary offset
        // this is the only thing that neembuu requires JD to provide
        // all other things are handled by neembuu.
        // You will notice that the code is not much.
        // The same logic may be used to make a NewConnecitonProvider
        // for FTP and other protocols as the case maybe.
        final JD_HTTP_Download_Manager newConnectionProvider = new JD_HTTP_Download_Manager(jdds);

        ResumeStateCallback resumeStateCallback = new ResumeStateCallback() {
            public boolean resumeState(List<RegionStorageManager> previouslyDownloadedData) {
                if (newConnectionProvider.estimateCreationTime(1) >= Integer.MAX_VALUE) return false;
                // for rapidshare type of links clean the
                // download directory and start fresh

                // retain stuff for others
                return true;
            }
        };

        // JD team might like to change this to something they like.
        // This default diskmanager saves each chunk in a different file
        // for this reason, chunks are placed in a directory.
        // In the default implementation, I chose not to save in a single file
        // because that would require to also maintain progress information in a
        // separete file. The name of the file contains the offset from which
        // it starts, and size of the file tells us how much was downloaded.
        // Very important logs are also saved along with these files.
        // They are in html format, and I would strongly suggest if an alternate
        // implementation to this is provided, leave the html logs as they
        // are. 2 log files are created for every unique chunk,
        // and one for each file which logs overall working of differnt
        // chunks/regions. For this reason the download folder would contain
        // a lot of files. These logs are highly essential.
        DiskManagerParams dmp = new DiskManagerParams.Builder().setMaxReadQueueManagerThreadLogSize(2 * 1024 * 1024).setMaxReadHandlerThreadLogSize(100 * 1024).setMaxDownloadThreadLogSize(100 * 1024).setBaseStoragePath(new File(jdds.getDownloadLink().getFileOutput()).getParentFile().getAbsolutePath()).setResumeStateCallback(resumeStateCallback).build();
        DiskManager diskManager = DiskManagers.getDefaultManager(dmp);

        // throttle is a very unique speed measuring, and controlling unit.
        // Limiting download speed is crucial to prevent starvation of regions
        // which really require speed, and make the system respond quickly. The
        // main work of throttle is not to limit speed as much as it is to kill
        // connections to improve speed where it is actually required. This
        // makes a HUGE difference in watch as you download experience of the
        // user.
        // Making a decent throttle is one of the biggest challenges. The
        // program as such is simple, but the key issue is same approach doesn't
        // work for all files. For avi's you might need a different approach,
        // for mkv still another. This GeneralThrottle is however pretty decent
        // and should be able to handle most of the files.
        ThrottleFactory throttleFactory = ThrottleFactory.General.SINGLETON;

        // all paramters below are compulsary. None of these may be left
        // unspecified.
        SeekableConnectionFile file = SeekableConnectionFileImplBuilder.build(new SeekableConnectionFileParams.Builder().setDiskManager(diskManager).setTroubleHandler(troubleHandler).setFileName(jdds.getDownloadLink().getName()).setFileSize(jdds.getDownloadLink().getDownloadSize()).setNewConnectionProvider(newConnectionProvider).setParent(fileSystem.getRootDirectory()).setThrottleFactory(throttleFactory).build());

        MonitoredHttpFile httpFile = new MonitoredHttpFile(file, newConnectionProvider);
        final String mountLocation = fileSystem.getMountLocation(jdds);
        HttpFilePanel httpFilePanel = new HttpFilePanel(httpFile);
        WatchAsYouDownloadSessionImpl sessionImpl = new WatchAsYouDownloadSessionImpl(file, httpFile, httpFilePanel, fileSystem, jdds);
        jdds.setWatchAsYouDownloadSession(sessionImpl);
        JPanel virtualFilesPanel = VirtualFilesPanel.getOrCreate(jdds, mountLocation, httpFilePanel);
        sessionImpl.filePanel = virtualFilesPanel;
        jdds.getWatchAsYouDownloadSession().getHttpFilePanel().setVirtualPathOfFile(new File(mountLocation, jdds.getDownloadLink().getName()).getAbsolutePath());

        // mount might have been already initiated by some other downloadlink in
        // this same filepackage
        if (!sessionImpl.isMounted()) sessionImpl.mount(mountLocation);

        fileSystem.addSession(jdds);

        return sessionImpl;
    }

    WatchAsYouDownloadSessionImpl(SeekableConnectionFile file, MonitoredHttpFile httpFile, HttpFilePanel monitoredSeekableHttpFilePanel, NB_VirtualFileSystem virtualFileSystem, DownloadSession jdds) {
        // this.file = file;
        this.httpFile = httpFile;
        this.virtualFileSystem = virtualFileSystem;
        this.jdds = jdds;
        this.monitoredSeekableHttpFilePanel = monitoredSeekableHttpFilePanel;
    }

    public final long getTotalDownloaded() {
        totalDownloaded = updateTotalDownloaded();// update first
        return totalDownloaded;
    }

    // @Override
    public final NB_VirtualFileSystem getVirtualFileSystem() {
        return virtualFileSystem;
    }

    public final HttpFilePanel getHttpFilePanel() {
        return monitoredSeekableHttpFilePanel;
    }

    // @Override
    public final SeekableConnectionFile getSeekableConnectionFile() {
        return httpFile;
    }

    // @Override
    public JPanel getFilePanel() {
        return filePanel;
    }

    // @Override
    public boolean isMounted() {
        synchronized (lock) {
            Mount mount = virtualFileSystem.getMount();
            if (mount == null) return false;
            return mount.isMounted();
        }
    }

    // @Override
    public File getMountLocation() {
        return virtualFileSystem.getMount().getMountLocation().getAsFile();
    }

    // @Override
    private void mount(final String mountLocation) throws Exception {
        synchronized (lock) {
            if (isMounted()) { throw new IllegalStateException("Already mounted"); }

            Mount m = Mounts.mount(new MountParamsBuilder().set(ParamType.LISTENER, new MountListener() {
                // @Override
                public void eventOccurred(FormatterEvent event) {
                    if (event.getEventType() == FormatterEvent.EVENT.SUCCESSFULLY_MOUNTED) {
                        try {
                            // jdds.getWatchAsYouDownloadSession().getVirtualFileSystem().removeAllSessions();
                        } catch (Exception ioe) {
                            jdds.getDownloadLink().getLivePlugin().getLogger().log(Level.INFO, event.getMessage(), event.getException());
                        }
                    } else if (event.getEventType() == FormatterEvent.EVENT.DETACHED) {
                        virtualFileSystem.unmountAndEndSessions(true);
                    }
                }
            }).set(ParamType.FILE_SYSTEM, virtualFileSystem).set(ParamType.MOUNT_LOCATION, mountLocation).build());
            virtualFileSystem.setMount(m);
        }
    }

    // @Override
    public void waitForDownloadToFinish() throws PluginException {
        jdds.getDownloadLink().getLinkStatus().addStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
        jdds.getDownloadInterface().getManagedConnetionHandler().addThrottledConnection(new FakeThrottledConnection(jdds));
        jdds.getDownloadLink().setChunksProgress(new long[] { getTotalDownloaded() });
        UPDATE_LOOP: while (totalDownloaded < jdds.getDownloadLink().getDownloadSize() && jdds.getWatchAsYouDownloadSession().isMounted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Log.L.log(Level.FINE, "updateloop interrupted", ie);
            }
            try {
                ((RAFDownload) jdds.getDownloadInterface()).setTotalLinkBytesLoaded(getTotalDownloaded());
            } catch (final Throwable e) {
            }
            jdds.getDownloadLink().setChunksProgress(new long[] { totalDownloaded });

            if (jdds.getDownloadInterface().externalDownloadStop()) {
                // if download of even one of the splits is stopped,
                // all downloads must stop
                try {
                    virtualFileSystem.unmountAndEndSessions();
                } catch (Exception ex) {
                    Log.L.log(Level.INFO, "Error in Unmount and end session ", ex);
                }
                break UPDATE_LOOP;
            }
        }

        jdds.getDownloadLink().getLinkStatus().removeStatus(LinkStatus.FINISHED);
        jdds.getDownloadLink().setDownloadInstance(null);
        // jdds.getDownloadLink().getLinkStatus().setStatusText(null);
        jdds.getDownloadLink().setChunksProgress(new long[] { getTotalDownloaded() });

        // wait for other splits to finish
        try {
            WAIT_FOR_SPLITS_TO_FINISH: while (jdds.getWatchAsYouDownloadSession().isMounted()) {
                if (virtualFileSystem.sessionsCompleted()) {
                    try {
                        virtualFileSystem.unmountAndEndSessions();
                    } catch (Exception ex) {
                        Log.L.log(Level.INFO, "Error in Unmount and end session ", ex);
                    }
                    break WAIT_FOR_SPLITS_TO_FINISH;
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException ie) {
            // the stop button pressed
            Log.L.log(Level.FINE, "Stop button pressed", ie);
        }

        jdds.getDownloadLink().getFilePackage().setProperty(NeembuuExtension.INITIATED_BY_WATCH_ACTION, false);
        if (virtualFileSystem.allFilesCompletelyDownloaded()) {
            // mark any one link as finished only when all finish
            // this will make sure that next time watch as you download is
            // pressed.
            // the split which is completed will also be mounted.
            // all splits must be mounted for user to be able watch the video.
            jdds.getDownloadLink().getLinkStatus().addStatus(LinkStatus.FINISHED);

            final AtomicBoolean done = new AtomicBoolean(false);
            // the task of completing session done in another thread
            // When stop button is pressed, this task is aborted as io channels
            // throw an inturrupt exception. We want to give this sometime to
            // complete
            // if it takes too long, we might want to stop it.
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile().getFileStorageManager().completeSession(new File(jdds.getDownloadLink().getFileOutput()), jdds.getDownloadLink().getDownloadSize());
                    } catch (Exception i) {
                        Log.L.log(Level.SEVERE, "Problem in completing session", i);
                    } finally {
                        done.set(true);
                    }
                }
            };
            thread.start();
            while (!done.get() && thread.isAlive()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    // If we close diskmanager while it is joining, we might end
                    // up with a unclosable
                    // diskmanger instance.
                    // break;
                }
            }
            jdds.getWatchAsYouDownloadSession().getVirtualFileSystem().getPostProcessors().downloadComplete(jdds);
        } else {
            close();
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE);
        }
        close();
    }

    private void close() {
        SeekableConnectionFile vf = jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile();
        try {
            // unmounting should have closed the file, often it doesn't
            if (vf.isOpenByCascading() || vf.getFileDescriptor().isOpen()) {
                jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile().close();
            }
        } catch (final Throwable e) {
            Log.L.log(Level.SEVERE, "could not close virtual file", e);
        }
        try {
            vf.closeCompletely();
        } catch (final Throwable e) {
            Log.L.log(Level.SEVERE, "could not completely close virtual file", e);
        }

        try {
            jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile().getFileStorageManager().close();
        } catch (Exception a) {
            Log.L.log(Level.SEVERE, "could not close filestoragemanager", a);
        }
        NeembuuExtension.getInstance().getVirtualFileSystems().remove(jdds.getDownloadLink().getFilePackage());
        virtualFileSystem.removeSession(jdds);
    }

    private long updateTotalDownloaded() {
        UnsyncRangeArrayCopy<ReadRequestState> handlers = getSeekableConnectionFile().getTotalFileReadStatistics().getReadRequestStates();
        // ReadRequestState is equivalent to a JD Chunk

        long total = 0;
        for (int i = 0; i < handlers.size(); i++) {
            total += handlers.get(i).ending() - handlers.get(i).starting() + 1;
        }

        return total;
    }

}
