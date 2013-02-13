package org.jdownloader.extensions.jdanywhere.api;

import java.util.List;

import jd.controlling.IOPermission;
import jd.controlling.captcha.CaptchaResult;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.jdownloader.api.captcha.CaptchaAPIImpl;
import org.jdownloader.api.captcha.CaptchaJob;
import org.jdownloader.extensions.jdanywhere.api.interfaces.ICaptchaApi;

public class CaptchaApi implements ICaptchaApi {

    CaptchaAPIImpl cpAPI = new CaptchaAPIImpl();

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

}
