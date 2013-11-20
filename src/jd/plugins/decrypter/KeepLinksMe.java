//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keeplinks.me" }, urls = { "https?://(www\\.)?keeplinks\\.me/(p|d)/[a-z0-9]+" }, flags = { 0 })
public class KeepLinksMe extends SaveLinksNet {

    public KeepLinksMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String HOST  = "keeplinks.me";
    private String cType = "notDetected";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final GeneralSafelinkingHandling gsh = new GeneralSafelinkingHandling(br, param, getHost());
        final String parameter = param.toString();
        gsh.startUp();
        try {
            br.getPage(parameter);
            handleErrors(parameter);
            /* unprotected links */
            if (parameter.contains("/d/")) {
                gsh.decryptSingleLink();
                decryptedLinks = gsh.getDecryptedLinks();
            } else {
                handleCaptcha(parameter, param);
                String[] links = br.getRegex("class=\"selecttext live\">(http[^<>\"]*?)</a>").getColumn(0);
                if (links == null || links.length == 0) links = br.getRegex("\"(http://(www\\.)?keeplinks\\.me/d/[a-z0-9]+)\" id=\"direct\\d+\"").getColumn(0);
                if (links == null || links.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return decryptedLinks;
                }
                for (final String aLink : links) {
                    decryptedLinks.add(createDownloadlink(aLink));
                }
            }
        } catch (final DecrypterException e) {
            final String errormessage = e.getMessage();
            if ("offline".equals(errormessage)) { return decryptedLinks; }
            throw e;
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    private void handleCaptcha(final String parameter, final CryptedLink param) throws Exception {
        br.setFollowRedirects(true);
        /* protected links */
        String protocol = parameter.startsWith("https:") ? "https:" : "http:";
        HashMap<String, String> captchaRegex = new HashMap<String, String>();
        captchaRegex.put("recaptcha", "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)");
        captchaRegex.put("basic", "(https?://" + HOST + "/includes/captcha_factory/securimage/securimage_(show\\.php\\?hash=[a-z0-9]+|register\\.php\\?hash=[^\"]+sid=[a-z0-9]{32}))");
        captchaRegex.put("threeD", "\"(https?://" + HOST + "/includes/captcha_factory/3dcaptcha/3DCaptcha\\.php)\"");
        captchaRegex.put("fancy", "name=\"captchatype\" id=\"captchatype\" value=\"Fancy\"");
        captchaRegex.put("qaptcha", "class=\"protected\\-captcha\"><div id=\"QapTcha\"");
        captchaRegex.put("solvemedia", "api(\\-secure)?\\.solvemedia\\.com/(papi)?");
        captchaRegex.put("simplecaptcha", "\"http://(www\\.)?keeplinks\\.me/simplecaptcha/captcha\\.php\"");

        final String id = br.getRegex("name=\"id\" id=\"id\" value=\"(\\d+)\"").getMatch(0);
        if (id == null) return;

        /* search for protected form */
        final Form protectedForm = br.getFormbyProperty("id", "frmprotect");
        if (protectedForm != null) {
            boolean password = protectedForm.getRegex("type=\"password\" name=\"link-password\"").matches(); // password?
            String captcha = br.getRegex("<form name=\"frmprotect\" id=\"frmprotect\"(.*?)</form>").getMatch(0); // captcha?
            if (captcha != null) prepareCaptchaAdress(captcha, captchaRegex, protocol);

            for (int i = 0; i <= 5; i++) {
                String data = "myhiddenpwd=&hiddenaction=CheckData&hiddencaptcha=1&hiddenpwd=&id=" + id;
                if (password) data += "&link-password=" + getUserInput(null, param);

                Browser captchaBr = null;
                if (!"notDetected".equals(cType)) captchaBr = br.cloneBrowser();

                switch (getCaptchaTypeNumber()) {
                case 1:
                    PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                    jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((jd.plugins.decrypter.LnkCrptWs) solveplug).getSolveMedia(br);
                    File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    String code = getCaptchaCode(cf, param);
                    String chid = sm.getChallenge(code);
                    data += "&solvemedia_response=" + code.replace(" ", "+") + "&adcopy_challenge=" + chid + "&adcopy_response=" + code.replace(" ", "+");
                    break;
                case 2:
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
                    final String rcID = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                    if (id == null) return;
                    rc.setId(rcID);
                    rc.load();
                    rc.load();
                    File cfRe = rc.downloadCaptcha(getLocalCaptchaFile());
                    data += "&captchatype=Re&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + getCaptchaCode(cfRe, param);
                    break;
                case 3:
                    data += "&securimage_response_field=" + getCaptchaCode(br.getRegex(captchaRegex.get("basic")).getMatch(0), param);
                    break;
                case 4:
                    data += "&3dcaptcha_response_field=" + getCaptchaCode(br.getRegex(captchaRegex.get("threeD")).getMatch(0), param);
                    break;
                case 5:
                    captchaBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    captchaBr.getPage("http://www." + HOST + "/fancycaptcha/captcha/captcha.php");
                    data += "&captchatype=Fancy&captcha=" + captchaBr.toString().trim();
                    break;
                case 6:
                    captchaBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    captchaBr.postPage("https://" + HOST + "/includes/captcha_factory/Qaptcha.jquery.php?hash=" + new Regex(parameter, "/p/(.+)").getMatch(0), "action=qaptcha");
                    if (!captchaBr.containsHTML("\"error\":false")) {
                        logger.warning("Decrypter broken for link: " + parameter + "\n");
                        logger.warning("Qaptcha handling broken");
                        if (password) { throw new DecrypterException("Decrypter for " + HOST + " is broken"); }
                    }
                    data += "&iQapTcha=";
                    break;
                case 7:
                    break;
                case 8:
                    break;
                case 9:
                    break;
                case 10:
                    break;
                case 11:
                    break;
                case 12:
                    final String result = getCaptchaCode("http://www.keeplinks.me/simplecaptcha/captcha.php", param);
                    data += "captchatype=Simple&norobot=" + result;
                    break;
                }

                if (captchaRegex.containsKey(cType) || data.contains("link-password")) {
                    br.postPage(parameter, data);
                    if (br.getHttpConnection().getResponseCode() == 500) {
                        logger.warning(HOST + ": 500 Internal Server Error. Link: " + parameter);
                        continue;
                    }
                    password = br.getRegex("type=\"password\" name=\"link-password\"").matches(); // password
                                                                                                  // correct?
                }

                if (!"notDetected".equals(cType) && br.containsHTML(captchaRegex.get(cType)) || (!br.containsHTML("class=\"co_form_title\">Live Link") && !br.containsHTML("class=\"co_form_title\">Direct Link")) || password || br.containsHTML("<strong>Prove you are human</strong>")) {
                    br.getPage(parameter);
                    prepareCaptchaAdress(captcha, captchaRegex, protocol);
                    continue;
                }
                break;
            }
            if (!"notDetected".equals(cType) && (br.containsHTML(captchaRegex.get(cType)) || br.containsHTML("<strong>Prove you are human</strong>"))) { throw new DecrypterException(DecrypterException.CAPTCHA); }
            if (password) { throw new DecrypterException(DecrypterException.PASSWORD); }
        }
    }

    public int getCaptchaTypeNumber() {
        if (cType.equals("solvemedia")) {
            return 1;
        } else if (cType.equals("recaptcha")) {
            return 2;
        } else if (cType.equals("basic")) {
            return 3;
        } else if (cType.equals("threeD")) {
            return 4;
        } else if (cType.equals("fancy")) {
            return 5;
        } else if (cType.equals("qaptcha")) {
            return 6;
        } else if (cType.equals("simple")) {
            return 7;
        } else if (cType.equals("dotty")) {
            return 8;
        } else if (cType.equals("cool")) {
            return 9;
        } else if (cType.equals("standard")) {
            return 10;
        } else if (cType.equals("cats")) {
            return 11;
        } else if (cType.equals("simplecaptcha")) { return 12; }
        // Not detected or other case
        return 0;
    }

    private void prepareCaptchaAdress(String captcha, HashMap<String, String> captchaRegex, String protocol) {
        br.getRequest().setHtmlCode(captcha);

        for (Entry<String, String> next : captchaRegex.entrySet()) {
            if (br.containsHTML(next.getValue())) {
                cType = next.getKey();
                break;
            }
        }

        logger.info("notDetected".equals(cType) ? "Captcha not detected." : "Detected captcha type \"" + cType + "\" for this " + HOST + ".");

        /* detect javascript */
        String javaScript = null;
        for (String js : br.getRegex("<script type=\"text/javascript\">(.*?)</script>").getColumn(0)) {
            if (!new Regex(js, captchaRegex.get(cType)).matches()) continue;
            javaScript = js;
        }
        if (javaScript == null) return;

        /* execute javascript */
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            /*
             * creating pseudo functions: document.location.protocol + document.write(value)
             */
            engine.eval("var document = { loc : function() { var newObj = new Object(); function protocol() { return \"" + protocol + "\"; } newObj.protocol = protocol(); return newObj; }, write : function(a) { return a; }}");
            engine.eval("document.location = document.loc();");
            result = engine.eval(javaScript);
        } catch (Throwable e) {
            return;
        }
        String res = result != null ? result.toString() : null;
        br.getRequest().setHtmlCode(res);
    }

    private void handleErrors(final String parameter) throws DecrypterException {
        if (br.containsHTML(">404 Page/File not found<|>The page/file you\\'ve requested has been moved")) {
            logger.info("Link offline: " + parameter);
            throw new DecrypterException("offline");
        }
    }
}