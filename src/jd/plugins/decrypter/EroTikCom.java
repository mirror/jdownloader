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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ero-tik.com" }, urls = { "https?://(?:www\\.)?ero\\-tik\\.com/[^<>\"/]+\\.html" })
public class EroTikCom extends PluginForDecrypt {
    public EroTikCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> crawledLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches("https?://www\\.ero-tik\\.com/(article|browse|contact_us|login|memberlist|profile|register)\\.html")) {
            logger.info("Unsupported/invalid link: " + parameter);
            return crawledLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getRedirectLocation() != null && br.getRedirectLocation().contains("/index.html")) {
            crawledLinks.add(this.createOfflinelink(parameter));
            return crawledLinks;
        }
        if (br.containsHTML("video-watch")) { // Single links
            crawlSingleLink(crawledLinks, parameter);
        } else { // Multi links
            final String fpName = "Ero-tik " + new Regex(parameter, "https?://www\\.ero-tik\\.com/(.*)\\.html").getMatch(0);
            logger.info("fpName: " + fpName);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            final String[] items = br.getRegex("<h3><a href=\"([^<>\"]+?)\" class=\"pm-title-link ?\"").getColumn(0);
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

    private void crawlSingleLink(final ArrayList<DownloadLink> crawledLinks, final String parameter) throws Exception {
        String title = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (title == null) {
            logger.warning("Decrypter broken (title regex) for link: " + parameter);
            return;
        }
        logger.info("title: " + title);
        String filename = title;
        filename = filename.trim();
        // src="http://videomega.tv/validatehash.php?hashkey=050116111118107087049053074086086074053049087107118111116050"
        // src="http://videomega.tv/iframe.js"
        // src="http://www.ero-tik.com/embed.php?vid=188412d51"
        String externID = br.getRegex("\"(https?://videomega\\.tv/[^<>\"]*?)\"").getMatch(0);
        String embed = br.getRegex("src=\"(https?://www\\.ero-tik\\.com/embed[^<>\"]*?)\"").getMatch(0);
        if (externID == null && embed == null) {
            logger.info("externID & embed not found");
            crawledLinks.add(this.createOfflinelink(parameter));
            return;
        }
        if (externID == "http://videomega.tv/iframe.js") {
            br.getPage(externID);
            String ref = br.getRegex(">ref=\"([^<>\"]*?)\"").getMatch(0);
            externID = "http://videomega.tv/view.php?ref=" + ref;
        }
        if (externID == null && embed != null) {
            br.getPage(embed);
            String ref = br.getRegex(">ref=\"([^<>\"]*?)\"").getMatch(0);
            if (ref != null) {
                externID = "http://videomega.tv/view.php?ref=" + ref;
            } else if (br.containsHTML("<iframe")) {
                externID = br.getRegex("<iframe .*?src=\"(http[^<>\"]*?)\"").getMatch(0);
            } else {
                logger.info("Empty embed, no iframe");
                crawledLinks.add(this.createOfflinelink(parameter));
                return;
            }
        }
        if (externID.contains("freemix")) {
            /* 2016-05-25: xxx.freemixporn.com is down - if it comes back online we can add a full plugin for it. */
            crawledLinks.add(this.createDownloadlink(externID));
            return;
        }
        if (externID == null) {
            logger.warning("Decrypter broken (externID == null) for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("externID: " + externID);
        DownloadLink dl = createDownloadlink(externID);
        if (externID.contains("validatehash.php?hashkey=")) {
            externID = externID.replace("validatehash.php?hashkey=", "genembedv2.php?ref=");
        }
        dl.setContentUrl(externID);
        dl.setFinalFileName(filename + "." + "mp4");
        crawledLinks.add(dl);
        logger.info("crawledLinks.add(dl) done");
        return;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}