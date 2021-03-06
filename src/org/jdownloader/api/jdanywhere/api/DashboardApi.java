package org.jdownloader.api.jdanywhere.api;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.gui.swing.jdgui.components.speedmeter.SpeedMeterPanel;
import jd.gui.swing.jdgui.components.toolbar.MainToolBar;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.storage.config.ValidationException;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.speedmeter.SpeedMeterInterface.Resolution;
import org.jdownloader.api.jdanywhere.api.interfaces.IDashboardApi;
import org.jdownloader.api.jdanywhere.api.storable.RunningObjectStorable;
import org.jdownloader.api.polling.PollingAPIImpl;
import org.jdownloader.api.polling.PollingResultAPIStorable;
import org.jdownloader.api.toolbar.JDownloaderToolBarAPIImpl;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.settings.staticreferences.CFG_RECONNECT;

public class DashboardApi implements IDashboardApi {
    PollingAPIImpl            plAPI      = new PollingAPIImpl();
    JDownloaderToolBarAPIImpl tbAPI      = new JDownloaderToolBarAPIImpl();
    LinkedList<Integer>       _speedList = new LinkedList<Integer>(Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    Timer                     timer      = new Timer();

    public DashboardApi() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                final int speed = DownloadWatchDog.getInstance().getDownloadSpeedManager().getSpeed();
                synchronized (_speedList) {
                    _speedList.addLast(speed);
                    _speedList.removeFirst();
                }
            }
        }, 3 * 1000, 3 * 1000);
    }

    public List<Integer> speedList() {
        synchronized (_speedList) {
            return new ArrayList<Integer>(_speedList);
        }
    }

    public String apiVersion() {
        return "1.0.20131030";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#start()
     */
    @Override
    public boolean start() {
        DownloadWatchDog.getInstance().startDownloads();
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#stop()
     */
    @Override
    public boolean stop() {
        DownloadWatchDog.getInstance().stopDownloads();
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#pause(java.lang .Boolean)
     */
    @Override
    public boolean pause() {
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(!DownloadWatchDog.getInstance().isPaused());
        return true;
    }

    // paused = 1
    // stopped = 2
    // running = 0
    /*
     * (non-Javadoc)
     *
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#getState()
     */
    @Override
    public String getState() {
        APIQuery queryParams = new APIQuery();
        queryParams.put("jdState", "");
        List<PollingResultAPIStorable> result = plAPI.poll(null, queryParams);
        for (PollingResultAPIStorable pr : result) {
            if (pr.getEventName() == "jdState") {
                return (String) pr.getEventData().get("data");
            }
        }
        return "";
        // return dlAPI.getState();
    }

    public synchronized Object getCompleteState() {
        org.jdownloader.myjdownloader.client.json.JsonMap ret = new org.jdownloader.myjdownloader.client.json.JsonMap();
        int running = DownloadWatchDog.getInstance().getActiveDownloads();
        ret.put("running", running > 0);
        ret.put("limit", org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled());
        // if
        // (org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled())
        // {
        ret.put("limitspeed", org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.getValue());
        // } else {
        // ret.put("limitspeed", 0);
        // }
        ret.put("reconnect", CFG_RECONNECT.AUTO_RECONNECT_ENABLED.isEnabled());
        ret.put("clipboard", org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED.isEnabled());
        ret.put("stopafter", DownloadWatchDog.getInstance().getSession().isStopMarkSet());
        ret.put("premium", org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled());
        if (running == 0) {
            ret.put("speed", 0);
        } else {
            ret.put("speed", DownloadWatchDog.getInstance().getDownloadSpeedManager().getSpeedMeter().getValue(Resolution.SECONDS));
        }
        ret.put("pause", DownloadWatchDog.getInstance().isPaused());
        List<DownloadLink> calc_progress = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {
            public int returnMaxResults() {
                return 0;
            }

            public boolean acceptNode(DownloadLink node) {
                if (!node.isEnabled()) {
                    return false;
                }
                if (FinalLinkState.CheckFailed(node.getFinalLinkState())) {
                    return false;
                }
                return true;
            }
        });
        long todo = 0;
        long done = 0;
        for (DownloadLink link : calc_progress) {
            done += Math.max(0, link.getView().getBytesLoaded());
            todo += Math.max(0, link.getView().getBytesTotalEstimated());
        }
        ret.put("download_current", done);
        ret.put("download_complete", todo);
        ret.put("maxDL", org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS.getValue());
        ret.put("maxConDL", org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_CHUNKS_PER_FILE.getValue());
        ret.put("maxConHost", org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS_PER_HOST.getValue());
        ret.put("maxConHostActive", org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED.isEnabled());
        return ret;
    }

    // returns the current downloadspeed
    // used in iPhone-App
    /*
     * (non-Javadoc)
     *
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#speed()
     */
    @Override
    public int speed() {
        return DownloadWatchDog.getInstance().getDownloadSpeedManager().getSpeed();
    }

    // returns the current downloadlimit
    // used in iPhone-App
    /*
     * (non-Javadoc)
     *
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#limit()
     */
    @Override
    public int limit() {
        return DownloadWatchDog.getInstance().getDownloadSpeedManager().getLimit();
    }

    // returns the current traffic
    // used in iPhone-App
    /*
     * (non-Javadoc)
     *
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#traffic()
     */
    @Override
    public long traffic() {
        return DownloadWatchDog.getInstance().getDownloadSpeedManager().getTraffic();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#setLimitspeed (int)
     */
    @Override
    public boolean setLimitspeed(int speed) {
        boolean returnValue = true;
        try {
            org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.setValue(speed);
        } catch (final ValidationException e) {
            returnValue = false;
        }
        return returnValue;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#activateLimitspeed (boolean)
     */
    @Override
    public boolean activateLimitspeed(boolean activate) {
        boolean returnValue = true;
        try {
            org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.setValue(activate);
        } catch (final ValidationException e) {
            returnValue = false;
        }
        return returnValue;
    }

    // returns the SpeedMeter from UI without the DownloadSpeed / AverageSpeed
    // Text as an PNG
    // used in iPhone-App
    /*
     * (non-Javadoc)
     *
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#speedMeter(org .appwork.remoteapi.RemoteAPIRequest,
     * org.appwork.remoteapi.RemoteAPIResponse)
     */
    @Override
    public void speedMeter(RemoteAPIRequest request, RemoteAPIResponse response) throws InternalApiException {
        OutputStream out = null;
        try {
            SpeedMeterPanel speedMeter = MainToolBar.getInstance().getSpeedMeter();
            BufferedImage bufferedImage = new BufferedImage(speedMeter.getWidth(), speedMeter.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.setColor(Color.white);
            g2d.fillRect(0, 0, speedMeter.getWidth(), speedMeter.getHeight());
            speedMeter.paintComponent(g2d, false);
            g2d.dispose();
            /* we force content type to image/png and allow caching of the image */
            /*
             * response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CACHE_CONTROL, "public,max-age=60", false));
             */
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "image/png", false));
            out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), false);
            ImageProvider.writeImage(bufferedImage, "png", out);
        } catch (IOException e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            throw new InternalApiException(e);
        } finally {
            try {
                out.close();
            } catch (final Throwable e) {
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#runningLinks()
     */
    @Override
    public List<RunningObjectStorable> runningLinks() {
        DownloadController dlc = DownloadController.getInstance();
        List<RunningObjectStorable> ret = new ArrayList<RunningObjectStorable>(dlc.getAllChildren().size());
        boolean b = dlc.readLock();
        try {
            for (FilePackage fpkg : dlc.getPackages()) {
                final boolean readL = fpkg.getModifyLock().readLock();
                try {
                    for (DownloadLink link : fpkg.getChildren()) {
                        if (link.getView().getSpeedBps() > 0) {
                            RunningObjectStorable ro = new RunningObjectStorable();
                            ro.setPackageID(fpkg.getUniqueID().getID());
                            ro.setLinkID(link.getUniqueID().getID());
                            ro.setDone(link.getView().getBytesLoaded());
                            ro.setSpeed(link.getView().getSpeedBps());
                            long remainingBytes = (link.getView().getBytesTotal() - link.getView().getBytesLoaded());
                            ro.setETA(remainingBytes / link.getView().getSpeedBps());
                            ret.add(ro);
                        }
                    }
                } finally {
                    fpkg.getModifyLock().readUnlock(readL);
                }
            }
        } finally {
            dlc.readUnlock(b);
        }
        return ret;
    }

    public boolean setMaxDL(int value) {
        boolean returnValue = true;
        try {
            org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS.setValue(value);
        } catch (final ValidationException e) {
            returnValue = false;
        }
        return returnValue;
    }

    public boolean setMaxConDL(int value) {
        boolean returnValue = true;
        try {
            org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_CHUNKS_PER_FILE.setValue(value);
        } catch (final ValidationException e) {
            returnValue = false;
        }
        return returnValue;
    }

    public boolean setMaxConHost(int value) {
        boolean returnValue = true;
        try {
            org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS_PER_HOST.setValue(value);
        } catch (final ValidationException e) {
            returnValue = false;
        }
        return returnValue;
    }

    public boolean activateMaxConHost(boolean value) {
        boolean returnValue = true;
        try {
            org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED.setValue(value);
        } catch (final ValidationException e) {
            returnValue = false;
        }
        return returnValue;
    }

    public boolean activateReconnect(boolean value) {
        boolean returnValue = true;
        try {
            CFG_RECONNECT.AUTO_RECONNECT_ENABLED.setValue(value);
        } catch (final ValidationException e) {
            returnValue = false;
        }
        return returnValue;
    }

    public boolean activatePremium(boolean value) {
        boolean returnValue = true;
        try {
            org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.setValue(value);
        } catch (final ValidationException e) {
            returnValue = false;
        }
        return returnValue;
    }
}
