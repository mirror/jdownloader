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
import java.util.Arrays;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gametrailers.com" }, urls = { "http://[\\w\\.]*?gametrailers\\.com/(video|user\\-movie)/.*?/[0-9]+" }, flags = { 0 })
public class GameTrailersCom extends PluginForDecrypt {

    public GameTrailersCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final ArrayList<String> damnedIDs = new ArrayList<String>();
        br.getPage(parameter);
        br.setFollowRedirects(true);
        // you must logged in for mov/wmv download
        final String[] httpLinks = br.getRegex("<a href=\"(http://www.gametrailers\\.com/download/\\d+/[\\w-\\.\\_]+mp4)\"").getColumn(0);
        final ArrayList<String> finallinks = new ArrayList<String>(Arrays.asList(httpLinks));
        String videoTitle = br.getRegex("<title>(.*?)Video Game, Review Pod").getMatch(0);
        if (videoTitle == null) {
            videoTitle = br.getRegex("name=\"title\" content=\"(.*?)\"").getMatch(0);
            if (videoTitle == null) {
                videoTitle = br.getRegex("var gamename = \"(.*?)\"").getMatch(0);
            }
        }
        final String title = new Regex(videoTitle, "(.+?) - ").getMatch(0);
        final String subject = new Regex(videoTitle, ".*? - (.+)").getMatch(0);
        if (title != null && subject != null) {
            /* reformat the filename */
            videoTitle = subject + " - " + title;
        }
        final String hdid = br.getRegex("class=\"hdButton\"><a href=\"/video/.*?/(\\d+)\\?type").getMatch(0);
        if (hdid != null) {
            damnedIDs.add(hdid);
        }
        final String neoplayermovieid = br.getRegex("NeoPlayer\\.movieId=(\\d+)").getMatch(0);
        if (neoplayermovieid != null) {
            damnedIDs.add(neoplayermovieid);
        }
        if (damnedIDs == null || damnedIDs.size() == 0) { return null; }
        for (final String damnedID : damnedIDs) {
            final Browser br2 = br.cloneBrowser();
            if (parameter.contains("user-movie")) {
                br2.getPage("http://www.gametrailers.com/neo/?page=xml.mediaplayer.Mediagen&movieId=" + damnedID + "&hd=1&um=1");
            } else {
                br2.getPage("http://www.gametrailers.com/neo/?page=xml.mediaplayer.Mediagen&movieId=" + damnedID);
            }
            String finallink = br2.getRegex("<src>(.*?)</src>").getMatch(0);
            if (finallink != null) {
                // If we got the wrong link it's a long way to get the real
                // link!
                if (finallink.matches(".*?\\.gametrailers\\.com/gt_vault/\\.flv")) {
                    final String configPage = br.getRegex("var config_url = \"(.*?)\"").getMatch(0);
                    if (configPage == null) { return null; }
                    br.getPage("http://www.gametrailers.com" + Encoding.htmlDecode(configPage));
                    String infoPage = br.getRegex("<player>.*?<feed>(.*?)</feed>").getMatch(0);
                    if (infoPage == null) { return null; }
                    infoPage = infoPage.trim().replace("amp;", "");
                    br.getPage(infoPage);
                    String lastPage = br.getRegex("medium=\"video\".*?url=\"(http.*?)\"").getMatch(0);
                    if (lastPage == null) { return null; }
                    lastPage = lastPage.replace("amp;", "");
                    br.getPage(lastPage);
                    finallink = br.getRegex("<src>(.*?)</src>").getMatch(0);
                    if (finallink == null) { return null; }
                }
                finallinks.add(finallink);
            } else {
                logger.warning("Decrypter failed to get finallink from id " + damnedID + " from link: " + parameter);
            }
        }
        String[] extensions = { "flv", "mov", "wmv", "mp4" };
        ArrayList<String> done = new ArrayList<String>();
        for (String ext : extensions) {
            for (final String finallink : finallinks) {
                if (parameter.contains("user-movie") && !"flv".equals(ext)) {
                    /* user movies only have flv files */
                    continue;
                }
                String ext2 = new Regex(finallink, ".+(\\....)$").getMatch(0);
                String link = finallink.replace(ext2, "." + ext);
                String check = new Regex(link, ".+/(.*?\\....)$").getMatch(0);
                if (done.contains(check)) continue;
                done.add(check);
                DownloadLink dl = createDownloadlink("directhttp://" + link);
                URLConnectionAdapter con = null;
                long size = 0;
                try {
                    Browser br = new Browser();
                    br.setFollowRedirects(true);
                    con = br.openGetConnection(link);
                    if (!con.isOK() || (con.getContentType() != null && con.getContentType().contains("text"))) continue;
                    size = con.getLongContentLength();
                } catch (Throwable e) {
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
                dl.setDownloadSize(size);
                dl.setAvailable(true);
                if (videoTitle != null) {
                    // correct title without "HD" or "SD"
                    videoTitle = videoTitle.trim().replaceAll(" [SH]D ?", "");
                    if (finallink.contains("_hd")) {
                        dl.setFinalFileName(videoTitle.trim() + " [HD-" + ext.toUpperCase() + "]." + ext);
                    } else {
                        dl.setFinalFileName(videoTitle.trim() + " [SD-" + ext.toUpperCase() + "]." + ext);
                    }
                }
                decryptedLinks.add(dl);
            }

        }

        if (videoTitle != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(videoTitle.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
