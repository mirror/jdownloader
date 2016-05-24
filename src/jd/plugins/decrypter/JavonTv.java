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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sexvidx.tv" }, urls = { "http://sexvidx.tv/[a-z0-9\\-/]*?/\\d+/[a-z0-9\\-]+\\.html" }, flags = { 0 })
public class JavonTv extends PluginForDecrypt {

    public JavonTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    // javon.tv is back to sexvidx.tv
    private String filename = null;
    private String externID = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> crawledLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            crawledLinks.add(createOfflinelink(parameter));
            return crawledLinks;
        }
        if (parameter.contains("/info-movie/")) {
            String watchLink = br.getRegex("(https?://sexvidx.tv/watch/movie.*?)\"").getMatch(0);
            br.getPage(watchLink);
        }
        filename = br.getRegex("top-title\">(?:Watch Online \\[Full Dvd\\] )?([^<>|]+)").getMatch(0);
        filename = Encoding.htmlDecode(filename.trim());
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(filename);
        final String[] watchLinks = br.getRegex("<a href=\"(/watch/movie[^\"]+)\"").getColumn(0);
        for (final String watchLink : watchLinks) {
            logger.info("watchLink: " + watchLink);
            br.getPage(watchLink);
            crawlWatchLink(crawledLinks, parameter);
            fp.addLinks(crawledLinks);
        }
        if (watchLinks.length == 0) {
            crawlWatchLink(crawledLinks, parameter);
        }
        return crawledLinks;
    }

    private void crawlWatchLink(final ArrayList<DownloadLink> crawledLinks, final String parameter) throws Exception {
        if (br.containsHTML("<iframe")) {
            externID = br.getRegex("<iframe.*? src=(\"|\')(https?.*?)(\"|\')").getMatch(1);
        }
        if (externID == null) {
            if (!br.containsHTML("s1\\.addParam\\(\\'flashvars\\'")) {
                logger.info("Link offline: " + parameter);
                return;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return;
        }
        if (externID.contains("cloudtime.to/embed/")) {
            String vid = br.getRegex("https?://(www.)?cloudtime.to/embed/\\?v=(.*)").getMatch(1);
            externID = "http://www.cloudtime.to/video/" + vid;
        }
        logger.info("externID: " + externID);
        externID = Encoding.htmlDecode(externID);
        DownloadLink dl = createDownloadlink(externID);
        dl.setFinalFileName(filename + ".mp4");
        crawledLinks.add(dl);
        return;
    }
}