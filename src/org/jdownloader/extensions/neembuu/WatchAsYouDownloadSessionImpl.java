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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.download.DownloadInterface.Chunk;
import jpfm.FormatterEvent;
import jpfm.MountListener;
import jpfm.mount.Mount;
import jpfm.mount.MountParams.ParamType;
import jpfm.mount.MountParamsBuilder;
import jpfm.mount.Mounts;
import neembuu.diskmanager.DiskManager;
import neembuu.diskmanager.DiskManagers;
import neembuu.rangearray.UnsyncRangeArrayCopy;
import neembuu.vfs.file.MonitoredHttpFile;
import neembuu.vfs.file.SeekableConnectionFile;
import neembuu.vfs.file.SeekableConnectionFileParams;
import neembuu.vfs.file.TroubleHandler;
import neembuu.vfs.progresscontrol.ThrottleFactory;
import neembuu.vfs.readmanager.ReadRequestState;
import neembuu.vfs.readmanager.impl.SeekableConnectionFileImplBuilder;

import org.jdownloader.extensions.neembuu.gui.HttpFilePanel;
import org.jdownloader.extensions.neembuu.gui.VirtualFilesPanel;
import org.jdownloader.extensions.neembuu.newconnectionprovider.JD_HTTP_Download_Manager;
import org.jdownloader.extensions.neembuu.postprocess.PostProcessors;

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
    private final Object               lock          = new Object();

    private volatile long              totalDownload = 0;

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
            jdds.getDownloadInterface().logger.info("Using new filesystem " + fileSystem);
        } else {
            jdds.getDownloadInterface().logger.info("Using previous created filesystem " + fileSystem);
        }
        // read the javadoc to know what this does
        // Briefly : sometimes the filesystem is hung because of big sized
        // requests or because a new connection cannot
        // be made. In such cases it decides
        // what must be done.
        TroubleHandler troubleHandler = new NBTroubleHandler(jdds);

        // JD team might like to change this to something they like.
        // this default diskmanager saves each chunk in a different file
        // for this reason, chucks are placed in a directory.
        // In the default implementation, I chose not to save in a single file
        // because that would require to also matin progress information in a
        // separete file. The name of the file contains the offset from which
        // it starts, and size of the file tells us how much was downloaded.
        // Very important logs are also saved along with these files.
        // They are in html format, and I would strongly suggest if an alternate
        // implementation to this is provided, leave the html logs as they
        // are. 2 log files are created for every unique chunk,
        // and one for each file which logs overall working of differnt
        // chunks/regions. For this reason the download folder would contain
        // a lot of files. These logs are highly essential.
        DiskManager diskManager = DiskManagers.getDefaultManager(new File(jdds.getDownloadLink().getFileOutput()).getParentFile().getAbsolutePath());

        // Is used to create new connections at some arbitary offset
        // this is the only thing that neembuu requires JD to provide
        // all other things are handled by neembuu.
        // You will notice that the code is not much.
        // The same logic may be used to make a NewConnecitonProvider
        // for FTP and other protocols as the case maybe.
        JD_HTTP_Download_Manager newConnectionProvider = new JD_HTTP_Download_Manager(jdds);

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
        SeekableConnectionFile file = SeekableConnectionFileImplBuilder.build(new SeekableConnectionFileParams.Builder().setDiskManager(diskManager).setTroubleHandler(troubleHandler).setFileName(jdds.getDownloadLink().getFinalFileName()).setFileSize(jdds.getDownloadLink().getDownloadSize()).setNewConnectionProvider(newConnectionProvider).setParent(fileSystem.getRootDirectory()).setThrottleFactory(throttleFactory).build());

        MonitoredHttpFile httpFile = new MonitoredHttpFile(file, newConnectionProvider);
        final String mountLocation = fileSystem.getMountLocation(jdds);
        HttpFilePanel httpFilePanel = new HttpFilePanel(httpFile);
        WatchAsYouDownloadSessionImpl sessionImpl = new WatchAsYouDownloadSessionImpl(file, httpFile, httpFilePanel, fileSystem, jdds);
        jdds.setWatchAsYouDownloadSession(sessionImpl);
        JPanel virtualFilesPanel = VirtualFilesPanel.getOrCreate(jdds, mountLocation, httpFilePanel);
        sessionImpl.filePanel = virtualFilesPanel;
        jdds.getWatchAsYouDownloadSession().getHttpFilePanel().setVirtualPathOfFile(new File(mountLocation, jdds.getDownloadLink().getFinalFileName()).getAbsolutePath());

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

    public final long getTotalDownload() {
        return totalDownload;
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
                            jdds.getDownloadInterface().logger.log(Level.INFO, event.getMessage(), event.getException());
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
    public void waitForDownloadToFinish() throws Exception {
        jdds.getDownloadInterface().addChunksDownloading(1);
        Chunk ch = jdds.getDownloadInterface().new Chunk(0, 0, null, null) {
            // @Override
            // public long getSpeed() {
            // return (long)
            // jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile().getTotalFileReadStatistics().getTotalAverageDownloadSpeedProvider().getDownloadSpeed_KiBps()
            // * 1024;
            // }
        };
        ch.setInProgress(true);
        jdds.getDownloadInterface().getChunks().add(ch);
        jdds.getDownloadLink().getLinkStatus().addStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);

        UPDATE_LOOP: while (totalDownload < jdds.getDownloadLink().getDownloadSize() && jdds.getWatchAsYouDownloadSession().isMounted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {

            }

            updateProgress(ch);

            if (jdds.getDownloadInterface().externalDownloadStop()) {
                break UPDATE_LOOP;
            }
        }

        jdds.getDownloadLink().getLinkStatus().removeStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
        jdds.getDownloadLink().setDownloadInstance(null);
        jdds.getDownloadLink().getLinkStatus().setStatusText(null);
        ch.setInProgress(false);

        if (totalDownload >= jdds.getDownloadLink().getDownloadSize()) {
            jdds.getDownloadLink().getLinkStatus().addStatus(LinkStatus.FINISHED);
        }

        // wait for other splits to finish
        WAIT_FOR_SPLITS_TO_FINISH: while (jdds.getWatchAsYouDownloadSession().isMounted()) {
            if (virtualFileSystem.sessionsCompleted()) {
                virtualFileSystem.unmountAndEndSessions();
                break WAIT_FOR_SPLITS_TO_FINISH;
            }
            Thread.sleep(1000);
        }

        if (totalDownload >= jdds.getDownloadLink().getDownloadSize()) {
            jdds.getDownloadLink().getLinkStatus().addStatus(LinkStatus.FINISHED);

            final AtomicBoolean done = new AtomicBoolean(false);
            new Thread() {
                @Override
                public void run() {
                    try {
                        jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile().getFileStorageManager().completeSession(new File(jdds.getDownloadLink().getFileOutput()), jdds.getDownloadLink().getDownloadSize());
                        done.set(true);
                    } catch (Exception i) {
                        Logger.getGlobal().log(Level.SEVERE, "Problem in completing session", i);
                    }
                }
            }.start();

            for (int j = 0; !done.get() /* && j < 20 */; j++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
            PostProcessors.downloadComplete(jdds.getDownloadLink());
        } else {
            jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile().getFileStorageManager().close();
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE);
        }
    }

    private void updateProgress(Chunk ch) {
        UnsyncRangeArrayCopy<ReadRequestState> handlers = jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile().getTotalFileReadStatistics().getReadRequestStates();
        // ReadRequestState is equivalent to a JD Chunk

        long total = 0;
        for (int i = 0; i < handlers.size(); i++) {
            total += handlers.get(i).ending() - handlers.get(i).starting() + 1;
        }

        totalDownload = total;

        jdds.getDownloadLink().setDownloadCurrent(total);
        jdds.getDownloadLink().setChunksProgress(new long[] { total });
        // jdds.getDownloadLink().requestGuiUpdate();
        // jdds.getDownloadInterface().addToChunksInProgress(total);
    }

}
