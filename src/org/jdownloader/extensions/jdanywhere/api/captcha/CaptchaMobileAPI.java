package org.jdownloader.extensions.jdanywhere.api.captcha;

import java.util.List;

import jd.controlling.IOPermission;
import jd.controlling.captcha.CaptchaResult;

import org.appwork.remoteapi.ApiDoc;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiSessionRequired;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.jdownloader.api.captcha.CaptchaJob;

@ApiNamespace("mobile/captcha")
@ApiSessionRequired
public interface CaptchaMobileAPI extends RemoteAPIInterface {
    public static enum ABORT {
        SINGLE,
        HOSTER,
        ALL
    }

    @ApiDoc("returns a list of all available captcha jobs")
    public List<CaptchaJob> list();

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id);

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id, final boolean returnAsDataURL);

    public boolean solve(final long id, CaptchaResult result);

    public boolean abort(final long id, IOPermission.CAPTCHA what);

}
