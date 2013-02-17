package org.jdownloader.extensions.jdanywhere.api;

import java.util.HashMap;

import jd.controlling.captcha.CaptchaController;
import jd.controlling.captcha.CaptchaDialogQueueEntry;
import jd.controlling.captcha.CaptchaEventListener;
import jd.controlling.captcha.CaptchaEventSender;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadControllerEvent;
import jd.controlling.downloadcontroller.DownloadControllerListener;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.remoteapi.EventsAPIEvent;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.api.captcha.CaptchaJob;
import org.jdownloader.extensions.jdanywhere.JDAnywhereController;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class EventsAPI implements DownloadControllerListener, CaptchaEventListener, StateEventListener {

    HashMap<Long, String> linkStatusMessages = new HashMap<Long, String>();

    public EventsAPI() {
        DownloadController.getInstance().addListener(this, true);
        CaptchaEventSender.getInstance().addListener(this);
        DownloadWatchDog.getInstance().getStateMachine().addListener(this);
        CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.getEventSender().addListener(new GenericConfigEventListener<Integer>() {

            public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
                HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("message", "LimitspeedChanged");
                data.put("data", CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.getValue());
                JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("LimitspeedChanged", data), null);
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
                JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("LimitspeedActivated", data), null);
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
                        data.put("linkID", dl.getUniqueID().getID());
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
                        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("LinkstatusChanged", data), null);
                    } else {
                        LinkStatus linkStatus = dl.getLinkStatus();
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
                            JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("LinkstatusChanged", data), null);
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
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("downloadLinkAdded", data), null);
    }

    private void downloadApiLinkRemoved(DownloadLink link) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("linkID", link.getUniqueID().toString());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("downloadLinkRemoved", data), null);
    }

    private void downloadApiPackageAdded(FilePackage fpkg) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("packageID", fpkg.getUniqueID().toString());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("downloadPackageAdded", data), null);
    }

    private void downloadApiPackageRemoved(FilePackage fpkg) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("packageID", fpkg.getUniqueID().toString());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("downloadPackageRemoved", data), null);
    }

    public void captchaTodo(CaptchaController controller) {
        sendEvent(controller, "new");
    }

    public void captchaFinish(CaptchaController controller) {
        sendEvent(controller, "expired");
    }

    private void sendEvent(CaptchaController controller, String type) {
        CaptchaDialogQueueEntry entry = controller.getDialog();
        if (entry != null) {
            CaptchaJob job = new CaptchaJob();
            job.setType(entry.getCaptchaController().getCaptchaType());
            job.setID(entry.getID().getID());
            job.setHoster(entry.getHost().getTld());
            HashMap<String, Object> data = new HashMap<String, Object>();
            data.put("message", type);
            data.put("data", job);
            JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("captcha", data), null);
        }

    }

    public void onStateChange(StateEvent event) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("message", "Running State Changed");
        data.put("data", event.getNewState().getLabel());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("runningstate", data), null);
    }

    public void onStateUpdate(StateEvent event) {
    }

}
