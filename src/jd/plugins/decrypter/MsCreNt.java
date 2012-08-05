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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "musicore.net" }, urls = { "http(s)?://(www\\.)?(r\\.)?musicore\\.net/forums/index\\.php\\?(/topic/\\d+\\-|showtopic=\\d+|\\?id=[A-Za-z0-9]+\\&url=[a-zA-Z0-9=\\+/\\-]+|/forum/\\d+\\-[A-Za-z0-9]+/(page_.+)?)" }, flags = { 0 })
public class MsCreNt extends PluginForDecrypt {

    public MsCreNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = Encoding.htmlDecode(parameter);
        br.setCookiesExclusive(true);
        br.setFollowRedirects(false);
        if (new Regex(parameter, ".*?(/topic/|showtopic=)").matches()) {
            String topicID = new Regex(parameter, "(topic/|showtopic=)(\\d+)").getMatch(1);
            if (topicID == null) return null;
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.setFollowRedirects(true);
            br.getPage("http://musicore.net/ajax/release.php?id=" + topicID);
            if (br.containsHTML(">Release is being uploaded to")) {
                // Try getting links without "API"
                br.getPage(parameter);
                final String[] allLinks = HTMLParser.getHttpLinks(br.toString(), "");
                for (String singleLink : allLinks) {
                    singleLink = Encoding.htmlDecode(singleLink);
                    if (!singleLink.contains("musicore.net/")) decryptedLinks.add(createDownloadlink(singleLink));
                }
            } else {
                br.setFollowRedirects(false);
                String[] redirectlinks = br.getRegex("(\\'|\")(http://r\\.musicore\\.net/\\?id=.*?url=.*?)(\\'|\")").getColumn(1);
                if (redirectlinks == null || redirectlinks.length == 0) redirectlinks = br.getRegex("<a href=\"(/redirect/.*?)\"").getColumn(0);
                if (redirectlinks == null || redirectlinks.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                br.getPage("http://musicore.net/ajax/ftp.php?id=" + topicID);
                String[] ftpLinks = br.getRegex("\"(ftp://\\d+:[a-z0-9]+@ftp\\.musicore\\.ru/.*?)\"").getColumn(0);
                for (String redirectlink : redirectlinks) {
                    redirectlink = Encoding.htmlDecode(redirectlink);
                    if (!redirectlink.contains("musicore.net")) redirectlink = "https://musicore.net" + redirectlink;
                    br.getPage(redirectlink);
                    String finallink = br.getRedirectLocation();
                    if (finallink == null) finallink = br.getRegex("URL=(.*?)\"").getMatch(0);
                    if (finallink == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        finallink = br.getRegex("URL=(.*?)\"").getMatch(0);
                    }
                    decryptedLinks.add(createDownloadlink(finallink));
                }
                if (ftpLinks != null && ftpLinks.length != 0) {
                    for (String ftpLink : ftpLinks) {
                        decryptedLinks.add(createDownloadlink(ftpLink));
                    }
                }
            }
        } else if (parameter.contains("musicore.net/forums/index.php?/forum/")) {
            br.getPage(parameter);
            String[] allLinks = br.getRegex("id=\"tid\\-link\\-\\d+\" href=\"(http.*?)\"").getColumn(0);
            if (allLinks == null || allLinks.length == 0) allLinks = br.getRegex("<ul class=\\'last_post\\'>[\t\n\r ]+<li>[\t\n\r ]+<a href=\\'(http.*?)\\'").getColumn(0);
            if (allLinks == null || allLinks.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String forumLink : allLinks)
                decryptedLinks.add(createDownloadlink(forumLink));
        } else {
            br.getPage(parameter);
            if (br.containsHTML("(An error occurred|We\\'re sorry for the inconvenience)")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;

    }
}
