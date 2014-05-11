//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mov-world.net" }, urls = { "http://(www\\.)?mov\\-world\\.net/(\\?id=\\d+|.*?/.*?\\d+\\.html|[a-z]{2}-[a-zA-Z0-9]+/)" }, flags = { 0 })
public class MvWrldNt extends PluginForDecrypt {

    // DEV NOTES
    // variants of X4fChWa

    public MvWrldNt(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            logger.info("Link offline (server error): " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("<h1>Dieses Release ist nur noch bei <a")) {
            logger.info("Link offline (offline): " + parameter);
            return decryptedLinks;
        } else if (br.getURL().contains("mov-world.net/error.html?error=404") || br.containsHTML(">404 Not Found<")) {
            logger.info("Link offline (error 404): " + parameter);
            return decryptedLinks;
        } else if (br.getURL().equals("http://mov-world.net/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        final String MAINPAGE = "http://" + br.getHost();
        final String password = br.getRegex("class=\"password\">Password: (.*?)</p>").getMatch(0);
        ArrayList<String> pwList = null;
        final Form captchaForm = br.getFormbyProperty("id", "mirrorlist-form");
        Browser brc = br.cloneBrowser();
        // captcha can be null, after you solve once... re-testing code doesn't invoke captcha again!
        if (captchaForm != null) {
            String captchaUrl = captchaForm.getRegex("\"(/captcha/[^\"]+\\.gif)\"").getMatch(0);
            captchaUrl = MAINPAGE + captchaUrl;
            final File captchaFile = getLocalCaptchaFile();
            brc.getDownload(captchaFile, captchaUrl);
            String code = null;
            for (int i = 0; i <= 5; i++) {
                if (i > 0) {
                    // Recognition failed, ask the user!
                    code = getCaptchaCode(null, captchaFile, param);
                } else {
                    code = getCaptchaCode("mov-world.net", captchaFile, param);
                }
                captchaForm.put("security-code", code);
                br.submitForm(captchaForm);
                if (br.getFormbyProperty("id", "mirrorlist-form") != null && i + 1 == 5)
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                else if (br.getFormbyProperty("id", "mirrorlist-form") != null)
                    continue;
                else
                    break;
            }
        }
        final String[] links = br.getRegex("<td class=\"link\"><p><a href=\"http://anonym.to/\\?([^\"]+)").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("mov-world.net: The content of this link is completly offline");
            logger.warning("mov-world.net: Please confirm via browser, and report any bugs to developement team. :" + parameter);
            return decryptedLinks;
        }
        boolean toManyLinks = false;
        if (links.length > 100) {
            toManyLinks = true;
        }
        progress.setRange(links.length);
        for (String dl : links) {
            if (!dl.startsWith("http")) {
                dl = MAINPAGE + dl;
            }
            // new formats are urlencoded
            dl = Encoding.urlDecode(dl, false);
            brc = br.cloneBrowser();
            brc.setFollowRedirects(false);
            if (dl.startsWith(MAINPAGE)) {
                brc.getPage(dl);
                if (brc.getRequest().getHttpConnection().getResponseCode() == 302) {
                    dl = brc.getRedirectLocation();
                } else {
                    dl = brc.getRegex("iframe src=\"(.*?)\"").getMatch(0);
                }
            }
            progress.increase(1);
            if (dl == null) {
                continue;
            }
            final DownloadLink downLink = createDownloadlink(dl);
            if (toManyLinks) {
                downLink.setAvailable(true);
            }
            pwList = new ArrayList<String>();
            final String host = Browser.getHost(parameter);
            if (password != null && !password.equals("")) {
                pwList.add(password.trim());
            }
            if (host != null) {
                pwList.add(host.trim());
            }
            downLink.setSourcePluginPasswordList(pwList);
            try {
                distribute(downLink);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
            decryptedLinks.add(downLink);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}