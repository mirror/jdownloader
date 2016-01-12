package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.imageio.ImageIO;

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
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserReference;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;
import org.jdownloader.controlling.UniqueAlltimeID;

public class RecaptchaV2Challenge extends AbstractBrowserChallenge {

    public static final String RECAPTCHAV2 = "recaptchav2";

    public static final class Recaptcha2FallbackChallenge extends BasicCaptchaChallenge {
        private final RecaptchaV2Challenge owner;
        private final Browser              iframe;
        private String                     challenge;
        private String                     payload;
        private String                     token;

        @Override
        public Object getAPIStorable(String format) throws Exception {
            if ("single".equals(format)) {
                final HashMap<String, Object> data = new HashMap<String, Object>();
                data.put("instructions", "Type 145 if image 1,4 and 5 match the question above.");
                data.put("explain", getExplain());
                final ArrayList<String> images = new ArrayList<String>();
                data.put("images", images);
                final BufferedImage img = ImageIO.read(getImageFile());
                int columnWidth = img.getWidth() / 3;
                int rowHeight = img.getHeight() / 3;
                final Font font = new Font("Arial", 0, 12).deriveFont(Font.BOLD);
                for (int yslot = 0; yslot < 3; yslot++) {
                    for (int xslot = 0; xslot < 3; xslot++) {
                        int xx = (xslot) * columnWidth;
                        int yy = (yslot) * rowHeight;
                        int num = xslot + yslot * 3 + 1;
                        final BufferedImage jpg = new BufferedImage(columnWidth, rowHeight, BufferedImage.TYPE_INT_RGB);
                        final Graphics g = jpg.getGraphics();
                        // g.drawImage(img, xx, yy, columnWidth, rowHeight, null);
                        g.setFont(font);
                        g.drawImage(img, 0, 0, columnWidth, rowHeight, xx, yy, xx + columnWidth, yy + rowHeight, null);
                        g.setColor(Color.WHITE);
                        g.fillRect(columnWidth - 20, 0, 20, 20);
                        g.setColor(Color.BLACK);
                        g.drawString(num + "", columnWidth - 20 + 5, 0 + 15);
                        g.dispose();
                        images.add(IconIO.toDataUrl(jpg));
                    }
                }
                return data;
            } else {
                // String mime = FileResponse.getMimeType(getImageFile().getName());
                final BufferedImage newImage = getAnnotatedImage();
                final String ret = IconIO.toDataUrl(newImage);
                return ret;
            }
        }

        @Override
        public BufferedImage getAnnotatedImage() throws IOException {
            final BufferedImage img = ImageIO.read(getImageFile());
            final Font font = new Font("Arial", 0, 12);
            final String instructions = "Type 145 if image 1,4 and 5 match the question above.";
            final int explainWidth = img.getGraphics().getFontMetrics(font.deriveFont(Font.BOLD)).stringWidth(getExplain()) + 10;
            final int solutionWidth = img.getGraphics().getFontMetrics(font).stringWidth(instructions) + 10;
            final BufferedImage newImage = IconIO.createEmptyImage(Math.max(Math.max(explainWidth, solutionWidth), img.getWidth()), img.getHeight() + 4 * 20);
            final Graphics2D g = (Graphics2D) newImage.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());
            g.setColor(Color.WHITE);
            g.setFont(font.deriveFont(Font.BOLD));
            int y = 0;
            g.drawString(getExplain(), 5, y += 20);
            g.setFont(font);
            g.drawString("Instructions:", 5, y += 20);
            g.drawString(instructions, 5, y += 20);
            y += 15;
            int xOffset;
            g.drawImage(img, xOffset = (newImage.getWidth() - img.getWidth()) / 2, y, null);
            g.setFont(new Font("Arial", 0, 16).deriveFont(Font.BOLD));
            int columnWidth = img.getWidth() / 3;
            int rowHeight = img.getHeight() / 3;
            for (int yslot = 0; yslot < 3; yslot++) {
                for (int xslot = 0; xslot < 3; xslot++) {
                    int xx = (xslot) * columnWidth;
                    int yy = (yslot) * rowHeight;
                    int num = xslot + yslot * 3 + 1;
                    g.setColor(Color.WHITE);
                    g.fillRect(xx + columnWidth - 20 + xOffset, yy + y, 20, 20);
                    g.setColor(Color.BLACK);
                    g.drawString(num + "", xx + columnWidth - 20 + xOffset + 5, yy + y + 15);
                }
            }
            g.dispose();
            return newImage;
        }

