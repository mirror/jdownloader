package org.jdownloader.extensions.jdanywhere.api.downloads;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadControllerEvent;
import jd.controlling.downloadcontroller.DownloadControllerListener;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.components.speedmeter.SpeedMeterPanel;
import jd.gui.swing.jdgui.components.toolbar.MainToolBar;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.EventsAPIEvent;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.api.downloads.DownloadsAPIImpl;
import org.jdownloader.api.polling.PollingAPIImpl;
import org.jdownloader.api.polling.PollingResultAPIStorable;
import org.jdownloader.extensions.jdanywhere.CheckUser;
import org.jdownloader.extensions.jdanywhere.JDAnywhereAPI;
import org.jdownloader.extensions.jdanywhere.JDAnywhereController;

public class DownloadsMobileAPIImpl implements DownloadsMobileAPI, DownloadControllerListener, JDAnywhereAPI {

    DownloadsAPIImpl  dlAPI = new DownloadsAPIImpl();
    PollingAPIImpl    plAPI = new PollingAPIImpl();
    private String    user;
    private String    pass;
    private CheckUser checkUser;

    public boolean start(final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        return dlAPI.start();
    }

    public boolean stop(final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        return dlAPI.stop();
    }

    public boolean pause(Boolean value, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        return dlAPI.pause(value);
    }

    @Override
    // paused = 1
    // stopped = 2
    // running = 0
    public String getState(final String username, final String password) {
        if (!checkUser.check(username, password)) return null;
        APIQuery queryParams = new APIQuery();
        queryParams.put("jdState", "");
        List<PollingResultAPIStorable> result = plAPI.poll(queryParams);
        for (PollingResultAPIStorable pr : result) {
            if (pr.getEventName() == "jdState") { return (String) pr.getEventData().get("data"); }
        }
        return "";
        // return dlAPI.getState();
    }

    public List<DownloadLinkAPIStorable> listDownloadLinks(long ID, final String username, final String password) {
        if (!checkUser.check(username, password)) return null;
        DownloadController dlc = DownloadController.getInstance();
        for (FilePackage fpkg : dlc.getPackages()) {
            if (fpkg.getUniqueID().getID() == ID) {
                synchronized (fpkg) {
                    List<DownloadLinkAPIStorable> links = new ArrayList<DownloadLinkAPIStorable>(fpkg.size());
                    for (DownloadLink link : fpkg.getChildren()) {
                        links.add(new DownloadLinkAPIStorable(link));
                    }
                    return links;
                }
            }
        }
        return null;
    }

    public List<FilePackageAPIStorable> listPackages(final String username, final String password) {
        if (!checkUser.check(username, password)) return null;
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        try {
            java.util.List<FilePackageAPIStorable> ret = new ArrayList<FilePackageAPIStorable>(dlc.size());
            for (FilePackage fpkg : dlc.getPackages()) {
                FilePackageAPIStorable pkg;
                ret.add(pkg = new FilePackageAPIStorable(fpkg));
                /*
                 * synchronized (fpkg) { List<DownloadLinkAPIStorable> links = new ArrayList<DownloadLinkAPIStorable>(fpkg.size()); for (DownloadLink link :
                 * fpkg.getChildren()) { links.add(new DownloadLinkAPIStorable(link)); } pkg.setLinks(links); }
                 */
            }
            return ret;
        } finally {
            dlc.readUnlock(b);
        }
    }

