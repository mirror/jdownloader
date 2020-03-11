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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dramanice.us" }, urls = { "http://(www\\.)?dramanice\\.(com|eu|to|us)/.*" })
public class DramaniceUs extends PluginForDecrypt {

    public DramaniceUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> crawledLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 Not Found<|Page not found")) {
            crawledLinks.add(createOfflinelink(parameter));
            return crawledLinks;
        }

        if (parameter.contains("/watch")) { // Single links
            crawlSingleLink(crawledLinks, parameter);
        } else { // Multi links, e.g.: http://www.dramanice.us/drama/empress-ki-detail - Page one only
            final String fpName = "Dramanice_ " + new Regex(parameter, "http://www.dramanice\\.[a-z]+/drama/(.*)-detail").getMatch(0);
            logger.info("fpName: " + fpName);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            String block = br.getRegex("(<div class=\"list-episodes.*?)<div class=\"clearfix\">").getMatch(0);
            final String[] items = new Regex(block, "<div class=\"col-md-3 item-episode\">\\s+<a href=\"(.*?)\"").getColumn(0);
            if ((items == null || items.length == 0) && crawledLinks.isEmpty()) {
                logger.warning("Decrypter broken (items regex) for link: " + parameter);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String item : items) {
                logger.info("item: " + item);
                // String fuid = new Regex(item, "<a href=\"(/[^<>\"]*?)\"").getMatch(0);
                // parameter = "http://www.javpub.com" + fuid;
                br.getPage(item);
                crawlSingleLink(crawledLinks, item);
                fp.addLinks(crawledLinks);
            }
        }

        return crawledLinks;
    }

    private void crawlSingleLink(final ArrayList<DownloadLink> crawledLinks, final String singleLink) throws Exception {
        // <title>Watch ... </title>
        String title = br.getRegex("<title>(Movie|Watch) (.*?)</title>").getMatch(1);
        if (title == null) {
            logger.warning("Decrypter broken (title regex) for link: " + singleLink);
            return;
        }
        logger.info("title: " + title);
        String filename = title.trim();
        String refreshLink = singleLink;
        // <source src="https://lh3.googleusercontent.com/67Iy2lmF0QRUCm2HhEQMEWQllMs4gxKAQG7rpq4swQkQ=m18" type="video/mp4" data-res="360"
        // <source src="http://2.bp.blogspot.com/AD7bpMc7P3kVb8NqyEXia2dPUwwd6TSTWF9GfpM4VR9B7wWxsQ8agXixEm2FvPe_tJfG=m18" . data-res="360"
        // <iframe ... src="http://cdn.dramanice.to/embed-drive.php?id=...
        final String[] vps = { "360p" }; // Vertical pixel, video resolution
        for (final String vp : vps) {
            String externID = br.getRegex("<source src=\"(https?://.*?)\" type=\"video/mp4\" data-res=\"360\"").getMatch(0);
            if (externID != null && !externID.contains("google")) {
                br.setFollowRedirects(false);
                br.getPage(externID);
                externID = br.getRedirectLocation();
            }
            if (externID == null && br.containsHTML("embed.php")) {
                // <iframe ... src="http://k-vid.com/embed.php?id=NDgzOTM===&width=728&height=450&title= ... &typesub=SUB&sub="
                refreshLink = br.getRegex("<iframe [^<>]*? src=\"(http[^<>]*?)\"\\s*target=\"_blank\">").getMatch(0);
                br.getPage(refreshLink);
                externID = br.getRegex("<source src=\'(https?://.*?)\' type=\'video/mp4\' label=\'360\'").getMatch(0);
            }
            if (externID == null) {
                String source = br.getRegex("<(source.*?)>").getMatch(0);
                logger.info("source: " + source + " (link is not found)");
                break;
            }
            logger.info("externID: " + externID);

            DownloadLink dl = createDownloadlink(externID);
            dl.setContentUrl(externID);
            dl.setFinalFileName(filename + "." + vp + ".mp4");
            dl.setProperty("mainlink", refreshLink);
            crawledLinks.add(dl);
            distribute(dl);
        }
        // image: "http://../big84a0a0d9ab96a2c74eb6d825c6806f25.jpg"
        /*
         * String img = br.getRegex("image: \"(http://[^\"]*?)\"").getMatch(0); DownloadLink dlimg = createDownloadlink(img);
         * dlimg.setFinalFileName(Encoding.htmlDecode(filename) + ".jpg"); crawledLinks.add(dlimg);
         */
        return;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}