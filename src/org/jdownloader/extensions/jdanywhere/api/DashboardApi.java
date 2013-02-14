package org.jdownloader.extensions.jdanywhere.api;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.components.speedmeter.SpeedMeterPanel;
import jd.gui.swing.jdgui.components.toolbar.MainToolBar;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.api.downloads.DownloadsAPIImpl;
import org.jdownloader.api.polling.PollingAPIImpl;
import org.jdownloader.api.polling.PollingResultAPIStorable;
import org.jdownloader.api.toolbar.JDownloaderToolBarAPIImpl;
import org.jdownloader.extensions.jdanywhere.api.interfaces.IDashboardApi;
import org.jdownloader.extensions.jdanywhere.api.storable.RunningObjectStorable;

public class DashboardApi implements IDashboardApi {
    DownloadsAPIImpl          dlAPI = new DownloadsAPIImpl();
    PollingAPIImpl            plAPI = new PollingAPIImpl();
    JDownloaderToolBarAPIImpl tbAPI = new JDownloaderToolBarAPIImpl();

    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#start()
     */
    @Override
    public boolean start() {
        return dlAPI.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#stop()
     */
    @Override
    public boolean stop() {
        return dlAPI.stop();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#pause(java.lang.Boolean)
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
        List<PollingResultAPIStorable> result = plAPI.poll(queryParams);
        for (PollingResultAPIStorable pr : result) {
            if (pr.getEventName() == "jdState") { return (String) pr.getEventData().get("data"); }
        }
        return "";
        // return dlAPI.getState();
    }

    public synchronized Object getCompleteState() {
        return tbAPI.getStatus();
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
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#setLimitspeed(int)
     */
    @Override
    public boolean setLimitspeed(int speed) {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.setValue(speed);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#activateLimitspeed(boolean)
     */
    @Override
    public boolean activateLimitspeed(boolean activate) {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.setValue(activate);
        return true;
    }

    // returns the SpeedMeter from UI without the DownloadSpeed / AverageSpeed Text as an PNG
    // used in iPhone-App
    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.extensions.jdanywhere.api.IDashboardApi#speedMeter(org.appwork.remoteapi.RemoteAPIRequest,
     * org.appwork.remoteapi.RemoteAPIResponse)
     */
    @Override
    public void speedMeter(RemoteAPIRequest request, RemoteAPIResponse response) {
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
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "image/png", false));
            out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), false);
            ImageIO.write(bufferedImage, "png", out);
        } catch (IOException e) {
            Log.exception(e);
            throw new RemoteAPIException(e);
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
        List<RunningObjectStorable> ret = new ArrayList<RunningObjectStorable>(dlc.getAllDownloadLinks().size());
        boolean b = dlc.readLock();
        try {
            for (FilePackage fpkg : dlc.getPackages()) {
                synchronized (fpkg) {
                    for (DownloadLink link : fpkg.getChildren()) {
                        if (link.getDownloadSpeed() > 0) {
                            RunningObjectStorable ro = new RunningObjectStorable();
                            ro.setPackageID(fpkg.getUniqueID().getID());
                            ro.setLinkID(link.getUniqueID().getID());
                            ro.setDone(link.getDownloadCurrent());
                            ro.setSpeed(link.getDownloadSpeed());
                            ret.add(ro);
                        }
                    }
                }
            }
        } finally {
            dlc.readUnlock(b);
        }
        return ret;
    }
}
