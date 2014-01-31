//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "embedupload.com" }, urls = { "http://(www\\.)?embedupload\\.(com|to)/\\?([A-Z0-9]{2}|d)=[A-Z0-9]+" }, flags = { 0 })
public class EmbedUploadCom extends PluginForDecrypt {

    public EmbedUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String        CURRENT_DOMAIN = "embedupload.to";
    private String        recaptcha      = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";
    private String        fuid           = null;
    private static Object LOCK           = new Object();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // one thread, will minimise captcha events.
        synchronized (LOCK) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            String parameter = param.toString().replace("embedupload.com/", "embedupload.to/");
            fuid = new Regex(parameter, "(com|to)/\\?([A-Z0-9]{2}|d)=([A-Z0-9]+)").getMatch(2);
            br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.76 Safari/537.36");
            br.setFollowRedirects(false);
            br.getPage(parameter);
            if (br.containsHTML("Copyright Abuse <br>") || br.containsHTML("Invalid file name <") || br.getURL().equals("http://" + CURRENT_DOMAIN + "/?d=")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML(recaptcha)) {
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
                final String id = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                rc.setId(id);
                if (id == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                rc.load();
                for (int i = 0; i <= 3; i++) {
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode(cf, param);
                    br.postPage(br.getURL(), "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                    if (br.containsHTML(recaptcha)) {
                        rc.reload();
                        continue;
                    }
                    break;
                }
                if (br.containsHTML(recaptcha)) throw new DecrypterException(DecrypterException.CAPTCHA);
            }

            if (parameter.contains(CURRENT_DOMAIN + "/?d=")) {
                String embedUploadDirectlink = br.getRegex("div id=\"embedupload\" style=\"padding-left:43px;padding-right:20px;padding-bottom:20px;font-size:17px;font-style:italic\" >[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
                if (embedUploadDirectlink == null) embedUploadDirectlink = br.getRegex("(\"|')(http://(www\\.)?embedupload\\.(com|to)/\\?EU=[A-Z0-9]+&urlkey=[A-Za-z0-9]+)(\"|')").getMatch(1);
                if (embedUploadDirectlink != null) {
                    decryptedLinks.add(createDownloadlink("directhttp://" + embedUploadDirectlink));
                }
                String[] redirectLinks = br.getRegex("style=\"padding-left:43px;padding-right:20px;padding-bottom:20px;font-size:17px;font-style:italic\" >[\t\r\n ]+<a href=\"(http://.*?)\"").getColumn(0);
                if (redirectLinks == null || redirectLinks.length == 0) redirectLinks = br.getRegex("(\"|')(http://(www\\.)?embedupload\\.(com|to)/\\?[A-Z0-9]{2}=" + fuid + ")(\"|')").getColumn(1);
                if (redirectLinks == null || redirectLinks.length == 0) {
                    if (br.containsHTML("You can download from these site : ") || br.containsHTML(">The file you are looking for is hosted on these")) {
                        logger.info("Link might be offline: " + parameter);
                        return decryptedLinks;
                    }
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (String singleLink : redirectLinks) {
                    if (!singleLink.contains("&urlkey=")) {
                        Browser br2 = br.cloneBrowser();
                        br2.getPage(singleLink);
                        if (br2.getRedirectLocation() != null) {
                            // redirects
                            decryptedLinks.add(createDownloadlink(br2.getRedirectLocation()));
                        } else if (br2.getHttpConnection().getContentType().contains("html")) {
                            // some links are not provided by redirect.
                            final String link = getSingleLink(br2);
                            if (link != null) {
                                decryptedLinks.add(createDownloadlink(link));
                            }
                        } else {
                            // spew out something here
                            logger.warning("EmbededUpload Decrypter can't find links: " + parameter);
                            return null;
                        }
                    }
                }
            } else {
                // redirects within the non ?d= links
                final String finallink = getSingleLink(br);
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            }
            return decryptedLinks;
        }
    }

    private String getSingleLink(Browser ibr) {
        String link = ibr.getRegex("link on a new browser window : ([^<>\"]*?)</b>").getMatch(0);
        if (link == null) link = ibr.getRegex("You should click on the download link : <a href=\\'(http[^<>\"]*?)\\'").getMatch(0);
        if (link == null) link = ibr.getRegex("File hosting link:[\t\n\r ]+<b>[\t\n\r ]+<a href=\\'(http[^<>\"]*?)\\'").getMatch(0);
        return link;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}