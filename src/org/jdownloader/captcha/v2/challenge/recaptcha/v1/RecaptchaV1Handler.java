package org.jdownloader.captcha.v2.challenge.recaptcha.v1;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.captcha.SkipRequest;
import jd.http.Browser;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserReference;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;
import org.jdownloader.captcha.v2.solver.service.BrowserSolverService;

public abstract class RecaptchaV1Handler {

    public static String load(Browser rcBr, final String siteKey) throws IOException, InterruptedException {
        if (Application.isHeadless()) {
            return null;
        }
        if (!BrowserSolverService.getInstance().getConfig().isBrowserLoopEnabled()) {
            return null;
        }
        final AtomicReference<String> url = new AtomicReference<String>();

        AbstractBrowserChallenge dummyChallenge = new AbstractBrowserChallenge("recaptcha", null) {

            @Override
            public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
                return false;
            }

            @Override
            public String getHTML() {
                String html;
                try {
                    URL url = RecaptchaV1Handler.class.getResource("recaptchaGetChallenge.html");
                    html = IO.readURLToString(url);

                    html = html.replace("%%%sitekey%%%", siteKey);
                    return html;
                } catch (IOException e) {
                    throw new WTFException(e);
                }
            }

            @Override
            public boolean onGetRequest(BrowserReference browserReference, GetRequest request, HttpResponse response) throws IOException {
                String pDo = request.getParameterbyKey("do");
                if (pDo.equals("setChallenge")) {
                    url.set(request.getParameterbyKey("url"));

                    response.getOutputStream(true).write("true".getBytes("UTF-8"));
                    synchronized (url) {
                        url.notifyAll();
                    }
                    return true;
                }
                return super.onGetRequest(browserReference, request, response);
            }

            @Override
            public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds) {
                return null;
            }
        };
        BrowserReference ref = new BrowserReference(dummyChallenge) {

            @Override
            public void onResponse(String request) {
            }

        };
        ref.open();
        try {
            synchronized (url) {
                url.wait(15000);
            }
        } finally {
            ref.dispose();
        }

        String urlString = url.get();
        if (StringUtils.isEmpty(urlString)) {
            return null;
        }

        return urlString.substring(urlString.indexOf("c=") + 2);
    }

}
