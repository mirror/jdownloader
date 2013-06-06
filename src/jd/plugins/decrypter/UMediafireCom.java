//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkhalt.com" }, urls = { "http://(www\\.)?linkhalt\\.com/(\\?d=[A-Za-z0-9]+|index\\.php\\?p=[\\da-zA-Z]+)" }, flags = { 0 })
public class UMediafireCom extends PluginForDecrypt {

    public UMediafireCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);

        if (parameter.contains("/index.php?p=")) {
            if (br.containsHTML("Requested page not found\\.")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String[] links = br.getRegex("(https?://[^<>]+)</a></div>").getColumn(0);
            if (links != null && links.length > 0) {
                for (String link : links) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
        } else {
            // the following string is always present in html, you need js
            // confirmation
            // if
            // (br.containsHTML(">Sorry, file is not exist or has been deleted"))
            // {
            // logger.info("Link offline: " + parameter);
            // return decryptedLinks;
            // }
            if (br.containsHTML(">Password protected<")) {
                logger.info("Password protected links aren't supported yet.\nTo support them we need example links with their corresponding valid passwords\n Link which failed: " + parameter);
                return decryptedLinks;
            }
            final String iFrame = br.getRegex("\"(index\\.php\\?(page=get_hotlink&r=\\d+|get=\\d+))\"").getMatch(0);
            if (iFrame == null) {
                if (br.getRedirectLocation() != null) {
                    String link = br.getRedirectLocation();
                    br.getPage(link);
                    decryptedLinks.add(createDownloadlink(link));
                    return decryptedLinks;
                }
                logger.info("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage("http://linkhalt.com/" + iFrame);
            // Values are within the above page grab, and definitions of what
            // values mean are
            // http://linkhalt.com/styles/default/javascript/download.js, just
            // needs JS interp, as it could change!

            if (br.containsHTML("google\\.com/recaptcha")) {
                if (br.containsHTML("google\\.com/recaptcha")) {
                    for (int i = 0; i <= 5; i++) {
                        final String captchaLink = br.getRegex("var captcha = \"(https?://[^<>\"]*?)\";").getMatch(0);
                        final String challenge = br.getRegex("var captchaInput = \"([^<>\"]*?)\"").getMatch(0);
                        if (captchaLink == null || challenge == null) {
                            logger.info("Decrypter broken for link: " + parameter);
                            return null;
                        }
                        final String code = getCaptchaCode(captchaLink, param);
                        br.postPage("http://umediafire.com/index.php", "type=captcha&proxy=0&get=" + new Regex(iFrame, "(\\d+)$").getMatch(0) + "&recaptcha_challenge_field=" + challenge + "&captcha=" + code);
                        logger.warning(br.toString());
                        if (br.containsHTML("google\\.com/recaptcha")) continue;
                        break;
                    }
                    if (br.containsHTML("google\\.com/recaptcha")) throw new Exception(DecrypterException.CAPTCHA);
                }
            }
            // old
            final String fastWay = br.getRegex("var check = (\"|')http://\\d+\\.\\d+\\.\\d+\\.\\d+/[a-z0-9]+/([a-z0-9]+)").getMatch(1);
            if (fastWay != null) {
                decryptedLinks.add(createDownloadlink("http://mediafire.com/?" + fastWay));
            } else {
                final String finallink = br.getRegex("var link = (\"|')(https?://[^<>\"]*?)(\"|');").getMatch(1);
                final String type = br.getRegex("var type = (\\d+)").getMatch(0);
                final String ori = br.getRegex("var ori = (\"|')([0-9a-z]+)").getMatch(1);
                if (finallink != null) {
                    decryptedLinks.add(createDownloadlink(finallink));
                } else if (finallink == null && ori != null) {
                    decryptedLinks.add(createDownloadlink("http://mediafire.com/?" + ori));
                } else if (finallink == null || ori == null || type == null) {
                    logger.info("Possible plugin error: " + parameter);
                    return null;
                }
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}