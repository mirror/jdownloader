package org.jdownloader.api.captcha;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.controlling.IOPermission;
import jd.controlling.captcha.CaptchaController;
import jd.controlling.captcha.CaptchaDialogQueue;
import jd.controlling.captcha.CaptchaDialogQueueEntry;
import jd.controlling.captcha.CaptchaEventListener;
import jd.controlling.captcha.CaptchaEventSender;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.EventsAPIEvent;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.net.httpserver.responses.FileResponse;
import org.jdownloader.api.RemoteAPIController;

public class CaptchaAPIImpl implements CaptchaAPI, CaptchaEventListener {

    public CaptchaAPIImpl() {
        CaptchaEventSender.getInstance().addListener(this);
    }

    public List<CaptchaJob> list() {
        List<CaptchaDialogQueueEntry> entries = CaptchaDialogQueue.getInstance().getEntries();
        ArrayList<CaptchaJob> ret = new ArrayList<CaptchaJob>();
        for (CaptchaDialogQueueEntry entry : entries) {
            if (entry.isFinished()) continue;
            CaptchaJob job = new CaptchaJob();
            job.setCaptchaID(entry.getID().getID());
            job.setHosterID(entry.getHost());
            ret.add(job);
        }
        return ret;
    }

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id) {
        CaptchaDialogQueueEntry captcha = CaptchaDialogQueue.getInstance().getCaptchabyID(id);
        if (captcha == null) { throw new RemoteAPIException(ResponseCode.ERROR_NOT_FOUND, "Captcha no longer available"); }
        final FileResponse fr = new FileResponse(request, response, captcha.getFile()) {

            @Override
            protected boolean allowGZIP() {
                /* chrome has issues with chunked content */
                return false;
            }

            protected boolean useContentDisposition() {
                /* we want the image to be displayed in browser */
                return false;
            }
        };
        try {
            fr.sendFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean solve(long id, String result) {
        CaptchaDialogQueueEntry captcha = CaptchaDialogQueue.getInstance().getCaptchabyID(id);
        if (captcha == null) return false;
        captcha.setResponse(result);
        return true;
    }

    public boolean abort(long id, IOPermission.CAPTCHA what) {
        CaptchaDialogQueueEntry captcha = CaptchaDialogQueue.getInstance().getCaptchabyID(id);
        if (captcha == null) return false;
        IOPermission io = captcha.getIOPermission();
        if (io != null) {
            if (IOPermission.CAPTCHA.BLOCKTHIS == what) {
                captcha.setResponse(null);
            } else {
                io.setCaptchaAllowed(captcha.getHost(), what);
            }
        }
        return true;
    }

    public void captchaTodo(CaptchaController controller) {
        CaptchaDialogQueueEntry entry = controller.getDialog();
        if (entry != null) {
            CaptchaJob job = new CaptchaJob();
            job.setCaptchaID(entry.getID().getID());
            job.setHosterID(entry.getHost());
            EventsAPIEvent event = new EventsAPIEvent();
            HashMap<String, Object> data = new HashMap<String, Object>();
            data.put("module", "captcha");
            data.put("type", "new");
            data.put("data", job);
            event.setData(data);
            RemoteAPIController.getInstance().getEventsapi().publishEvent(event, null);
        }
    }

    public void captchaFinish(CaptchaController controller) {
    }
}
