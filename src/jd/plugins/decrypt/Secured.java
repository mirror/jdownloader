package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;
import jd.utils.JDUtilities;

/**
 * DecryptPlugin für secured.in Links
 * 
 */
public class Secured extends PluginForDecrypt {

    static private final Pattern PAT_SUPPORTED    = getSupportPattern("http://[*]secured.in/download[+]");

    static private final Pattern PAT_FILE_ID      = Pattern.compile("accessDownload\\([^']*'([^']*)");

    static private final Pattern PAT_CAPTCHA      = Pattern.compile("<img src=\"(captcha-[^\"]*)");

    // simplepattern. ° ist platzhalter
    static private final String  PAT_DOWNLOAD_CMD = "if (alreadyClicked == 0) {°alreadyClicked = 1;°document.getElementById('img-'+file_id).src = \"http://secured.in/images/file_loading.png\";°new Ajax.Request('ajax-handler.php',°method:'post',°parameters: {cmd: '°', download_id: dl_id},";

    static private final String  HOST             = "secured.in";

    static private final String  PLUGIN_NAME      = "secured.in";

    static private final String  PLUGIN_VERSION   = "0";

    static private final String  PLUGIN_ID        = PLUGIN_NAME + "-" + VERSION;

    static private final String  CODER            = "olimex(coalado angepasst)";

    static private final String  JS_URL           = "http://secured.in/scripts/main31.js";

    static private final String  AJAX_URL         = "http://secured.in/ajax-handler.php";

    static private String        DOWNLOAD_CMD     = "downloaditfgh4w5z4e5";

    private String               cryptedLink;

    public Secured() {
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public boolean isClipboardEnabled() {
        return true;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    /**
     * Eine Secured ID in eine URL übersetzen
     * 
     * @param id Secured ID
     * @return URL als String
     * @throws IOException
     * 
     * 
     * 
     * 
     * 
     */

    public String decryptId(String id) throws IOException {

        HashMap<String, String> request = new HashMap<String, String>();
        request.put("Accept", "text/javascript, text/html, application/xml, text/xml, */*");
        request.put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        request.put("Accept-Encoding", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        request.put("Keep-Alive", "300");
        request.put("Connection", "keep-alive");
        request.put("X-Requested-With", "XMLHttpRequest");
        request.put("X-Prototype-Version", "1.5.0");
        request.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        request.put("Cache-Control", "no-cache");
        request.put("Pragma", "no-cache");

        RequestInfo info = postRequest(new URL(AJAX_URL), null, cryptedLink, request, "cmd=" + DOWNLOAD_CMD + "&download_id=" + id, true);
        return info.getHtmlCode();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String cryptedLink = (String) parameter;
        switch (step.getStep()) {
            case PluginStep.STEP_DECRYPT:
                Vector<String> decryptedLinks = new Vector<String>();
                logger.finest("Decrypt: " + cryptedLink);
                this.cryptedLink = cryptedLink;

                try {
                    RequestInfo requestInfo = getRequest(new URL(JS_URL));
                    DOWNLOAD_CMD = getSimpleMatch(requestInfo.getHtmlCode(), PAT_DOWNLOAD_CMD, 0, 5);
                    
                    firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_MAX, 1));
                    URL url = new URL(cryptedLink);
                    requestInfo = getRequest(url);

                    String html = requestInfo.getHtmlCode();

                    for (;;) { // for() läuft bis kein Captcha mehr abgefragt
                        // wird
                        Matcher matcher = PAT_CAPTCHA.matcher(html);

                        if (matcher.find()) {
                            logger.finest("Captcha Protected");
                            String capHash = matcher.group(1).substring(8);
                            capHash = capHash.substring(0, capHash.length() - 4);
                            String captchaAdress = "http://" + HOST + "/" + matcher.group(1);
                            File dest = JDUtilities.getResourceFile("captchas/" + this.getPluginName() + "/captcha_" + (new Date().getTime()) + ".jpg");
                            JDUtilities.download(dest, captchaAdress);

                            String capTxt = Plugin.getCaptchaCode(dest, this);
                            String postData = "captcha_key=" + capTxt + "&captcha_hash=" + capHash;

                            requestInfo = postRequest(url, postData);
                            html = requestInfo.getHtmlCode();
                        }
                        else {
                            break;
                        }
                    }

                    // Alle File ID aus dem HTML-Code ziehen
                    Matcher matcher = PAT_FILE_ID.matcher(html);
                    Vector<String> ids = new Vector<String>();
                    while (matcher.find()) {                    
                        ids.add(matcher.group(1));
                    }
                    firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_MAX, ids.size()));
                    while (ids.size() > 0) {
                        String fileUrl = this.decryptId(ids.remove(0));
                       
                        decryptedLinks.add(fileUrl);
                        firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_INCREASE, null));

                    }
                    logger.finest("URL#: " + decryptedLinks.size());
                    firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, null));

                }
                catch (Exception e) {
                    logger.warning("Exception: " + e);
                }
                step.setParameter(decryptedLinks);
                break;

        }
        return null;
    }
}
