//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "extreme-protect.net" }, urls = { "http://(?:www\\.)?extreme\\-protect\\.net/([a-z0-9\\-_]+)" }, flags = { 0 })
public class ExtremeProtectNet extends PluginForDecrypt {

    public ExtremeProtectNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String RECAPTCHATEXT  = "api\\.recaptcha\\.net";
    private static final String RECAPTCHATEXT2 = "google\\.com/recaptcha/api/challenge";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String fpName = null;
        String[] links = null;
        {
            // this bypasses captcha etc. problem is title isn't shown...
            br.postPage("http://extreme-protect.net/api.php", "id=" + new Regex(parameter, this.getSupportedLinks()).getMatch(0));
            fpName = getJson("titre");
            String lnks = getJsonArray("liens");
            if (lnks != null) {
                links = new Regex(lnks, "http[^\"]+").getColumn(-1);
                if (links != null) {
                    for (String dl : links) {
                        decryptedLinks.add(createDownloadlink(dl));
                    }
                }
            }
        }
        if (inValidate(fpName)) {
            br = new Browser();
            br.setFollowRedirects(true);
            try {
                br.setAllowedResponseCodes(new int[] { 500 });
            } catch (final Throwable e) {
                // logger.info("Link can only be decrypted in JDownloader 2: " + parameter);
                // return decryptedLinks;
            }
            br.getPage(parameter);
            if (inValidate(fpName)) {
                fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            }
            if (decryptedLinks.isEmpty()) {
                // fail over
                if (br.getHttpConnection().getResponseCode() == 403 || !br.containsHTML("class=\"contenu_liens\"")) {
                    logger.info("Link offline: " + parameter);
                    final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                    offline.setAvailable(false);
                    offline.setProperty("offline", true);
                    decryptedLinks.add(offline);
                    return decryptedLinks;
                }
                if (br.containsHTML(RECAPTCHATEXT) || br.containsHTML(RECAPTCHATEXT2)) {
                    boolean failed = true;
                    for (int i = 0; i <= 5; i++) {
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        rc.parse();
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String c = getCaptchaCode("recaptcha", cf, param);
                        br.postPage(br.getURL(), "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c + "&submit_captcha=VALIDER");
                        if (br.containsHTML(RECAPTCHATEXT) || br.containsHTML(RECAPTCHATEXT2)) {
                            br.getPage(parameter);
                            continue;
                        }
                        failed = false;
                        break;
                    }
                    if (failed) {
                        throw new DecrypterException(DecrypterException.CAPTCHA);
                    }
                } else {
                    final String pass = generatePass();

                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                    br.getHeaders().put("Cache-Control", null);
                    br.postPage("/requis/captcha_formulaire.php", "action=qaptcha&qaptcha_key=" + pass);

                    br.getHeaders().put("Accept", "text/html, application/xhtml+xml, */*");
                    br.getHeaders().put("X-Requested-With", null);
                    br.postPage(parameter, pass + "=&submit_captcha=VALIDER");
                }

                links = br.getRegex("class=\"lien\" ><a target=\"_blank\" href=\"(http[^<>\"]*?)\"").getColumn(0);
                if (links == null || links.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (String dl : links) {
                    decryptedLinks.add(createDownloadlink(dl));
                }
                if (inValidate(fpName)) {
                    fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
                }
            }
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String generatePass() {
        int nb = 32;
        final String chars = "azertyupqsdfghjkmwxcvbn23456789AZERTYUPQSDFGHJKMWXCVBN_-#@";
        String pass = "";
        for (int i = 0; i < nb; i++) {
            long wpos = Math.round(Math.random() * (chars.length() - 1));
            int lool = (int) wpos;
            pass += chars.substring(lool, lool + 1);
        }
        return pass;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(ibr.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonResultsFromArray(source);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}