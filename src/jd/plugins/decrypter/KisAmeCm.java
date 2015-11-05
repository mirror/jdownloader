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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;

/**
 *
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision: 20515 $", interfaceVersion = 3, names = { "kissanime.com" }, urls = { "https?://(?:www\\.)?kissanime\\.(?:com|to)/anime/[a-zA-Z0-9\\-\\_]+/[a-zA-Z0-9\\-\\_]+" }, flags = { 0 })
public class KisAmeCm extends antiDDoSForDecrypt {

    public KisAmeCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        /* Error handling */
        if (br.containsHTML("Page Not Found") || br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String title = br.getRegex("<title>\\s*(.*?)\\s*- Watch\\s*\\1[^<]*</title>").getMatch(0);
        if (title == null) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        title = title.replaceAll("\\s+", " ");
        // we have two things we need to base64decode
        final String qualityselection = br.getRegex("<select id=\"selectQuality\">.*?</select").getMatch(-1);
        if (qualityselection != null) {
            final String[][] quals = new Regex(qualityselection, "<option [^>]*value\\s*=\\s*('|\"|)(.*?)\\1[^>]*>(\\d+p)").getMatches();
            if (quals != null) {
                for (final String qual[] : quals) {
                    String decode = Encoding.Base64Decode(qual[1]);
                    if (StringUtils.contains(decode, "blogspot.com/")) {
                        // this is redirect bullshit
                        final Browser test = new Browser();
                        test.getPage(decode);
                        decode = test.getRedirectLocation();
                    }
                    DownloadLink dl = createDownloadlink(decode);
                    dl.setFinalFileName(title + "-" + qual[2] + ".mp4");
                    dl.setAvailableStatus(AvailableStatus.TRUE);
                    decryptedLinks.add(dl);
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}