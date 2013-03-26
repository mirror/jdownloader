package org.jdownloader.api.captcha;

import java.util.List;

import org.appwork.remoteapi.ApiDoc;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;

@ApiNamespace("captcha")
public interface CaptchaAPI extends RemoteAPIInterface {
    public static enum ABORT {
        SINGLE,
        HOSTER,
        ALL
    }

    @ApiDoc("returns a list of all available captcha jobs")
    public List<CaptchaJob> list();

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id);

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id, final boolean returnAsDataURL);

    public boolean solve(final long id, String result);

    public boolean abort(final long id);

}
