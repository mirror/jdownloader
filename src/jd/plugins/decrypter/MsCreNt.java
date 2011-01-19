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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "musicore.net" }, urls = { "http://(www\\.)?(r\\.)?musicore\\.net/(forums/index\\.php\\?/topic/\\d+-|forums/index\\.php\\?showtopic=\\d+|url\\.php\\?id=[A-Za-z0-9]+\\&url=[a-zA-Z0-9=\\+/\\-]+)" }, flags = { 0 })
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
        if (!parameter.contains("musicore.net/forums/index.php?")) {
            br.getPage(parameter);
            if (br.containsHTML("(An error occurred|We\\'re sorry for the inconvenience)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter is defect, browser contains: " + br.toString());
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            String topicID = new Regex(parameter, "(topic/|showtopic=)(\\d+)").getMatch(1);
            if (topicID == null) return null;
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("http://musicore.net/ajax/release.php?id=" + topicID);
            String redirectlinks[] = br.getRegex("(\\'|\")(http://r\\.musicore\\.net/\\?id=.*?url=.*?)(\\'|\")").getColumn(1);
            if (redirectlinks == null || redirectlinks.length == 0) return null;
            br.getPage("http://musicore.net/ajax/ftp.php?id=" + topicID);
            String ftpLinks[] = br.getRegex("\"(ftp://\\d+:[a-z0-9]+@ftp\\.musicore\\.ru/.*?)\"").getColumn(0);
            progress.setRange(redirectlinks.length);
            for (String redirectlink : redirectlinks) {
                redirectlink = Encoding.htmlDecode(redirectlink);
                br.getPage(redirectlink);
                String finallink = br.getRedirectLocation();
                if (finallink == null) finallink = br.getRegex("URL=(.*?)\"").getMatch(0);
                if (finallink == null) {
                    logger.warning("finallink from the following link had to be regexes and could not be found by the direct redirect: " + parameter);
                    logger.warning("Browser contains test: " + br.toString());
                    finallink = br.getRegex("URL=(.*?)\"").getMatch(0);
                }
                decryptedLinks.add(createDownloadlink(finallink));
                progress.increase(1);
            }
            if (ftpLinks != null && ftpLinks.length != 0) {
                for (String ftpLink : ftpLinks) {
                    decryptedLinks.add(createDownloadlink(ftpLink));
                }
            }
        }
        return decryptedLinks;

    }

}
