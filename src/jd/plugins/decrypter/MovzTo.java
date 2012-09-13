//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "moviez.to" }, urls = { "http://(www\\.)?moviez\\.to/movies/\\d+[a-z0-9\\-]+/releases/\\d+[a-z0-9\\-]+" }, flags = { 0 })
public class MovzTo extends PluginForDecrypt {

    public MovzTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:15.0) Gecko/20100101 Firefox/15.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us,en;q=0.5");
        String fpName = null;
        if (parameter.matches("http://(www\\.)?moviez\\.to/movies/\\d+[a-z0-9\\-]+/releases/\\d+[a-z0-9\\-]+")) {
            br.getPage(parameter);
            final String theCode = decodeCode(parameter);
            if (theCode == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }

            fpName = getFpName();
            final String[][] links = new Regex(theCode, "\"(http://(www\\.)?moviez\\.to/movies/[a-z0-9\\-]+/download\\?download_id=(\\d+)\\&security_token=([a-z0-9]+))\"").getMatches();
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            int failedLinksCount = 0;
            final String linkPart = new Regex(parameter, "(http://(www\\.)?moviez\\.to/movies/\\d+[a-z0-9\\-]+/)releases/\\d+[a-z0-9\\-]+").getMatch(0);
            for (final String[] info : links) {
                final String currentLink = info[0];
                final String movieID = info[2];
                final String secToken = info[3];
                try {
                    br.getPage(currentLink);
                } catch (final Exception e) {
                    logger.info("Skipping broken link: " + currentLink);
                    failedLinksCount++;
                    continue;
                }
                final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\\&amp;error=expression\"").getMatch(0);
                if (rcID == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }

                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setId(rcID);
                rc.load();
                boolean forceConfinue = false;
                for (int i = 0; i <= 5; i++) {
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode(cf, param);
                    try {
                        System.out.println(linkPart + Encoding.urlEncode("download?utf8=%E2%9C%93&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c + "&download_id=" + movieID + "&security_token=" + secToken + "&commit=Download"));
                        br.getPage(linkPart + Encoding.urlEncode("download?utf8=%E2%9C%93&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c + "&download_id=" + movieID + "&security_token=" + secToken + "&commit=Download"));
                    } catch (Exception e) {
                        rc.reload();
                        forceConfinue = true;
                        continue;
                    }
                    if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)") || br.containsHTML(">Error 403 Forbidden<")) {
                        rc.reload();
                        forceConfinue = false;
                        continue;
                    }
                    break;
                }
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)") || br.containsHTML(">Error 403 Forbidden<") || forceConfinue) {
                    logger.info("Captcha failed, continuing: " + currentLink);
                    continue;
                }
                final String finallink = br.getRedirectLocation();
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            }
            if (failedLinksCount == links.length) {
                logger.info("Site has server problems, all lins failed to decrypt for link: " + parameter);
                return decryptedLinks;
            }
            if (decryptedLinks.size() == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        } else {
            // Code not used at the moment
            String linkpart = new Regex(parameter, "moviez\\.to/ddl/#/popup/(.*?/\\d+/)").getMatch(0);
            if (linkpart == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            linkpart = linkpart.replace("spiele", "gamez").replace("filme", "moviez").replace("musik", "mp3").replace("programme", "appz").replace("erotik", "xxx");
            parameter = "http://www.moviez.to/nojs/" + linkpart;
            br.getPage(parameter);
            if (br.containsHTML("Sorry, f\\&uuml;r diese File haben wir leider keine Beschreibung")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            fpName = getFpName();
            String[] links = br.getRegex("<td class=\"row1\"><a href=\"(.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\"(http://linksave\\.in/[a-z0-9]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String dl : links) {
                if (dl.contains("FriendlyDuck.com/") || dl.contains("firstload.de/")) continue;
                decryptedLinks.add(createDownloadlink(dl));
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String getFpName() {
        String fpName = br.getRegex("class=\"kategorien1\" style=\"font-size: 12px; text-align:center;\">(.*?)</h1>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<a title=\"(.*?)\"").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("style=\"font-weight:800;\">Jetzt (.*?) <br />kostenlos aus dem Usenet downloaden\\.</td>").getMatch(0);
            }
        }
        return fpName;
    }

    private String decodeCode(final String parameter) {
        final String encodedJS = br.getRegex("eval\\(Decoder\\.decode\\(\"([^<>\"]*?)\"\\)\\);").getMatch(0);
        if (encodedJS == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[] b64parts = encodedJS.split("\\\\n");
        if (b64parts == null || b64parts.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String theCode = "";
        for (final String b64part : b64parts) {
            theCode += Encoding.Base64Decode(b64part);
        }
        return theCode.replace("\\", "");
    }
}
