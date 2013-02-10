package org.jdownloader.extensions.jdanywhere.api.captcha;

import java.util.List;

import jd.controlling.IOPermission;
import jd.controlling.captcha.CaptchaResult;

import org.appwork.remoteapi.ApiDoc;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.jdownloader.api.captcha.CaptchaJob;
import org.jdownloader.extensions.jdanywhere.JDAnywhereAPI;

@ApiNamespace("mobile/captcha")
public interface CaptchaMobileAPI extends RemoteAPIInterface, JDAnywhereAPI {
    public static enum ABORT {
        SINGLE,
        HOSTER,
        ALL
    }

    @ApiDoc("returns a list of all available captcha jobs")
    public List<CaptchaJob> list(final String username, final String password);

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id, final String username, final String password);

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id, final boolean returnAsDataURL, final String username, final String password);

    public boolean solve(final long id, CaptchaResult result, final String username, final String password);

    public boolean abort(final long id, IOPermission.CAPTCHA what, final String username, final String password);

}
