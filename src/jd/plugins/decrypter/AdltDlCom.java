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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "adultddl.ws" }, urls = { "http://(www\\.)?adultddl\\.(com|ws)/\\d{4}/\\d{2}/\\d{2}/[^<>\"\\'/]+" }, flags = { 0 })
public class AdltDlCom extends PluginForDecrypt {

    public AdltDlCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("adultddl.com/", "adultddl.ws/");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // Offline1
        if (br.containsHTML("404 Not Found<|Sorry, the page could not be found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // Offline2
        if (br.containsHTML("<title> \\| AdultDDL</title>")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // Offline3
        if (parameter.contains("adultddl.ws/1970/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex(" title=\"Comment on ([^<>\"\\']+)\"").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>([^<>\"\\']+) \\| AdultDDL</title>").getMatch(0);
        final String streamLink = br.getRegex("\\'(http://(www\\.)?putlocker\\.com/embed/[A-Z0-9]+)\\'").getMatch(0);
        if (streamLink != null) decryptedLinks.add(createDownloadlink(streamLink));
        final String linksText = br.getRegex("<div class=\\'links\\'>(.*?)<div id=\"comment\\-section\"").getMatch(0);
        if (linksText == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String decryptPage = new Regex(linksText, "<iframe src=\\'(http://secure\\.adultddl\\.(com|ws)/\\?decrypt=\\d+)\\'").getMatch(0);
        if (decryptPage != null) {
            br.getPage(decryptPage);
            final String[] cryptedLinks = br.getRegex("\\'(http://secure\\.adultddl\\.ws/\\?decrypt=\\d+)\\'").getColumn(0);
            if (cryptedLinks == null || cryptedLinks.length == 0) {
                if (!br.containsHTML("<form method='post' action='captcha\\.php'>")) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final String cryptID = new Regex(decryptPage, "(\\d+)$").getMatch(0);
                if (!handleCaptcha(param, cryptID)) throw new DecrypterException(DecrypterException.CAPTCHA);
                final String[] links = HTMLParser.getHttpLinks(br.toString(), "");
                if ((links == null || links.length == 0) && streamLink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (String singleLink : links) {
                    if (!singleLink.contains("adultddl.com/")) {
                        final DownloadLink dl = createDownloadlink(singleLink);
                        try {
                            distribute(dl);
                        } catch (final Throwable e) {
                            // Not available in old 0.9.581 Stable
                        }
                        decryptedLinks.add(dl);
                    }
                }
                return decryptedLinks;
            }
            for (final String cryptedLink : cryptedLinks) {
                final String cryptID = new Regex(cryptedLink, "(\\d+)$").getMatch(0);
                if (!handleCaptcha(param, cryptID)) throw new DecrypterException(DecrypterException.CAPTCHA);
                final String[] links = HTMLParser.getHttpLinks(br.toString(), "");
                if ((links == null || links.length == 0) && streamLink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (links != null && links.length != 0) {
                    for (String singleLink : links) {
                        if (!singleLink.contains("adultddl.com/")) {
                            final DownloadLink dl = createDownloadlink(singleLink);
                            try {
                                distribute(dl);
                            } catch (final Throwable e) {
                                // Not available in old 0.9.581 Stable
                            }
                            decryptedLinks.add(dl);
                        }
                    }
                }
            }
        } else {
            final String[] links = HTMLParser.getHttpLinks(linksText, "");
            if ((links == null || links.length == 0) && streamLink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (links != null && links.length != 0) {
                for (String singleLink : links)
                    if (!singleLink.contains("adultddl.com/")) {
                        final DownloadLink dl = createDownloadlink(singleLink);
                        try {
                            distribute(dl);
                        } catch (final Throwable e) {
                            // Not available in old 0.9.581 Stable
                        }
                        decryptedLinks.add(dl);
                    }
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private boolean handleCaptcha(final CryptedLink param, final String id) throws IOException, DecrypterException {
        boolean done = false;
        for (int i = 1; i <= 3; i++) {
            br.postPage("http://secure.adultddl.ws/captcha.php", "submit=Click+Here&decrypt=" + id);
            final String cLnk = br.getRegex("\\'(/securimage/securimage_show\\.php\\?\\d+)\\'").getMatch(0);
            if (cLnk == null) { return false; }
            final String c = getCaptchaCode("http://secure.adultddl.ws" + cLnk, param);
            br.postPage("http://secure.adultddl.ws/verify.php", "submit=Submit&captcha_code=" + Encoding.urlEncode(c) + "&decrypt=" + id);
            if (br.containsHTML("The CAPTCHA entered was incorrect")) {
                continue;
            }
            done = true;
            break;
        }
        return done;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}