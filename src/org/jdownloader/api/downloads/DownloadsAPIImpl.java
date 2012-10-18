package org.jdownloader.api.downloads;

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
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.HTTPHeader;

public class DownloadsAPIImpl implements DownloadsAPI {

    public List<FilePackageAPIStorable> list() {
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        try {
            java.util.List<FilePackageAPIStorable> ret = new ArrayList<FilePackageAPIStorable>(dlc.size());
            for (FilePackage fpkg : dlc.getPackages()) {
                FilePackageAPIStorable pkg;
                ret.add(pkg = new FilePackageAPIStorable(fpkg));
                synchronized (fpkg) {
                    List<DownloadLinkAPIStorable> links = new ArrayList<DownloadLinkAPIStorable>(fpkg.size());
                    for (DownloadLink link : fpkg.getChildren()) {
                        links.add(new DownloadLinkAPIStorable(link));
                    }
                    pkg.setLinks(links);
                }
            }
            return ret;
        } finally {
            dlc.readUnlock(b);
        }
    }

    // returns a list of all running downloads
    // used in iPhone-App
    public List<FilePackageAPIStorable> running() {
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        try {
            java.util.List<FilePackageAPIStorable> ret = new ArrayList<FilePackageAPIStorable>(dlc.size());
            for (FilePackage fpkg : dlc.getPackages()) {
                FilePackageAPIStorable pkg;
                ret.add(pkg = new FilePackageAPIStorable(fpkg));
                boolean hasRunningLinks = false;
                synchronized (fpkg) {
                    List<DownloadLinkAPIStorable> links = new ArrayList<DownloadLinkAPIStorable>(fpkg.size());
                    for (DownloadLink link : fpkg.getChildren()) {
                        DownloadLinkAPIStorable apiLink = new DownloadLinkAPIStorable(link);
                        if (apiLink.getSpeed() > 0) {
                            hasRunningLinks = true;
                            links.add(new DownloadLinkAPIStorable(link));
                        }
                    }
                    pkg.setLinks(links);
                }
                if (!hasRunningLinks) {
                    ret.remove(pkg);
                }
            }
            return ret;
        } finally {
            dlc.readUnlock(b);
        }
    }

    private DownloadLink getDownloadLinkFromID(long ID) {
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        try {
            for (FilePackage fpkg : dlc.getPackages()) {
                synchronized (fpkg) {
                    for (DownloadLink link : fpkg.getChildren()) {
                        if (link.getUniqueID().getID() == ID) { return link; }
                    }
                }
            }
            return null;
        } finally {
            dlc.readUnlock(b);
        }
    }

    // Sets the enabled flag of a downloadlink
    // used in iPhone-App
    public boolean downloadLinkEnabled(String ID, boolean enabled) {
        DownloadLink link = getDownloadLinkFromID(Long.valueOf(ID));
        link.setEnabled(enabled);
        return true;
    }

    public boolean stop() {
        DownloadWatchDog.getInstance().startDownloads();
        return true;
    }

    public boolean start() {
        DownloadWatchDog.getInstance().stopDownloads();
        return true;
    }

    // returns the current downloadspeed
    // used in iPhone-App
    public int speed() {
        return DownloadWatchDog.getInstance().getDownloadSpeedManager().getSpeed();
    }

    // returns the current downloadlimit
    // used in iPhone-App
    public int limit() {
        return DownloadWatchDog.getInstance().getDownloadSpeedManager().getLimit();
    }

    // returns the current traffic
    // used in iPhone-App
    public long traffic() {
        return DownloadWatchDog.getInstance().getDownloadSpeedManager().getTraffic();
    }

    // returns the SpeedMeter from UI without the DownloadSpeed / AverageSpeed Text as an PNG
    // used in iPhone-App
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

}
