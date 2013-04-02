package org.jdownloader.extensions.jdanywhere.api;

import java.util.ArrayList;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.jdownloader.api.captcha.CaptchaAPIImpl;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.extensions.jdanywhere.api.interfaces.CAPTCHA;
import org.jdownloader.extensions.jdanywhere.api.interfaces.ICaptchaApi;
import org.jdownloader.extensions.jdanywhere.api.storable.CaptchaJob;

public class CaptchaApi implements ICaptchaApi {

    CaptchaAPIImpl cpAPI = new CaptchaAPIImpl();

    public List<CaptchaJob> list() {
        java.util.List<CaptchaJob> ret = new ArrayList<CaptchaJob>();
        for (SolverJob<?> entry : ChallengeResponseController.getInstance().listJobs()) {
            if (entry.isDone()) continue;
            if (entry.getChallenge() instanceof ImageCaptchaChallenge) {
                ImageCaptchaChallenge<?> challenge = (ImageCaptchaChallenge<?>) entry.getChallenge();
                CaptchaJob apiJob = new CaptchaJob();
                if (challenge.getResultType().isAssignableFrom(String.class))
                    apiJob.setType("Text");
                else
                    apiJob.setType("Click");
                // apiJob.setType(challenge.getClass().getSimpleName());
                apiJob.setID(challenge.getId().getID());
                apiJob.setHoster(challenge.getPlugin().getHost());
                apiJob.setCaptchaCategory(challenge.getExplain());
                ret.add(apiJob);
            }
        }
        return ret;
    }

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id) {
        cpAPI.get(request, response, id);
    }

    @Override
    public boolean solve(long id, String result) {
        return cpAPI.solve(id, result);
    }

    public boolean abort(long id, CAPTCHA what) {
        return cpAPI.abort(id);
    }

}