        @Override
        public AbstractResponse<String> parseAPIAnswer(String json, ChallengeSolver<?> solver) {
            json = json.replaceAll("[^\\d]+", "");
            final StringBuilder sb = new StringBuilder();
            final HashSet<String> dupe = new HashSet<String>();
            for (int i = 0; i < json.length(); i++) {
                if (dupe.add(json.charAt(i) + "")) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(Integer.parseInt(json.charAt(i) + ""));
                }
            }
            return new CaptchaResponse(this, solver, sb.toString(), sb.toString().length() == 0 || sb.toString().length() > 5 ? 0 : 100);
        }

        public Recaptcha2FallbackChallenge(RecaptchaV2Challenge challenge) {
            super(challenge.getTypeID(), null, null, challenge.getExplain(), challenge.getPlugin(), 0);
            this.owner = challenge;
            setAccountLogin(owner.isAccountLogin());
            iframe = owner.getBr().cloneBrowser();
            load();
        }

        @Override
        public UniqueAlltimeID getId() {
            return owner.getId();
        }

        private void load() {
            try {
                final String dataSiteKey = owner.getSiteKey();
                if (round == 1) {
                    iframe.getPage("http://www.google.com/recaptcha/api2/demo");
                    iframe.getHeaders().put(new HTTPHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:37.0) Gecko/20100101 Firefox/37.0"));
                    iframe.getHeaders().put(new HTTPHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
                    iframe.getHeaders().put(new HTTPHeader("Accept-Language", TranslationFactory.getDesiredLanguage()));
                    iframe.getPage("http://www.google.com/recaptcha/api/fallback?k=" + dataSiteKey);
                }
                payload = Encoding.htmlDecode(iframe.getRegex("\"(/recaptcha/api2/payload[^\"]+)").getMatch(0));
                String message = Encoding.htmlDecode(iframe.getRegex("<label .*?class=\"fbc-imageselect-message-text\">(.*?)</label>").getMatch(0));
                if (message == null) {
                    message = Encoding.htmlDecode(iframe.getRegex("<div .*?class=\"fbc-imageselect-message-error\">(.*?)</div>").getMatch(0));
                }
                if (message != null) {
                    setExplain("Round #" + round + "/2: " + message.replaceAll("<.*?>", "").replaceAll("\\s+", " "));
                }
                challenge = iframe.getRegex("name=\"c\"\\s+value=\\s*\"([^\"]+)").getMatch(0);
                setImageFile(Application.getResource("rc_" + System.currentTimeMillis() + ".jpg"));
                FileOutputStream fos = null;
                URLConnectionAdapter con = null;
                try {
                    con = iframe.cloneBrowser().openGetConnection("http://www.google.com" + payload);
                    fos = new FileOutputStream(getImageFile());
                    IO.readStreamToOutputStream(-1, con.getInputStream(), fos, true);
                } finally {
                    try {
                        fos.close();
                    } catch (final Throwable ignore) {
                    }
                    try {
                        con.disconnect();
                    } catch (final Throwable ignore) {
                    }
                }
            } catch (Throwable e) {
                throw new WTFException(e);
            }
        }

        public String getChallenge() {
            return challenge;
        }

        @Override
        public boolean validateResponse(AbstractResponse<String> response) {
            try {
                final String dataSiteKey = owner.getSiteKey();
                final Form form = iframe.getFormbyKey("c");
                String responses = "";
                final HashSet<String> dupe = new HashSet<String>();
                final String re = response.getValue().replaceAll("[^\\d]+", "");
                for (int i = 0; i < re.length(); i++) {
                    final int num = Integer.parseInt(response.getValue().replaceAll("[^\\d]+", "").charAt(i) + "") - 1;
                    if (dupe.add(Integer.toString(num))) {
                        responses += "&response=" + num;
                    }
                }
                // iframe.getHeaders().put(new HTTPHeader("Origin", "http://www.google.com"));
                iframe.postPageRaw("http://www.google.com/recaptcha/api/fallback?k=" + dataSiteKey, "c=" + Encoding.urlEncode(form.getInputField("c").getValue()) + responses);
                token = iframe.getRegex("\"this\\.select\\(\\)\">(.*?)</textarea>").getMatch(0);
            } catch (Throwable e) {
                throw new WTFException(e);
            }
            // always return true. recaptchav2 fallback requires several captchas. we need to accept all answers. validation will be done
            // later
            return true;
        }

        public String getToken() {
            return token;
        }

        @Override
        public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
            return owner.canBeSkippedBy(skipRequest, solver, challenge);
        }

        private int round = 1;

        public void reload(int i, String lastResponse) throws IOException {
            round = i;
            load();
        }
    }

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
        super(RECAPTCHAV2, pluginForHost);
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
        try {
            final URL url = RecaptchaV2Challenge.class.getResource("recaptcha.html");
            String html = IO.readURLToString(url);
            html = html.replace("%%%sitekey%%%", siteKey);
            return html;
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }

    /**
     * Used to validate result against expected pattern. <br />
     * This is different to AbstractBrowserChallenge.isSolved, as we don't want to throw the same error exception.
     *
     * @param result
     * @return
     * @author raztoki
     */
    protected final boolean isCaptchaResponseValid() {
        if (isSolved() && getResult().getValue().matches("[\\w-]{30,}")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean validateResponse(AbstractResponse<String> response) {
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
