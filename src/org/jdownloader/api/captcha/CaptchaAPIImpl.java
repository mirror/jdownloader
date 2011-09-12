package org.jdownloader.api.captcha;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.captcha.CaptchaDialogQueue;
import jd.controlling.captcha.CaptchaDialogQueueEntry;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.net.httpserver.responses.FileResponse;

public class CaptchaAPIImpl implements CaptchaAPI {

    public List<CaptchaJob> list() {
        List<CaptchaDialogQueueEntry> entries = CaptchaDialogQueue.getInstance().getEntries();
        ArrayList<CaptchaJob> ret = new ArrayList<CaptchaJob>();
        for (CaptchaDialogQueueEntry entry : entries) {
            if (entry.isFinished()) continue;
            CaptchaJob job = new CaptchaJob();
            job.setCaptchaID(entry.getID() + "");
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
}
