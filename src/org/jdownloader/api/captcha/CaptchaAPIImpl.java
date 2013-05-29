package org.jdownloader.api.captcha;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.remoteapi.exceptions.RemoteAPIException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.ReusableByteArrayOutputStream;
import org.appwork.utils.ReusableByteArrayOutputStreamPool;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.Base64OutputStream;
import org.appwork.utils.net.httpserver.responses.FileResponse;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ClickCaptchaResponse;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class CaptchaAPIImpl implements CaptchaAPI {

    public CaptchaAPIImpl() {
        // ChallengeResponseController.getInstance().getEventSender().addListener(this);
    }

    public List<CaptchaJob> list() {

        java.util.List<CaptchaJob> ret = new ArrayList<CaptchaJob>();
        for (SolverJob<?> entry : ChallengeResponseController.getInstance().listJobs()) {
            if (entry.isDone()) continue;
            if (entry.getChallenge() instanceof ImageCaptchaChallenge) {
                CaptchaJob job = new CaptchaJob();
                Class<?> cls = entry.getChallenge().getClass();
                while (cls != null && StringUtils.isEmpty(job.getType())) {
                    job.setType(cls.getSimpleName());
                    cls = cls.getSuperclass();
                }

                job.setID(entry.getChallenge().getId().getID());
                job.setHoster(((ImageCaptchaChallenge) entry.getChallenge()).getPlugin().getHost());
                job.setCaptchaCategory(entry.getChallenge().getTypeID());
                ret.add(job);
            }

        }
        return ret;
    }

    public void get(RemoteAPIRequest request, RemoteAPIResponse response, long id) throws InternalApiException, RemoteAPIException {
        SolverJob<?> job = ChallengeResponseController.getInstance().getJobById(id);
        if (job == null || !(job.getChallenge() instanceof ImageCaptchaChallenge) || job.isDone()) { throw new RemoteAPIException(CaptchaAPI.Error.NOT_AVAILABLE); }

        ImageCaptchaChallenge<?> challenge = (ImageCaptchaChallenge<?>) job.getChallenge();
        try {
            OutputStream out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), true);
            String mime = FileResponse.getMimeType(challenge.getImageFile().getName());
            String header = "{\r\n\"data\":\"" + mime + ";base64,";
            out.write(header.getBytes("UTF-8"));
            Base64OutputStream b64os = new Base64OutputStream(out);
            FileInputStream fis = null;
            ReusableByteArrayOutputStream ros = null;
            try {
                fis = new FileInputStream(challenge.getImageFile());
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
                b64os.flush(true);
                out.write("\"\r\n}".getBytes("UTF-8"));
            } finally {
                try {
                    out.close();
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
        } catch (IOException e) {
            Log.exception(e);
            throw new InternalApiException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public boolean solve(long id, String result) throws RemoteAPIException {

        SolverJob<?> job = ChallengeResponseController.getInstance().getJobById(id);
        if (job == null || !(job.getChallenge() instanceof ImageCaptchaChallenge) || job.isDone()) { throw new RemoteAPIException(CaptchaAPI.Error.NOT_AVAILABLE); }

        ImageCaptchaChallenge<?> challenge = (ImageCaptchaChallenge<?>) job.getChallenge();

        if (challenge instanceof BasicCaptchaChallenge) {
            String res = JSonStorage.restoreFromString("\"" + result + "\"", new TypeRef<String>() {
            });

            ((SolverJob<String>) job).addAnswer(new CaptchaResponse((BasicCaptchaChallenge) challenge, null, res, 100));
        } else if (challenge instanceof ClickCaptchaChallenge) {
            ClickedPoint res = JSonStorage.restoreFromString(result, new TypeRef<ClickedPoint>() {
            });

            ((SolverJob<ClickedPoint>) job).addAnswer(new ClickCaptchaResponse((ClickCaptchaChallenge) challenge, this, res, 100));
        } else {
            throw new RemoteAPIException(CaptchaAPI.Error.UNKNOWN_CHALLENGETYPE, challenge.getClass().getName());

        }

        return true;
    }

    public boolean abort(long id) throws RemoteAPIException {
        SolverJob<?> job = ChallengeResponseController.getInstance().getJobById(id);
        if (job == null || !(job.getChallenge() instanceof ImageCaptchaChallenge) || job.isDone()) { throw new RemoteAPIException(CaptchaAPI.Error.NOT_AVAILABLE); }

        // ImageCaptchaChallenge<?> challenge = (ImageCaptchaChallenge<?>) job.getChallenge();
        job.kill();
        return true;
    }

    @SuppressWarnings("static-access")
    public boolean skip(long id) throws RemoteAPIException {
        // SolverJob<?> job = ChallengeResponseController.getInstance().getJobById(id);
        // if (job == null || !(job.getChallenge() instanceof ImageCaptchaChallenge) || job.isDone()) { throw new
        // RemoteAPIException(CaptchaAPI.Error.NOT_AVAILABLE); }
        //
        // // ImageCaptchaChallenge<?> challenge = (ImageCaptchaChallenge<?>) job.getChallenge();
        // job.kill();

        SolverJob<?> job = ChallengeResponseController.getInstance().getJobById(id);
        job.getChallenge().getDownloadLink(job.getChallenge()).setSkipped(true);

        return true;
    }

    @Override
    public CaptchaJob getCaptchaJob(long id) {
        SolverJob<?> entry = ChallengeResponseController.getInstance().getJobById(id);
        CaptchaJob ret = new CaptchaJob();

        Class<?> cls = entry.getChallenge().getClass();
        while (cls != null && StringUtils.isEmpty(ret.getType())) {
            ret.setType(cls.getSimpleName());
            cls = cls.getSuperclass();
        }

        ret.setID(entry.getChallenge().getId().getID());
        ret.setHoster(((ImageCaptchaChallenge) entry.getChallenge()).getPlugin().getHost());
        ret.setCaptchaCategory(entry.getChallenge().getTypeID());
        ret.setExplain(entry.getChallenge().getExplain());

        if (entry.getChallenge().getDownloadLink(entry.getChallenge()) != null) {
            ret.setLink(entry.getChallenge().getDownloadLink(entry.getChallenge()).getUniqueID().getID());
        }

        return ret;
    }
    // public void captchaTodo(CaptchaHandler controller) {
    // sendEvent(controller, "new");
    // }
    //
    // public void captchaFinish(CaptchaHandler controller) {
    // sendEvent(controller, "expired");
    // }
    //
    // private void sendEvent(CaptchaHandler controller, String type) {
    // BasicCaptchaDialogHandler entry = controller.getDialog();
    // if (entry != null) {
    // CaptchaJob job = new CaptchaJob();
    // job.setType(entry.getCaptchaController().getCaptchaType());
    // job.setID(entry.getID().getID());
    // job.setHoster(entry.getHost().getTld());
    // HashMap<String, Object> data = new HashMap<String, Object>();
    // data.put("message", type);
    // data.put("data", job);
    // RemoteAPIController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("captcha", data), null);
    // }
    //
    // }

}
