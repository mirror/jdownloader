package org.jdownloader.captcha.v2.challenge.adscaptcha;

import java.net.URL;

import javax.swing.SwingUtilities;

import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/**
 * primary domain 'adscaptcha.com' that run this captcha service is now dead, but still can be located at minteye.com (but captcha seems to
 * fail also as subdomain 'api.minteye.com' has no dns record.).
 *
 * @author .bismarck
 * @author coalado
 * @author raztoki
 *
 *
 */
public class AdsCaptcha {
    private final Browser br;
    public Browser        acBr;
    private String        challenge;
    private String        publicKey;
    private String        captchaAddress;
    private String        captchaId;
    private String        result;
    private int           count = -1;

    public AdsCaptcha(final Browser br) {
        this.br = br;
    }

    public Form getResult() throws Exception {
        try {
            load();
        } catch (final Throwable e) {
            e.printStackTrace();
            throw new PluginException(LinkStatus.ERROR_FATAL, e.getMessage());
        } finally {
            try {
                acBr.getHttpConnection().disconnect();
            } catch (final Throwable e) {
            }
        }
        final Form ret = new Form();
        if (result == null) {
            return null;
        }
        ret.put("aera", result);
        ret.put("adscaptcha_challenge_field", challenge);
        ret.put("adscaptcha_response_field", result);
        return ret;
    }

    private void load() throws Exception {
        acBr = br.cloneBrowser();
        if (!checkIfSupported()) {
            throw new Exception("AdsCaptcha: Captcha type not supported!");
        }
        acBr.getPage(captchaAddress);
        getChallenge();
        getPublicKey();
        getImageCount();
        if (challenge == null || publicKey == null) {
            throw new Exception("AdsCaptcha: challenge and/or publickey equal null!");
        }

        final URL[] images = imageUrls();
        if (count <= 0 && images.length == 1) {
            throw new Exception("AdsCaptcha modul broken!");
        }
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                SliderCaptchaDialog sc = new SliderCaptchaDialog(0, "AdsCaptcha - " + br.getHost(), images, count);
                sc.displayDialog();
                result = sc.getReturnValue();
            }
        });

    }

    private void getChallenge() {
        challenge = acBr.getRegex("\"challenge\":\"([0-9a-f\\-]+)\"").getMatch(0);
    }

    private void getPublicKey() {
        publicKey = acBr.getRegex("\"publicKey\":\"([0-9a-f\\-]+)\"").getMatch(0);
        if (publicKey == null) {
            publicKey = new Regex(captchaAddress, "PublicKey=([0-9a-f\\-]+)&").getMatch(0);
        }
    }

    private void getImageCount() {
        String c = acBr.getRegex("\"count\":\"?(\\d+)\"?").getMatch(0);
        if (c != null) {
            count = Integer.parseInt(c);
        }
    }

    private boolean checkIfSupported() throws Exception {
        captchaAddress = acBr.getRegex("src=\'(https?://api\\.adscaptcha\\.com/Get\\.aspx\\?CaptchaId=\\d+&PublicKey=[^\'<>]+)").getMatch(0);
        captchaId = new Regex(captchaAddress, "CaptchaId=(\\d+)\\&").getMatch(0);
        if (captchaAddress == null || captchaId == null) {
            throw new Exception("AdsCaptcha: Captcha address not found!");
        }
        if (!"3671".equals(captchaId)) {
            return false;
        }
        return true;
    }

    private URL[] imageUrls() throws Exception {
        acBr.getPage("http://api.minteye.com/Slider/SliderData.ashx?cid=" + challenge + "&CaptchaId=" + captchaId + "&PublicKey=" + publicKey + "&w=180&h=150");
        String urls[] = acBr.getRegex("\\{\'src\':\\s\'(https?://[^\']+)\'\\}").getColumn(0);
        if (urls == null || urls.length == 0) {
            urls = acBr.getRegex("\\{\'src\':\\s\'(//[^\']+)\'\\}").getColumn(0);
        }
        if (urls == null || urls.length == 0) {
            urls = acBr.getRegex("(\'|\")spriteUrl\\1:\\s*(\'|\")(.*?)\\2").getColumn(2);
        }
        if (urls == null || urls.length == 0) {
            throw new Exception("AdsCaptcha: Image urls not found!");
        }
        URL out[] = new URL[urls.length];
        int i = 0;
        for (String u : urls) {
            if (u.startsWith("//")) {
                u = "http:" + u;
            }
            out[i++] = new URL(u);
        }
        return out;
    }

    public String getChallengeId() {
        return challenge;
    }

    public String getCaptchaUrl() {
        return captchaAddress;
    }

    public String getResultValue() {
        return result;
    }

}