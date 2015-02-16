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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

import org.jdownloader.captcha.utils.recaptcha.api2.Recaptcha2Helper;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "adultddl.ws" }, urls = { "http://(www\\.)?adultddl\\.(com|ws)/\\d{4}/\\d{2}/\\d{2}/[^<>\"'/]+" }, flags = { 0 })
public class AdltDlCom extends PluginForDecrypt {

    public AdltDlCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser br2 = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("adultddl.com/", "adultddl.ws/");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 Not Found<|Sorry, the page could not be found") || br.containsHTML("<title> \\| AdultDDL</title>") || parameter.contains("adultddl.ws/1970/")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("<h2[^>]+entry-title[^>]+>(.*?)</h2>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>([^<>\"']+) (?:\\||-) AdultDDL</title>").getMatch(0);
        }
        // these are still shown, keep until we remove firedirve/putlocker.
        final String streamLink = br.getRegex("'(http://(www\\.)?(putlocker|firedrive)\\.com/embed/[A-Z0-9]+)'").getMatch(0);
        if (streamLink != null) {
            decryptedLinks.add(createDownloadlink(streamLink));
        }
        final String linksText = br.getRegex("<div class='links'>(.*?)(?:<div id=\"comment-section\"|<!--//entry-content)").getMatch(0);
        if (linksText == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[] decryptPage = new Regex(linksText, "<iframe src='(http://secure\\.adultddl\\.(?:ws|com)/\\?decrypt=\\d+)'").getColumn(0);
        if (decryptPage == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String dp : decryptPage) {
            br2 = br.cloneBrowser();
            br2.getPage(dp);
            handleCaptcha(param);
            addLinks(decryptedLinks, parameter);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void addLinks(final ArrayList<DownloadLink> decryptedLinks, final String parameter) throws PluginException {
        final String[] links = HTMLParser.getHttpLinks(br2.toString(), "");
        if ((links == null || links.length == 0) && decryptedLinks.isEmpty()) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (String singleLink : links) {
            if (singleLink.matches(".+://img\\.adultddl\\.(?:ws|com)/.+") || !singleLink.matches(".+://.*?adultddl\\.(?:ws|com)/.+")) {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
    }

    private void handleCaptcha(final CryptedLink param) throws Exception {
        final int retry = 3;
        Form captcha = br2.getForm(0);
        if (captcha != null && captcha.containsHTML("value='Click Here'> to continue...")) {
            // base link requires another submit
            br2.submitForm(captcha);
            captcha = br2.getForm(0);
        }
        for (int i = 1; i < retry; i++) {
            if (captcha == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (br2.containsHTML("class=\"g-recaptcha\"") && br2.containsHTML("google\\.com/recaptcha")) {
                // recaptcha v2
                int counter = 0;
                boolean success = false;
                String responseToken = null;
                do {
                    Recaptcha2Helper rchelp = new Recaptcha2Helper();
                    rchelp.init(this.br2);
                    final File outputFile = rchelp.loadImageFile();
                    String code = getCaptchaCode("recaptcha", outputFile, param);
                    success = rchelp.sendResponse(code);
                    responseToken = rchelp.getResponseToken();
                    counter++;
                } while (!success && counter <= retry);
                if (!success) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                captcha.put("g-recaptcha-response", Encoding.urlEncode(responseToken));
            } else if (br2.containsHTML("google\\.com/recaptcha")) {
                // recaptcha v1
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br2);
                rc.findID();
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, param);
                captcha.put("recaptcha_challenge_field", rc.getChallenge());
                captcha.put("recaptcha_response_field", Encoding.urlEncode(c));
            } else {
                final String cLnk = br2.getRegex("\\'(/securimage/securimage_show\\.php\\?\\d+)\\'").getMatch(0);
                if (cLnk == null) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                final String c = getCaptchaCode("http://secure.adultddl.ws" + cLnk, param);
                captcha.put("captcha_code", Encoding.urlEncode(c));
            }
            br2.submitForm(captcha);
            if (br2.containsHTML("The CAPTCHA entered was incorrect|Please go back and try again")) {
                if (i + 1 > retry) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                } else {
                    captcha = br2.getForm(0);
                    continue;
                }
            }
            break;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}