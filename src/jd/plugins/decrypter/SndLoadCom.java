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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sound-load.com" }, urls = { "http://(www\\.)?sound\\-load\\.com/download/[^<>\"\\'/]+/\\d+/[^<>\"\\'/]+\\.html" }, flags = { 0 })
public class SndLoadCom extends PluginForDecrypt {

    public SndLoadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        Browser decryptBR = br.cloneBrowser();
        final String fpName = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"\\']+)\"").getMatch(0);
        String[] links = br.getRegex("title=\"Download part \\d+ @ [^<>\"\\'/]+\" onclick=\"window\\.open\\(\\'(http://[^<>\"\\']+)\\'\\)").getColumn(0);
        if (links != null && links.length != 0) {
            for (String singleLink : links) {
                decryptBR.getPage(Encoding.htmlDecode(singleLink));
                final String finallink = decryptBR.getRedirectLocation();
                if (finallink == null) {
                    continue;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            }
        }
        String[][] streamIDs = br.getRegex("<div id=\"filename_([^<>\"\\']+)\" style=\"clear: left; width: 485px; font\\-size: 7pt; padding\\-left: 10px;\">([^<>\"\\']+)</div>").getMatches();
        if (streamIDs != null && streamIDs.length != 0) {
            for (String streamID[] : streamIDs) {
                decryptBR.getPage("http://www.sound-load.com/api/player/" + streamID[0]);
                final String templink = "http://" + streamID[0] + ".track.sndapi.com";
                String finallink = null;
                final URLConnectionAdapter con = decryptBR.openGetConnection(templink);
                if (con.getContentType().contains("html")) {
                    decryptBR.followConnection();
                    finallink = decryptBR.getRedirectLocation();
                } else
                    finallink = templink;
                con.disconnect();
                if (finallink == null) continue;
                final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
                // Set final name here or we have no name
                dl.setFinalFileName(Encoding.htmlDecode(streamID[1]));
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
