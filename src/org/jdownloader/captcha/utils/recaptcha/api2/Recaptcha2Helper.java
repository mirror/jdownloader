package org.jdownloader.captcha.utils.recaptcha.api2;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.utils.JDUtilities;

import org.appwork.utils.IO;
import org.appwork.utils.Regex;

public class Recaptcha2Helper {

    private String  siteKey;
    private Browser br;
    private String  version  = "r20141202135649";
    private String  language = "en";

    // private HashMap<String, String> apiProperties;
    private long    rpcToken;
    private String  iframeId;
    private String  jshString;
    private String  host;
    private String  tokenForFrameLoading;
    private String  tokenToReload;
    private String  tokenForCaptchaChallengePayload;
    private long    timeImageLoading;
    private String  botGuardString;
    private String  responseToken;
    private boolean success;
    private int     successIdentifier;
    private int     timeout;
    private long    verifyTime;

    public static boolean isEmpty(final String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    protected void initBrowser() {
        br = new Browser();
    }

    public void init(final Browser br) throws RecaptchaException, IOException {
        this.br = br.cloneBrowser();
        this.host = new URL(br.getURL()).getHost();
        this.siteKey = new Regex(br.toString(), "data-sitekey\\s*=\\s*\"([^\"]+)").getMatch(0);
        if (isEmpty(siteKey)) {
            throw new RecaptchaException("Parsing Error: Could not find data-sitekey");
        }
        // lets use static version and language
        // initStaticRecaptchaScript();
        initGoogleApiProperties();
    }

    /**
     * Use when hoster doesn't give public API key via standard recaptchav2 html, or at all!<br />
     * Browser can be null and new Browser session will be used. Be aware it will be using JDownloader default User-Agent <br />
     * Provided Browser referer is nullfied. <br />
     * Host can be null only when existing Browser is provided, host will be determined from current URL. <br />
     *
     *
     * @author raztoki
     * @param br
     * @param siteKey
     * @param host
     * @throws RecaptchaException
     * @throws IOException
     */
    public void init(final Browser br, final String siteKey, final String host) throws RecaptchaException, IOException {
        if (siteKey == null) {
            throw new RecaptchaException("'siteKey' can not be null");
        }
        if (br == null) {
            if (host == null) {
                throw new RecaptchaException("'host' name can not be null");
            }
            this.br = new Browser();
        } else {
            if (host == null) {
                final String brHost = new URL(br.getURL()).getHost();
                if (brHost == null) {
                    throw new RecaptchaException("'host' can not be determined!");
                } else {
                    this.host = brHost;
                }
            } else {
                this.host = host;
            }
            this.br = br.cloneBrowser();
            // nullify referer, its not required
            this.br.getHeaders().put("Referer", "");
        }
        this.siteKey = siteKey;
        // now other required components!
        initGoogleApiProperties();
    }

    // lets use static version and language
    private void initStaticRecaptchaScript() throws IOException, RecaptchaException {

        final String apiJs = br.getPage("http://www.google.com/recaptcha/api.js");
        final String recaptcha__de = new Regex(apiJs, "po.src = '(.*?)'").getMatch(0);
        if (recaptcha__de == null || !recaptcha__de.matches("https?\\://www.gstatic.com/recaptcha/api2/r\\d+/recaptcha__\\w+.js")) {
            throw new RecaptchaException("Could not find recaptcha_lng.js in http://www.google.com/recaptcha/api.js");
        }

        version = new Regex(recaptcha__de, "/(r\\d+)/").getMatch(0);
        language = new Regex(recaptcha__de, "__(\\w+)\\.js").getMatch(0);

    }

    public String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    public void initGoogleApiProperties() throws IOException, RecaptchaException {
        br.getPage("https://apis.google.com/js/api.js");

        jshString = unjsonify(br.getRegex("\"h\"\\:\"([^\"]+)").getMatch(0));

    }

    private String unjsonify(String match) {
        try {
            return org.appwork.storage.JSonStorage.restoreFromString("\"" + match + "\"", org.appwork.storage.TypeRef.STRING);
        } catch (Throwable e) {
            // jd09

            return decodeUnicode(match);
        }
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        if (true) {
            Browser br = new Browser();
            br.forceDebug(true);
            br.setVerbose(true);
            br.setLogger(org.jdownloader.logging.LogController.getInstance().getLogger("Recaptcha"));

            br.getPage("http://www.google.com/recaptcha/api2/demo");

            Recaptcha2Helper rc = new Recaptcha2Helper();
            rc.init(br);
            boolean success = false;
            while (!success) {
                BufferedImage image = rc.loadImage();
                String response = org.appwork.utils.swing.dialog.Dialog.getInstance().showInputDialog(0, "Recaptcha", "", null, new ImageIcon(image), null, null);
                success = rc.sendResponse(response);
                if (!success) {
                    org.appwork.utils.swing.dialog.Dialog.getInstance().showMessageDialog("Captcha wrong");
                }
            }
            String gRecaptchaResponseToken = rc.getResponseToken();
            Form form = br.getForms()[0];
            form.addInputField(new InputField("g-recaptcha-response", gRecaptchaResponseToken));

            br.submitForm(form);
            if (br.containsHTML("recaptcha-success")) {
                org.appwork.utils.swing.dialog.Dialog.getInstance().showMessageDialog("Send Form. recaptcha-success");
            } else {
                org.appwork.utils.swing.dialog.Dialog.getInstance().showMessageDialog("Send Form. Somthing went wrong. " + br);
            }

        }

    }

    public BufferedImage loadImage() throws IOException, RecaptchaException {

        final URLConnectionAdapter conn = br.openGetConnection(loadImageUrl());
        try {
            return ImageIO.read(conn.getInputStream());

        } finally {
            conn.disconnect();
        }

    }

    private String loadImageUrl() throws IOException, RecaptchaException {
        final String anchor = "https://www.google.com/recaptcha/api2/anchor";
        iframeId = "I" + 0 + "_" + (System.currentTimeMillis() / 1000);

        rpcToken = Math.round(1E8 * Double.parseDouble("0." + (int) (Math.random() * Integer.MAX_VALUE)));
        final String anchorUrl = anchor + "?k=" + siteKey + "&hl=" + language + "&v=" + version + "&usegapi=1&jsh=" + Encoding.urlEncode(jshString) + "#id=" + iframeId + "&parent=" + Encoding.urlEncode("http://" + host) + "&pfname=&rpctoken=" + rpcToken;

        br.getPage(anchorUrl);

        tokenForFrameLoading = br.getRegex("id=\"recaptcha-token\" value=\"([^\"]+)").getMatch(0);
        if (isEmpty(tokenForFrameLoading)) {
            throw new RecaptchaException("Could not find recaptcha-token");
        }

        // botGuard not supported
        botGuardString = "";

        final String rcFrameUrl = "https://www.google.com/recaptcha/api2/frame?c=" + tokenForFrameLoading + "&hl=" + language + "&v=" + version + "&bg=" + botGuardString + "&usegapi=1&jsh=" + Encoding.urlEncode(jshString);

        br.getPage(rcFrameUrl);

        tokenToReload = unjsonify(br.getRegex("\\[\\\\x22finput\\\\x22\\,\\s*\\\\x22([^\\\\]+)").getMatch(0));

        br.postPage("https://www.google.com/recaptcha/api2/reload", "c=" + tokenToReload + "&reason=fi");
        tokenForCaptchaChallengePayload = unjsonify(br.getRegex("\\[\"rresp\",\\s*\"([^\"]+)").getMatch(0));

        timeImageLoading = System.currentTimeMillis();
        return "https://www.google.com/recaptcha/api2/payload?c=" + tokenForCaptchaChallengePayload;
    }

    public String getResponseToken() {
        return responseToken;
    }

    public boolean isTimedout() {
        return System.currentTimeMillis() > verifyTime + timeout;
    }

    public boolean sendResponse(final String response) throws IOException {

        final String responseJson = "{\"response\":\"" + response + "\"}";

        final long timeToSolve = System.currentTimeMillis() - timeImageLoading;
        final long timeToSolveMore = timeToSolve + (long) (Math.random() * 500);
        br.postPage("https://www.google.com/recaptcha/api2/userverify", "c=" + tokenForCaptchaChallengePayload + "&response=" + Encoding.Base64Encode(responseJson) + "&t=" + timeToSolve + "&ct=" + timeToSolveMore + "&bg=" + botGuardString);
        String[] responseData = br.getRegex("\\[\"uvresp\"\\s*,\\s*\"([^\"]+)\"\\s*\\,\\s*(\\d*)\\s*\\,\\s*(\\d*)").getRow(0);
        responseToken = responseData[0];
        successIdentifier = responseData[1].length() > 0 ? Integer.parseInt(responseData[1]) : 0;
        // I'm not sure if this is the timeout. it may be the timeout in seconds until responseToken is valid
        verifyTime = System.currentTimeMillis();
        timeout = responseData[2].length() > 0 ? Integer.parseInt(responseData[2]) * 1000 : 0;
        success = successIdentifier > 0;

        return success;
    }

    // No FINAL. This would not be JD2 plugin sys compatible any more
    private static Object LOCK = new Object();

    public static void writeToFile(final File file, final byte[] data) throws IOException {

        if (file == null) {
            throw new IllegalArgumentException("File is null.");
        }
        if (file.exists()) {
            throw new IllegalArgumentException("File already exists: " + file);
        }
        file.createNewFile();
        if (!file.isFile()) {
            throw new IllegalArgumentException("Is not a file: " + file);
        }
        if (!file.canWrite()) {
            throw new IllegalArgumentException("Cannot write to file: " + file);
        }

        FileOutputStream out = null;

        try {
            out = new FileOutputStream(file);
            out.write(data);
            out.flush();

        } finally {
            try {
                out.close();
            } catch (final Throwable e) {
            }

        }

    }

    public File loadImageFile() throws IOException, RecaptchaException {
        final URLConnectionAdapter conn = br.openGetConnection(loadImageUrl());
        byte[] bytes = null;
        try {
            bytes = IO.readStream(-1, conn.getInputStream());
        } finally {
            conn.disconnect();
        }
        // synchronize to avoid existing file problems
        synchronized (LOCK) {

            int i = 0;
            File file = null;
            do {
                file = JDUtilities.getResourceFile("recaptcha2/img_" + i + ".jpg", true);
                i++;
            } while (file.exists());
            file.deleteOnExit();
            try {
                org.appwork.utils.IO.writeToFile(file, bytes);
            } catch (IOException e) {
                throw e;
            } catch (Throwable e) {
                // ((JD09))
                writeToFile(file, bytes);
            }
            return file;
        }

    }

}
