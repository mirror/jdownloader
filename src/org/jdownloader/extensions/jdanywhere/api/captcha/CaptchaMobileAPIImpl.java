package org.jdownloader.extensions.jdanywhere.api.captcha;

import java.util.HashMap;
import java.util.List;

import jd.controlling.IOPermission;
import jd.controlling.captcha.CaptchaController;
import jd.controlling.captcha.CaptchaDialogQueueEntry;
import jd.controlling.captcha.CaptchaEventListener;
import jd.controlling.captcha.CaptchaEventSender;
import jd.controlling.captcha.CaptchaResult;

import org.appwork.remoteapi.EventsAPIEvent;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.jdownloader.api.captcha.CaptchaAPIImpl;
import org.jdownloader.api.captcha.CaptchaJob;
import org.jdownloader.extensions.jdanywhere.JDAnywhereController;

public class CaptchaMobileAPIImpl implements CaptchaMobileAPI, CaptchaEventListener {

    CaptchaAPIImpl cpAPI = new CaptchaAPIImpl();

    public CaptchaMobileAPIImpl() {
        CaptchaEventSender.getInstance().addListener(this);
    }

    public List<CaptchaJob> list() {
        return cpAPI.list();
    }

    public void get(RemoteAPIRequest request, RemoteAPIResponse response, long id, final boolean returnAsDataURL) {
        cpAPI.get(request, response, id, returnAsDataURL);
    }

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id) {
        cpAPI.get(request, response, id, false);
    }

    public boolean solve(long id, CaptchaResult result) {
        return cpAPI.solve(id, result);
    }

    public boolean abort(long id, IOPermission.CAPTCHA what) {
        return cpAPI.abort(id, what);
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

}
