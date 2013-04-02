package org.jdownloader.extensions.jdanywhere.api.interfaces;

import java.util.List;

import org.appwork.remoteapi.ApiDoc;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiSessionRequired;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.jdownloader.extensions.jdanywhere.api.storable.CaptchaJob;

@ApiNamespace("jdanywhere/captcha")
@ApiSessionRequired
public interface ICaptchaApi extends RemoteAPIInterface {
    public static enum ABORT {
        SINGLE,
        HOSTER,
        ALL
    }

    @ApiDoc("returns a list of all available captcha jobs")
    public List<CaptchaJob> list();

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id);

    public boolean solve(final long id, String result);

    public boolean abort(final long id, CAPTCHA what);

}
