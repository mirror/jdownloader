package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import jd.controlling.captcha.SkipRequest;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.RemoteAPIException;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserReference;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;
import org.jdownloader.captcha.v2.solver.gui.DialogBasicCaptchaSolver;

public abstract class RecaptchaV2Challenge extends AbstractBrowserChallenge {

    public static final class Recaptcha2FallbackChallenge extends BasicCaptchaChallenge {
        private RecaptchaV2Challenge          owner;
        private Browser                       iframe;
        private String                        challenge;
        private Form                          responseForm;
        private LinkedHashMap<String, String> responseMap;

        public Recaptcha2FallbackChallenge(RecaptchaV2Challenge challenge) {
            super(challenge.getTypeID(), null, null, challenge.getExplain(), challenge.getPlugin(), 0);
            this.owner = challenge;
            setAccountLogin(owner.isAccountLogin());
            URLConnectionAdapter conn = null;

            iframe = owner.getBr().cloneBrowser();

            load();
        }

        private void load() {

            URLConnectionAdapter conn = null;
            FileOutputStream fos = null;
            try {

                iframe.getPage("http://www.google.com/recaptcha/api2/demo");

                String dataSiteKey = owner.getSiteKey();
                iframe.getHeaders().put(new HTTPHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:37.0) Gecko/20100101 Firefox/37.0"));
                iframe.getHeaders().put(new HTTPHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));

                HTTPHeader cookie;
                iframe.getPage("http://www.google.com/recaptcha/api/fallback?k=" + dataSiteKey);

                boolean first = true;
                while (true) {
                    String payload = Encoding.htmlDecode(iframe.getRegex("<img src=\"(/recaptcha/api2/payload[^\"]+)").getMatch(0));

                    String challenge = iframe.getRegex("name=\"c\"\\s+value=\\s*\"([^\"]+)").getMatch(0);
                    System.out.println(challenge);
                    System.out.println("Challenge length: " + challenge.length());
                    conn = iframe.cloneBrowser().openGetConnection("http://www.google.com" + payload);

                    File file;

                    IO.readStreamToOutputStream(-1, conn.getInputStream(), fos = new FileOutputStream(file = Application.getResource("rc_" + System.currentTimeMillis() + ".jpg")), true);
                    conn.disconnect();
                    fos.close();
                    BufferedImage img = ImageIO.read(file);
                    // iframe.getHeaders().remove("Cookie");
                    String response = Dialog.getInstance().showInputDialog(0, "Recaptcha", first ? "Please Enter..." : "Wrong Captcha Input. Try again...", null, new ImageIcon(IconIO.getScaledInstance(img, img.getWidth() * 2, img.getHeight() * 2)), null, null);
                    Form form = iframe.getFormByInputFieldKeyValue("reason", "r");
                    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
                    map.put("c", Encoding.urlEncode(form.getInputField("c").getValue()));
                    map.put("response", Encoding.urlEncode(response));
                    // iframe.getHeaders().put(new HTTPHeader("Origin", "http://www.google.com"));
                    iframe.postPage("http://www.google.com/recaptcha/api/fallback?k=" + dataSiteKey, map);
                    System.out.println(iframe);
                    String token = iframe.getRegex("\"this\\.select\\(\\)\">(.*?)</textarea>").getMatch(0);
                    if (token != null) {
                        Dialog.getInstance().showConfirmDialog(0, "Result", "OK: " + response, new ImageIcon(IconIO.getScaledInstance(img, img.getWidth() * 2, img.getHeight() * 2)), null, null);
                        break;
                    }
                    Dialog.getInstance().showConfirmDialog(0, "Result", "WRONG: " + response, new ImageIcon(IconIO.getScaledInstance(img, img.getWidth() * 2, img.getHeight() * 2)), null, null);

                    first = false;
                }
                System.out.println(1);

            } catch (Throwable e) {
                throw new WTFException(e);
            } finally {
                try {
                    conn.disconnect();
                } catch (Throwable e) {

                }
                try {
                    fos.close();
                } catch (Throwable e) {

                }
            }
        }

        @Override
        public boolean validateResponse(AbstractResponse<String> response) {
            try {

                responseMap.put("response", Encoding.urlEncode(response.getValue()));
                // iframe.getHeaders().put(new HTTPHeader("Origin", "http://www.google.com"));
                iframe.postPage("http://www.google.com/recaptcha/api/fallback?k=" + owner.getSiteKey(), responseMap);
                System.out.println(iframe.toString());
                String token = iframe.getRegex("\"this\\.select\\(\\)\">(.*?)</textarea>").getMatch(0);
                if (token != null) {
                    return true;
                }
            } catch (Throwable e) {

                throw new WTFException(e);
            }
            return false;

        }

        @Override
        public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
            return owner.canBeSkippedBy(skipRequest, solver, challenge);
        }
    }

    public static final boolean   FALLBACK_ENABLED = false;

    private String                siteKey;
    private BasicCaptchaChallenge basicChallenge;

    public String getSiteKey() {
        return siteKey;
    }

    @Override
    public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds) {
        Rectangle rect = null;
        int sleep = 500;
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            rect = screenResource.getRectangleByColor(0xff9900, 0, 0, 1d, elementBounds.x, elementBounds.y);
            if (rect == null) {
                sleep *= 2;

                continue;
            }
            break;
        }
        return new Recaptcha2BrowserViewport(screenResource, rect, elementBounds);
    }

    public RecaptchaV2Challenge(String siteKey, Plugin pluginForHost) {
        super("recaptchav2", pluginForHost);
        this.siteKey = siteKey;
        if (siteKey == null || !siteKey.matches("^[\\w-]+$")) {
            throw new WTFException("Bad SiteKey");
        }

    }

    @Override
    public boolean onGetRequest(BrowserReference browserReference, GetRequest request, HttpResponse response) throws IOException, RemoteAPIException {
        String pDo = request.getParameterbyKey("do");
        if ("solve".equals(pDo)) {
            String responsetoken = request.getParameterbyKey("response");

            browserReference.onResponse(responsetoken);
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

            response.getOutputStream(true).write("Please Close the Browser now".getBytes("UTF-8"));
            return true;
        }
        return false;
    }

    @Override
    public String getHTML() {
        String html;
        try {
            URL url = RecaptchaV2Challenge.class.getResource("recaptcha.html");
            html = IO.readURLToString(url);

            html = html.replace("%%%sitekey%%%", siteKey);
            return html;
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }

    @Override
    public boolean validateResponse(AbstractResponse<String> response) {
        if (response.getSolver() instanceof DialogBasicCaptchaSolver) {
            return basicChallenge.validateResponse(response);
        }
        return true;
    }

    public synchronized BasicCaptchaChallenge createBasicCaptchaChallenge() {

        if (basicChallenge != null) {
            return basicChallenge;
        }

        basicChallenge = new Recaptcha2FallbackChallenge(this);

        return basicChallenge;
    }
}
