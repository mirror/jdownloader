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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jav68.tv" }, urls = { "http://(www\\.)?jav68\\.(tv|me)/.*" })
public class Jav68Tv extends PluginForDecrypt {

    public Jav68Tv(PluginWrapper wrapper) {
        super(wrapper);
    }

    // http://jav68.tv/watch/soe-077-rookie-girimoza-rookie-girimoza-hasui-shiho-1399 - Not found (empty)

    private String movieLink = null;
    private String watchLink = null;
    private String filename  = null;

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> crawledLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace(".tv", ".me");
        // parameter = parameter.replace("/watch/", "/movie/"); // Not the same (number) anymore(?)
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 Not Found<|Page not found")) {
            crawledLinks.add(createOfflinelink(parameter));
            return crawledLinks;
        }

        if (br.containsHTML("/watch/")) { // Single link
            crawlSingleLink(crawledLinks, parameter);
        } else { // Multi links, e.g.: http://jav68.tv/category-amateur
            final String fpName = "Jav68 " + new Regex(parameter, "http://jav68\\.(tv|me)/(.*)").getMatch(1);
            logger.info("fpName: " + fpName);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            String block = br.getRegex("(main_loop clearfix.*?)<footer").getMatch(0);
            if (block == null) { // http://jav68.me/
                block = br.getRegex("(<div class=\"movie-box\">.*?)<div class=\"right-menu\">").getMatch(0);
            }
            final String[] items = new Regex(block, "class=\"main-thumb\"\\s+href=\"(.*?)\" title=").getColumn(0);
            if ((items == null || items.length == 0) && crawledLinks.isEmpty()) {
                logger.warning("Decrypter broken (items regex) for link: " + parameter);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String item : items) {
                logger.info("item: " + item);
                // String fuid = new Regex(item, "<a href=\"(/[^<>\"]*?)\"").getMatch(0);
                // parameter = "http://www.jav68.tv" + fuid;
                br.getPage(item);
                crawlSingleLink(crawledLinks, item);
                fp.addLinks(crawledLinks);
            }
        }
        return crawledLinks;
    }

    private void crawlSingleLink(final ArrayList<DownloadLink> crawledLinks, final String singleLink) throws Exception {
        String title = br.getRegex("<title>(Movie|Watch) (.*?)</title>").getMatch(1);
        if (title == null) {
            logger.warning("Decrypter broken (title regex) for link: " + singleLink);
            return;
        }
        logger.info("title: " + title);
        filename = title.trim();
        if (singleLink.contains("/movie/")) {
            crawlImage(crawledLinks, singleLink);
            watchLink = br.getRegex("(http://[^/]+/watch/.*?)\"").getMatch(0);
            logger.info("watchLink: " + watchLink);
            br.getPage(watchLink);
        }
        // <link href="http://jav68.tv/watch/mstd-002-indecent-woman-suzuhara-emiri-that-lurk-in-st-girl-9943" rel="canonical"/>
        // <a href="http://down.jav68.tv/down/mstd-002-indecent-woman-suzuhara-emiri-that-lurk-in-st-girl-5324"
        if (singleLink.contains("/watch/")) {
            watchLink = singleLink;
            String downLink = br.getRegex("(http://[^/]+/down/.*?)\"").getMatch(0);
            movieLink = downLink.replace("down.", "").replace("/down/", "/movie/");
            logger.info("Punch this movieLink: " + movieLink);
            // br.getPage(movieLink);
            // crawlImage(crawledLinks, movieLink); - Should use br2 - final Browser br2 = br.cloneBrowser();
        }
        crawlWatchLink(crawledLinks, watchLink);
        String savedWatchLink = watchLink;
        // Find all other watch links here then decrypt them
        String servers = br.getRegex("(servers clearfix.*?</ul></div></div></div></div>)").getMatch(0);
        final String[] watchLinks = new Regex(servers, "(http://[^/]+/watch/.*?)\"").getColumn(0);
        for (final String watchLink : watchLinks) {
            if (!watchLink.equals(savedWatchLink)) {
                br.getPage(watchLink);
                crawlWatchLink(crawledLinks, watchLink); // GetPage is done above, but: dl.setProperty("mainlink", watchLink);
            }
        }
    }

    private void crawlWatchLink(final ArrayList<DownloadLink> crawledLinks, final String parameter) throws Exception {
        if (br.containsHTML("frameborder=\"0\" src=")) {
            if (br.containsHTML("player.jav68.me/plugins")) {
                String player = br.getRegex("frameborder=\"0\" src=\"(https?://.*?)\"").getMatch(0);
                br.getPage(player);
            }
            String externID = br.getRegex("frameborder=\"0\" src=\"(https?://.*?)\"").getMatch(0);
            logger.info("externID: " + externID);
            DownloadLink dl = createDownloadlink(externID);
            dl.setContentUrl(externID);
            dl.setFinalFileName(filename + "." + ".mp4");
            crawledLinks.add(dl);
        } else if (br.containsHTML("tplugin")) {
            crawlPlayLink(crawledLinks, watchLink);
        } else {
            // label: "360p", file:
            // "https://redirector.googlevideo.tv/videoplayback?requiressl=yes&id=ae78dcff3e68b445&itag=18&source=picasa&cmo=secure_transport%3Dyes&ip=0.0.0.0&ipbits=0&expire=1436784191&sparams=requiressl,id,itag,source,ip,ipbits,expire&signature=D6A6AF4329E5057C97D2D6F839A38474435C4A0.4D7C3C9F5EC70ABC97B7CC55D5D2AB3F63B54F79&key=lh1&file=sd/file.mp4"
            // "https://lh3.googleusercontent.com/3nB9TuugwkfWh3jtjoHWQMexwqFRR6QdB4prWCrTs4Fdfb7t2Q=m18"
            final String[] vps = { "360p" }; // Vertical pixel, video resolution // "720p", "480p",
            for (final String vp : vps) {
                String externID = br.getRegex(vp + "\", file: \"(https?://[^\"]+?)\"").getMatch(0);
                // String externID = br.getRegex("file: \"(https?://.*?)\"").getMatch(0);
                // Kalau vp pindah kebelakang, "720p" akan dapat "360p" dan "720p"
                if (externID == null) {
                    // externID = br.getRegex("href=\"(https?://.*?)\" class=\"btn btn-watch\">" + vp).getMatch(0);
                }
                if (externID == null) {
                    String label = br.getRegex("label: (\"" + vp + "\", file: .*?)\\}").getMatch(0);
                    logger.info(label + " (link is not found (- Gone))");
                    break;
                }
                logger.info("externID: " + externID);
                if (externID.contains("googleusercontent")) {
                    // if (br.containsHTML("frameborder=\"0\" src=")) { xxxxx
                    // Redirect to https://r14---sn-npo7enes.googlevideo.com/videoplayback?id=
                    // DownloadLink dl = createDownloadlink("directhttp://" + externID);
                    DownloadLink dl = createDownloadlink(externID);
                    dl.setContentUrl(externID);
                    dl.setFinalFileName(filename + "." + vp + ".mp4");
                    crawledLinks.add(dl);
                    continue;
                }
                DownloadLink dl = createDownloadlink(externID);
                dl.setContentUrl(externID);
                dl.setProperty("mainlink", watchLink);
                dl.setFinalFileName(filename + "." + vp + ".mp4");
                crawledLinks.add(dl);
            }
        }
        return;
    }

    private void crawlImage(final ArrayList<DownloadLink> crawledLinks, final String movieLink) throws Exception {
        // image: "http://img.jav68.tv/big84a0a0d9ab96a2c74eb6d825c6806f25.jpg"
        // <img src="http://img.jav68.tv/bigf35b3bee8aba4969f1a76067d1aa5fd3.jpg"
        String img = br.getRegex("(image: |img src=)\"(http://img.[^/]+/big[^\"]*?)\"").getMatch(1);
        logger.info("img: " + img);
        DownloadLink dlimg = createDownloadlink(img);
        dlimg.setFinalFileName(Encoding.htmlDecode(filename) + ".jpg");
        crawledLinks.add(dlimg);
        return;
    }

    private void crawlPlayLink(final ArrayList<DownloadLink> crawledLink, final String watchLink) throws Exception {
        final String watchId = new Regex(watchLink, "-([0-9]*)$").getMatch(0);
        logger.info("watchId: " + watchId);
        final Browser br2 = br.cloneBrowser();
        // String playLink = "http://play.cuteav.com/-" + watchId;
        String playLink = null;
        String[] playServers = { "javpub.me", "cuteav.com", "jav163.com", "javun.net", "javcen.me" };
        for (final String playServer : playServers) {
            playLink = "http://play." + playServer + "/-" + watchId;
            br2.getPage(playLink);
            // {label: "360p", file: "https://yt3.ggpht.com/.*?=m18","type":"video/mp4","default":"true"},
            // {label: "360p", file: "http://r7---sn-.*?.googlevideo.com/.*?signature=","type":"video/mp4","default":"true"},
            // String externID = br2.getRegex("file: \"(http.*?)\"").getMatch(0);
            String externID = br2.getRegex("file: \"(http[^\"]+)\"").getMatch(0);
            if (externID != null) {
                logger.info("externID: " + externID);
                DownloadLink dl = createDownloadlink(externID);
                dl.setFinalFileName(Encoding.htmlDecode(filename) + "." + ".mp4");
                dl.setProperty("mainlink", playLink);
                // dl.setContentUrl(externID); Create error in linkcollectornnnnn.zip?
                crawledLink.add(dl);
                break;
            }
        }
        return;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}