    public List<RunningObjectAPIStorable> runningLinks(final String username, final String password) {
        if (!checkUser.check(username, password)) return null;
        DownloadController dlc = DownloadController.getInstance();
        List<RunningObjectAPIStorable> ret = new ArrayList<RunningObjectAPIStorable>(dlc.getAllDownloadLinks().size());
        boolean b = dlc.readLock();
        try {
            for (FilePackage fpkg : dlc.getPackages()) {
                synchronized (fpkg) {
                    for (DownloadLink link : fpkg.getChildren()) {
                        if (link.getDownloadSpeed() > 0) {
                            RunningObjectAPIStorable ro = new RunningObjectAPIStorable();
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

    // returns the current downloadspeed
    // used in iPhone-App
    public int speed(final String username, final String password) {
        if (!checkUser.check(username, password)) return 0;
        return DownloadWatchDog.getInstance().getDownloadSpeedManager().getSpeed();
    }

    // returns the current downloadlimit
    // used in iPhone-App
    public int limit(final String username, final String password) {
        if (!checkUser.check(username, password)) return 0;
        return DownloadWatchDog.getInstance().getDownloadSpeedManager().getLimit();
    }

    // returns the current traffic
    // used in iPhone-App
    public long traffic(final String username, final String password) {
        if (!checkUser.check(username, password)) return 0;
        return DownloadWatchDog.getInstance().getDownloadSpeedManager().getTraffic();
    }

    private Boolean isNewLinkStatusJob(LinkStatusJob job) {
        Boolean returnValue = false;
        if (lastJobEvent != null) {
            returnValue = (job.isActive() == lastJobEvent.isActive());
            returnValue = returnValue && (job.isFinished() == lastJobEvent.isFinished());
            returnValue = returnValue && (job.isInProgress() == lastJobEvent.isInProgress());
            returnValue = returnValue && (job.getLinkID().equals(lastJobEvent.getLinkID()));
            returnValue = returnValue && (job.getStatus() == lastJobEvent.getStatus());
            returnValue = returnValue && (job.getStatusText().equals(lastJobEvent.getStatusText()));
        }
        lastJobEvent = job;
        return !returnValue;
    }

    @Override
    public DownloadLinkAPIStorable getDownloadLink(long ID, final String username, final String password) {
        if (!checkUser.check(username, password)) return null;
        DownloadLink link = getDownloadLinkFromID(ID);
        return new DownloadLinkAPIStorable(link);
    }

    @Override
    public FilePackageAPIStorable getFilePackage(long ID, final String username, final String password) {
        if (!checkUser.check(username, password)) return null;
        FilePackage fpkg = getFilePackageFromID(ID);
        return new FilePackageAPIStorable(fpkg);
    }

    public static LinkStatusJob lastJobEvent = null;

    public DownloadsMobileAPIImpl(String user, String pass) {
        DownloadController.getInstance().addListener(this, true);
        this.user = user;
        this.pass = pass;
        checkUser = new CheckUser(user, pass);

        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.getEventSender().addListener(new GenericConfigEventListener<Integer>() {

            public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
                HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("message", "LimitspeedChanged");
                data.put("data", org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.getValue());
                JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("LimitspeedChanged", data), null);
            }

            public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
            }
        }, false);
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("message", "LimitspeedActivated");
                data.put("data", org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled());
                JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("LimitspeedActivated", data), null);
            }
        }, false);

    }

    // public List<FilePackageAPIStorable> list() {

    public String getPackageIDFromLinkID(long ID, final String username, final String password) {
        if (!checkUser.check(username, password)) return null;
        DownloadLink dl = getDownloadLinkFromID(ID);
        FilePackage fpk = dl.getFilePackage();
        return fpk.getUniqueID().toString();
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
        } finally {
            dlc.readUnlock(b);
        }
        return null;
    }

    private FilePackage getFilePackageFromID(long ID) {
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        try {
            for (FilePackage fpkg : dlc.getPackages()) {
                if (fpkg.getUniqueID().getID() == ID) {
                    synchronized (fpkg) {
                        return fpkg;
                    }
                }
            }
        } finally {
            dlc.readUnlock(b);
        }
        return null;
    }

    public boolean removeDownloadLink(String ID, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        long id = Long.valueOf(ID);
        try {
            for (FilePackage fpkg : dlc.getPackages()) {
                synchronized (fpkg) {
                    for (DownloadLink link : fpkg.getChildren()) {
                        if (link.getUniqueID().getID() == id) {
                            List<DownloadLink> dlLinks = new ArrayList<DownloadLink>(0);
                            dlLinks.add(link);
                            dlc.removeChildren(fpkg, dlLinks, true);
                            return true;
                        }
                    }
                }
            }
        } finally {
            dlc.readUnlock(b);
        }
        return true;
    }

    public boolean removeDownloadPackage(String ID, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        long id = Long.valueOf(ID);
        FilePackage fpkg = getFilePackageFromID(id);
        if (fpkg != null) {
            DownloadController.getInstance().removePackage(fpkg);
        }
        return true;
    }

    // Sets the enabled flag of a downloadlink
    // used in iPhone-App
    public boolean downloadLinkEnabled(String ID, boolean enabled, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        DownloadLink link = getDownloadLinkFromID(Long.valueOf(ID));
        link.setEnabled(enabled);
        return true;
    }

    // Sets the enabled flag of a downloadPackage
    // used in iPhone-App
    public boolean downloadPackageEnabled(String ID, boolean enabled, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        long id = Long.valueOf(ID);
        try {
            for (FilePackage fpkg : dlc.getPackages()) {
                if (fpkg.getUniqueID().getID() == id) synchronized (fpkg) {
                    for (DownloadLink link : fpkg.getChildren()) {
                        link.setEnabled(enabled);
                    }
                    return true;
                }
            }
        } finally {
            dlc.readUnlock(b);
        }
        return true;
    }

    public boolean resetPackage(String ID, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        long id = Long.valueOf(ID);
        try {
            for (FilePackage fpkg : dlc.getPackages()) {
                if (fpkg.getUniqueID().getID() == id) synchronized (fpkg) {
                    for (DownloadLink link : fpkg.getChildren()) {
                        link.reset();
                    }
                    return true;
                }
            }
        } finally {
            dlc.readUnlock(b);
        }
        return true;
    }

    public boolean resetDownloadLink(String ID, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        DownloadLink link = getDownloadLinkFromID(Long.valueOf(ID));
        link.reset();
        return true;
    }

    public boolean setLimitspeed(int speed, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.setValue(speed);
        return true;
    }

    public boolean activateLimitspeed(boolean activate, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.setValue(activate);
        return true;
    }

    // returns the SpeedMeter from UI without the DownloadSpeed / AverageSpeed Text as an PNG
    // used in iPhone-App
    public void speedMeter(RemoteAPIRequest request, RemoteAPIResponse response, final String username, final String password) {
        if (!checkUser.check(username, password)) return;

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

    @Override
    public void onDownloadControllerEvent(DownloadControllerEvent event) {
        switch (event.getType()) {
        case REFRESH_CONTENT:
        case REFRESH_STRUCTURE:
            if (event.getParameter() instanceof DownloadLink) {
                DownloadLink dl = (DownloadLink) event.getParameter();
                if (dl != null) {
                    Object param = event.getParameter(1);
                    if (param instanceof DownloadLinkProperty) {
                        /* put your code here */
                    }
                    LinkStatusJob job = new LinkStatusJob();
                    LinkStatus linkStatus = dl.getLinkStatus();
                    job.setActive(linkStatus.isPluginActive());
                    job.setFinished(linkStatus.isFinished());
                    job.setInProgress(linkStatus.isPluginInProgress());

                    job.setLinkID(dl.getUniqueID().toString());
                    job.setStatus(linkStatus.getStatus());
                    String messageText = linkStatus.getMessage(false);
                    if (messageText != null) {
                        job.setStatusText(messageText);
                    } else {
                        job.setStatusText("");
                    }

                    if (isNewLinkStatusJob(job)) {
                        HashMap<String, Object> data = new HashMap<String, Object>();
                        data.put("message", dl.getName());
                        data.put("data", job);
                        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("linkstatus", data), null);
                    }
                }

            }
            break;
        case REMOVE_CONTENT:
            if (event.getParameters() != null) {
                for (Object link : (Object[]) event.getParameters()) {
                    if (link instanceof DownloadLink) downloadApiLinkRemoved((DownloadLink) link);
                    if (link instanceof FilePackage) downloadApiPackageRemoved((FilePackage) link);
                }
            }
            break;
        case ADD_CONTENT:
            if (event.getParameters() != null) {
                for (Object link : (Object[]) event.getParameters()) {
                    if (link instanceof DownloadLink) downloadApiLinkAdded((DownloadLink) link);
                    if (link instanceof FilePackage) downloadApiPackageAdded((FilePackage) link);
                }
            }
            break;
        }
    }

    private void downloadApiLinkAdded(DownloadLink link) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", "downloadLinkAdded");
        data.put("message", link.getName());
        data.put("data", link.getUniqueID().toString());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("downloadLinkAdded", data), null);
    }

    private void downloadApiLinkRemoved(DownloadLink link) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", "downloadLinkRemoved");
        data.put("message", link.getName());
        data.put("data", link.getUniqueID().toString());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("downloadLinkRemoved", data), null);
    }

    private void downloadApiPackageAdded(FilePackage fpkg) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", "downloadPackageAdded");
        data.put("message", fpkg.getName());
        data.put("data", fpkg.getUniqueID().toString());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("downloadPackageAdded", data), null);
    }

    private void downloadApiPackageRemoved(FilePackage fpkg) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", "downloadPackageRemoved");
        data.put("message", fpkg.getName());
        data.put("data", fpkg.getUniqueID().toString());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("downloadPackageRemoved", data), null);
    }

    public String getUsername() {
        return user;
    }

    public String getPassword() {
        return pass;
    }

}
