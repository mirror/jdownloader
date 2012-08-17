package org.jdownloader.api.captcha;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.controlling.IOPermission;
import jd.controlling.captcha.CaptchaController;
import jd.controlling.captcha.CaptchaDialogQueue;
import jd.controlling.captcha.CaptchaDialogQueueEntry;
import jd.controlling.captcha.CaptchaEventListener;
import jd.controlling.captcha.CaptchaEventSender;
import jd.controlling.captcha.CaptchaResult;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.EventsAPIEvent;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.ReusableByteArrayOutputStreamPool;
import org.appwork.utils.ReusableByteArrayOutputStreamPool.ReusableByteArrayOutputStream;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.Base64OutputStream;
import org.appwork.utils.net.httpserver.responses.FileResponse;
import org.jdownloader.api.RemoteAPIController;

public class CaptchaAPIImpl implements CaptchaAPI, CaptchaEventListener {

    public CaptchaAPIImpl() {
        CaptchaEventSender.getInstance().addListener(this);
    }

    public List<CaptchaJob> list() {
        List<CaptchaDialogQueueEntry> entries = CaptchaDialogQueue.getInstance().getJobs();
        java.util.List<CaptchaJob> ret = new ArrayList<CaptchaJob>();
        for (CaptchaDialogQueueEntry entry : entries) {
            if (entry.isFinished()) continue;
            CaptchaJob job = new CaptchaJob();
            job.setID(entry.getID().getID());
            job.setHoster(entry.getHost().getTld());
            ret.add(job);
        }
        return ret;
    }

    public void get(RemoteAPIRequest request, RemoteAPIResponse response, long id, final boolean returnAsDataURL) {
        CaptchaDialogQueueEntry captcha = CaptchaDialogQueue.getInstance().getCaptchabyID(id);
        if (captcha == null || captcha.getCaptchaFile() == null || !captcha.getCaptchaFile().exists()) { throw new RemoteAPIException(ResponseCode.ERROR_NOT_FOUND, "Captcha no longer available"); }
        try {
            if (returnAsDataURL) {
                OutputStream out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), true);
                String mime = FileResponse.getMimeType(captcha.getCaptchaFile().getName());
                String header = "{\r\n\"data\":\"" + mime + ";base64,";
                out.write(header.getBytes("UTF-8"));
                Base64OutputStream b64os = new Base64OutputStream(out);
                FileInputStream fis = null;
                ReusableByteArrayOutputStream ros = null;
                try {
                    fis = new FileInputStream(captcha.getCaptchaFile());
                    ros = ReusableByteArrayOutputStreamPool.getReusableByteArrayOutputStream(1024);
                    int read = 0;
                    while ((read = fis.read(ros.getInternalBuffer())) >= 0) {
                        if (read > 0) {
                            b64os.write(ros.getInternalBuffer(), 0, read);
                        } else {
                            synchronized (this) {
                                try {
                                    this.wait(500);
                                } catch (final InterruptedException e) {
                                    throw new IOException(e);
                                }
                            }
                        }
                    }
                    b64os.flush();
                    out.write("\"\r\n}".getBytes("UTF-8"));
                } finally {
                    try {
                        /*
                         * we need to close the b64 stream which closes the RemoteAPI Outputstream
                         */
                        b64os.close();
                    } catch (final Throwable e) {
                    }
                    try {
                        fis.close();
                    } catch (final Throwable e) {
                    }
                    try {
                        ReusableByteArrayOutputStreamPool.reuseReusableByteArrayOutputStream(ros);
                    } catch (final Throwable e) {
                    }
                }
            } else {
                final FileResponse fr = new FileResponse(request, response, captcha.getCaptchaFile()) {

                    protected boolean useContentDisposition() {
                        /* we want the image to be displayed in browser */
                        return false;
                    }
                };
                fr.sendFile();
            }
        } catch (IOException e) {
            Log.exception(e);
            throw new RemoteAPIException(e);
        }
    }

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id) {
        get(request, response, id, false);
    }

    public boolean solve(long id, CaptchaResult result) {
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
                io.setCaptchaAllowed(captcha.getHost().getTld(), what);
            }
        }
        return true;
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
            job.setID(entry.getID().getID());
            job.setHoster(entry.getHost().getTld());
            HashMap<String, Object> data = new HashMap<String, Object>();
            data.put("message", type);
            data.put("data", job);
            RemoteAPIController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("captcha", data), null);
        }

    }

}
