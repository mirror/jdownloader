//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "korean720.com" }, urls = { "https?://(www\\.)?korean720\\.com/[a-z0-9\\-_/]+" })
public class AaaKorean720Com extends PluginForDecrypt {

    public AaaKorean720Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    // http://jav96.com/video/147/jav-uncen-babe-girl-japan-mayuka-akimoto

    private String fid       = null;
    private String filename  = null;
    private String videoLink = null;

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> crawledLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 Not Found<|Page not found")) {
            crawledLinks.add(createOfflinelink(parameter));
            return crawledLinks;
        }

        if (br.containsHTML("entry-embed")) { // Single link !"entry-box  first-row"
            crawlSingleLink(crawledLinks, parameter);
        } else { // Multi links, e.g.: http://korean720.com/category/...-.../page/2/
            final String fpName = "Korean720 " + new Regex(parameter, "http://korean720.com/(.*)").getMatch(0);
            logger.info("fpName: " + fpName);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            final String[] items = br.getRegex("\"entry-box.*?>\\s+<a href=\"([^\"]*?)\"").getColumn(0);
            if ((items == null || items.length == 0) && crawledLinks.isEmpty()) {
                logger.warning("Decrypter broken (items regex) for link: " + parameter);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String item : items) {
                logger.info("item: " + item);
                br.getPage(item);
                crawlSingleLink(crawledLinks, item);
                fp.addLinks(crawledLinks);
            }
        }

        return crawledLinks;
    }

    private void crawlSingleLink(final ArrayList<DownloadLink> crawledLinks, final String singleLink) throws Exception {
        // br.getPage(singleLink);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 Not Found<|Page not found")) {
            crawledLinks.add(createOfflinelink(singleLink));
            return;
        }
        String title = br.getRegex("<title>(.*?)\\|.*?</title>").getMatch(0);
        logger.info("title: " + title);
        filename = title.trim();
        final String[] videoLinks = br.getRegex("<iframe src=\"([^\"]*?)\"").getColumn(0);
        for (final String videoLink : videoLinks) {
            logger.info("videoLink: " + videoLink);
            DownloadLink dl = createDownloadlink(videoLink);
            dl.setFinalFileName(filename + ".mp4");
            dl.setContentUrl(videoLink);
            dl.setProperty("mainlink", "http://jav96.com/mobile_src.php?id=" + fid);
            dl.setProperty("fixName", filename + ".mp4");
            crawledLinks.add(dl);
        }
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}