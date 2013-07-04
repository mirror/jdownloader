package org.jdownloader.api.jdanywhere.api;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.captcha.SkipRequest;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.remoteapi.exceptions.RemoteAPIException;
import org.jdownloader.api.captcha.CaptchaAPISolver;
import org.jdownloader.api.jdanywhere.api.interfaces.CAPTCHA;
import org.jdownloader.api.jdanywhere.api.interfaces.ICaptchaApi;
import org.jdownloader.api.jdanywhere.api.storable.CaptchaJob;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class CaptchaApi implements ICaptchaApi {

    CaptchaAPISolver cpAPI = CaptchaAPISolver.getInstance();

    public List<CaptchaJob> list() {
        java.util.List<CaptchaJob> ret = new ArrayList<CaptchaJob>();
        for (SolverJob<?> entry : cpAPI.listJobs()) {
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

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id) throws InternalApiException, RemoteAPIException {
        cpAPI.get(request, response, id);
    }

    @Override
    public boolean solve(long id, String result) throws RemoteAPIException {
        return cpAPI.solve(id, result);
    }

    public boolean abort(long id, CAPTCHA what) throws RemoteAPIException {
        return cpAPI.skip(id, SkipRequest.SINGLE);
    }

}
