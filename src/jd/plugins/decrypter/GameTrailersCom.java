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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gametrailers.com" }, urls = { "http://[\\w\\.]*?gametrailers\\.com/(video|user-movie)/.*?/[0-9]+" }, flags = { 0 })
public class GameTrailersCom extends PluginForDecrypt {

    public GameTrailersCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        ArrayList<String> damnedIDs = new ArrayList<String>();
        br.getPage(parameter);
        String videoTitle = br.getRegex("<title>(.*?)Video Game, Review Pod").getMatch(0);
        if (videoTitle == null) {
            videoTitle = br.getRegex("name=\"title\" content=\"(.*?)\"").getMatch(0);
            if (videoTitle == null) {
                videoTitle = br.getRegex("var gamename = \"(.*?)\"").getMatch(0);
            }
        }
        String hdid = br.getRegex("class=\"hdButton\"><a href=\"/video/.*?/(\\d+)\\?type").getMatch(0);
        if (hdid != null) damnedIDs.add(hdid);
        String neoplayermovieid = br.getRegex("NeoPlayer\\.movieId=(\\d+)").getMatch(0);
        if (neoplayermovieid != null) damnedIDs.add(neoplayermovieid);
        if (damnedIDs == null || damnedIDs.size() == 0) return null;
        for (String damnedID : damnedIDs) {
            Browser br2 = br.cloneBrowser();
            br2.getPage("http://www.gametrailers.com/neo/?page=xml.mediaplayer.Mediagen&movieId=" + damnedID);
            String finallink = br2.getRegex("<src>(.*?)</src>").getMatch(0);
            if (finallink != null) {
                // If we got the wrong link it's a long way to get the real
                // link!
                if (finallink.equals("http://trailers-ll.gametrailers.com/gt_vault/.flv")) {
                    String configPage = br.getRegex("var config_url = \"(.*?)\"").getMatch(0);
                    if (configPage == null) return null;
                    br.getPage("http://www.gametrailers.com" + Encoding.htmlDecode(configPage));
                    String infoPage = br.getRegex("<player>.*?<feed>(.*?)</feed>").getMatch(0);
                    if (infoPage == null) return null;
                    infoPage = infoPage.trim().replace("amp;", "");
                    br.getPage(infoPage);
                    String lastPage = br.getRegex("medium=\"video\".*?url=\"(http.*?)\"").getMatch(0);
                    if (lastPage == null) return null;
                    lastPage = lastPage.replace("amp;", "");
                    br.getPage(lastPage);
                    finallink = br.getRegex("<src>(.*?)</src>").getMatch(0);
                    if (finallink == null) return null;
                }
                DownloadLink dl = createDownloadlink("directhttp://" + finallink);
                if (videoTitle != null) {
                    videoTitle = videoTitle.trim();
                    if (finallink.contains("_hd")) {
                        dl.setFinalFileName("HD - " + videoTitle + ".flv");
                    } else {
                        dl.setFinalFileName("SD - " + videoTitle + ".flv");
                    }
                }
                decryptedLinks.add(dl);
            } else
                logger.warning("Decrypter failed to get finallink from id " + damnedID + " from link: " + parameter);
        }
        if (videoTitle != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(videoTitle.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
