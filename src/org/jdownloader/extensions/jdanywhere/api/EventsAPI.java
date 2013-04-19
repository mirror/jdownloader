package org.jdownloader.extensions.jdanywhere.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadControllerEvent;
import jd.controlling.downloadcontroller.DownloadControllerListener;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcollector.VariousCrawledPackage;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.DownloadLinkProperty;
import jd.plugins.DownloadLink.DownloadLinkProperty.Property;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.captcha.event.ChallengeResponseListener;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.extensions.jdanywhere.api.interfaces.IEventsApi;
import org.jdownloader.extensions.jdanywhere.api.storable.CaptchaJob;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class EventsAPI implements DownloadControllerListener, StateEventListener, LinkCollectorListener, ChallengeResponseListener, IEventsApi {

    HashMap<Long, String> linkStatusMessages = new HashMap<Long, String>();

    public EventsAPI() {
        DownloadController.getInstance().addListener(this, true);
        ChallengeResponseController.getInstance().getEventSender().addListener(this);
        LinkCollector.getInstance().getEventsender().addListener(this, true);
        DownloadWatchDog.getInstance().getStateMachine().addListener(this);
        CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.getEventSender().addListener(new GenericConfigEventListener<Integer>() {

            public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
                HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("message", "Limitspeed");
                data.put("data", CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.getValue());
                // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("SettingsChanged", data), null);
            }

            public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
            }
        }, false);
        CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("message", "LimitspeedActivated");
                data.put("data", CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled());
                // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("SettingsChanged", data), null);
            }
        }, false);
        CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS.getEventSender().addListener(new GenericConfigEventListener<Integer>() {

            public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
                HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("message", "MaxDL");
                data.put("data", CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS.getValue());
                // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("SettingsChanged", data), null);
            }

            public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
            }
        }, false);
        CFG_GENERAL.MAX_CHUNKS_PER_FILE.getEventSender().addListener(new GenericConfigEventListener<Integer>() {

            public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
                HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("message", "MaxConDL");
                data.put("data", CFG_GENERAL.MAX_CHUNKS_PER_FILE.getValue());
                // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("SettingsChanged", data), null);
            }

            public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
            }
        }, false);
        CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS_PER_HOST.getEventSender().addListener(new GenericConfigEventListener<Integer>() {

            public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
                HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("message", "MaxConHost");
                data.put("data", CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS_PER_HOST.getValue());
                // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("SettingsChanged", data), null);
            }

            public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
            }
        }, false);
        CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("message", "MaxConHostActivated");
                data.put("data", CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED.isEnabled());
                // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("SettingsChanged", data), null);
            }
        }, false);
        CFG_GENERAL.AUTO_RECONNECT_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("message", "Reconnect");
                data.put("data", CFG_GENERAL.AUTO_RECONNECT_ENABLED.isEnabled());
                // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("SettingsChanged", data), null);
            }
        }, false);
        CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("message", "Premium");
                data.put("data", CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled());
                // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("SettingsChanged", data), null);
            }
        }, false);
    }

    // Sets the enabled flag of a downloadPackage
    // used in iPhone-App
    public boolean downloadPackageEnabled(String ID, boolean enabled) {
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

    public boolean resetPackage(String ID) {
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

    @SuppressWarnings("incomplete-switch")
    @Override
    public void onDownloadControllerEvent(DownloadControllerEvent event) {
        switch (event.getType()) {
        case REFRESH_CONTENT:
        case REFRESH_STRUCTURE:
            if (event.getParameter() instanceof DownloadLink) {
                DownloadLink dl = (DownloadLink) event.getParameter();
                if (dl != null) {
                    HashMap<String, Object> data = new HashMap<String, Object>();
                    Object param = event.getParameter(1);
                    if (param instanceof DownloadLinkProperty) {

                        if (((DownloadLinkProperty) param).getProperty() == Property.RESET) {
                            data.put("linkID", dl.getUniqueID().getID());
                            data.put("packageID", dl.getFilePackage().getUniqueID().toString());
                            data.put("action", "Reset");
                            // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("LinkChanged", data), null);
                        } else {
                            data.put("linkID", dl.getUniqueID().getID());
                            data.put("packageID", dl.getFilePackage().getUniqueID().toString());
                            data.put("NewValue", ((DownloadLinkProperty) param).getValue());
                            switch (((DownloadLinkProperty) param).getProperty()) {
                            case NAME:
                                data.put("action", "NameChanged");
                                break;
                            case PRIORITY:
                                data.put("action", "PriorityChanged");
                                break;
                            case ENABLED:
                                data.put("action", "EnabledChanged");
                                break;
                            case AVAILABILITY:
                                data.put("action", "AvailabilityChanged");
                                break;
                            }
                            // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("LinkstatusChanged", data), null);
                        }
                    } else {
                        LinkStatus linkStatus = dl.getLinkStatus();
                        if (linkStatus.getLatestStatus() == 2 && linkStatus.isPluginActive()) { // && linkStatus.getStatus() !=
                                                                                                // linkStatus.getLatestStatus()) {
                            data.put("linkID", dl.getUniqueID().getID());
                            data.put("packageID", dl.getFilePackage().getUniqueID().toString());
                            data.put("action", "Finished");
                            // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("LinkChanged", data), null);
                            if (dl.getFilePackage().getFinishedDate() > 0) {
                                data = new HashMap<String, Object>();
                                data.put("packageID", dl.getFilePackage().getUniqueID().toString());
                                data.put("action", "PackageFinished");
                                // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("PackageFinished", data), null);
                            }
                        } else {
                            String lastMessage = linkStatusMessages.get(dl.getUniqueID().getID());
                            if (lastMessage == null) {
                                lastMessage = "";
                            }
                            String newMessage = linkStatus.getMessage(false);
                            if (newMessage == null) {
                                newMessage = "";
                            }
                            if (!lastMessage.equals(newMessage)) {
                                linkStatusMessages.remove(dl.getUniqueID().getID());
                                linkStatusMessages.put(dl.getUniqueID().getID(), newMessage);
                                data.put("action", "MessageChanged");
                                data.put("linkID", dl.getUniqueID().getID());

                                data.put("NewValue", newMessage);
                                // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("LinkstatusChanged", data), null);
                            }
                        }
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
        data.put("linkID", link.getUniqueID().toString());
        data.put("packageID", link.getFilePackage().getUniqueID().toString());
        // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("downloadLinkAdded", data), null);
    }

    private void downloadApiLinkRemoved(DownloadLink link) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("linkID", link.getUniqueID().toString());
        // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("downloadLinkRemoved", data), null);
    }

    private void downloadApiPackageAdded(FilePackage fpkg) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("packageID", fpkg.getUniqueID().toString());
        // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("downloadPackageAdded", data), null);
    }

    private void downloadApiPackageRemoved(FilePackage fpkg) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("packageID", fpkg.getUniqueID().toString());
        // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("downloadPackageRemoved", data), null);
    }

    private void sendEvent(SolverJob<?> job, String type) {
        // if (job == null || !(job.getChallenge() instanceof ImageCaptchaChallenge) || job.isDone()) { throw new
        // RemoteAPIException(ResponseCode.ERROR_NOT_FOUND, "Captcha no longer available"); }
        long captchCount = 0;
        java.util.List<CaptchaJob> ret = new ArrayList<CaptchaJob>();
        for (SolverJob<?> entry : ChallengeResponseController.getInstance().listJobs()) {
            if (entry.isDone()) continue;
            if (entry.getChallenge() instanceof ImageCaptchaChallenge) {
                captchCount++;
            }
        }

        ImageCaptchaChallenge<?> challenge = (ImageCaptchaChallenge<?>) job.getChallenge();

        CaptchaJob apiJob = new CaptchaJob();
        if (challenge.getResultType().isAssignableFrom(String.class))
            apiJob.setType("Text");
        else
            apiJob.setType("Click");
        // apiJob.setType(challenge.getClass().getSimpleName());
        apiJob.setID(challenge.getId().getID());
        apiJob.setHoster(challenge.getPlugin().getHost());
        apiJob.setCaptchaCategory(challenge.getExplain());
        apiJob.setCount(captchCount);
        // apiJob.setType(challenge.getClass().getSimpleName());
        // apiJob.setID(challenge.getId().getID());
        // apiJob.setHoster(challenge.getPlugin().getHost());
        // apiJob.setCaptchaCategory(challenge.getTypeID());
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("message", type);
        data.put("data", apiJob);
        // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("captcha", data), null);

    }

    public void onStateChange(StateEvent event) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("message", "Running State Changed");
        data.put("data", event.getNewState().getLabel());
        // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("runningstate", data), null);
    }

    public void onStateUpdate(StateEvent event) {
    }

    private void linkCollectorApiLinkAdded(CrawledLink link) {
        if (link.getParentNode().getUniqueID() != null) {
            HashMap<String, Object> data = new HashMap<String, Object>();
            data.put("packageID", link.getParentNode().getUniqueID().toString());
            data.put("linkID", link.getUniqueID().toString());
            // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("linkCollectorLinkAdded", data), null);
        }
    }

    private void linkCollectorApiLinkRemoved(List<CrawledLink> links) {
        List<String> linkIDs = new ArrayList<String>();
        for (CrawledLink link : links) {
            linkIDs.add(link.getDownloadLink().getUniqueID().toString());
        }

        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("linkIDs", linkIDs);
        // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("linkCollectorLinkRemoved", data), null);
    }

    private void linkCollectorApiPackageAdded(CrawledPackage cpkg) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("packageID", cpkg.getUniqueID().toString());
        // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("linkCollectorPackageAdded", data), null);
    }

    private void linkCollectorApiPackageRemoved(CrawledPackage cpkg) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("packageID", cpkg.getUniqueID().toString());
        // JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventObject("linkCollectorPackageRemoved", data), null);
    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
        if (event.getParameters() != null) {
            for (Object object : event.getParameters()) {
                if (object instanceof List<?>) {
                    if (object != null && ((List<?>) object).get(0) instanceof CrawledLink) {
                        linkCollectorApiLinkRemoved((List<CrawledLink>) object);
                    }
                }
                if (object instanceof CrawledPackage) linkCollectorApiPackageRemoved((CrawledPackage) event.getParameter());
            }
        }
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
        if (event.getParameters() != null) {
            for (Object object : event.getParameters()) {
                if (object instanceof CrawledLink) linkCollectorApiLinkAdded((CrawledLink) object);
                if (object instanceof CrawledPackage || object instanceof VariousCrawledPackage) linkCollectorApiPackageAdded((CrawledPackage) event.getParameter());
            }
        }
    }

    @Override
    public void onLinkCollectorContentModified(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
        linkCollectorApiLinkAdded((CrawledLink) parameter);
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onNewJobAnswer(SolverJob<?> job, AbstractResponse<?> response) {
    }

    @Override
    public void onJobDone(SolverJob<?> job) {
        sendEvent(job, "expired");
    }

    @Override
    public void onNewJob(SolverJob<?> job) {
        sendEvent(job, "new");
        sendNewCaptcha(job);
    }

    @Override
    public void onJobSolverEnd(ChallengeSolver<?> solver, SolverJob<?> job) {
    }

    @Override
    public void onJobSolverStart(ChallengeSolver<?> solver, SolverJob<?> job) {
    }

    @Override
    public void onLinkCollectorListLoaded() {
    }

    Map<String, Long> captchaPushList = new HashMap<String, Long>();

    public boolean RegisterCaptchaPush(String host, String path, String query) {
        URI uri = null;
        try {
            uri = new URI("http", host, path, query, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String request = uri.toASCIIString();
        if (!captchaPushList.containsKey(request)) {
            captchaPushList.put(request, (long) 0);
        }
        return true;
    }

    private void sendNewCaptcha(final SolverJob<?> job) {
        new Thread() {
            public void run() {
                long startTime = System.currentTimeMillis();
                long captchCount = 0;
                for (SolverJob<?> entry : ChallengeResponseController.getInstance().listJobs()) {
                    if (entry.isDone()) continue;
                    if (entry.getChallenge() instanceof ImageCaptchaChallenge) {
                        captchCount++;
                    }
                }
                for (Map.Entry<String, Long> entry : captchaPushList.entrySet()) {
                    if (startTime - entry.getValue() > 10 * 60 * 1000) {
                        try {
                            String request = entry.getKey();
                            ImageCaptchaChallenge<?> challenge = (ImageCaptchaChallenge<?>) job.getChallenge();
                            Long count = new Long(captchCount);
                            request = request.replace("%7BCaptchaCount%7D", count.toString());
                            getHTML(request);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        captchaPushList.put(entry.getKey(), startTime);
                    }
                }
            }
        }.start();
    }

    public String getHTML(String urlToRead) {
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(urlToRead);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
