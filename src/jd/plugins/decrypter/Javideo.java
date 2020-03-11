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
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "javr.club" }, urls = { "https?://(www\\.)?(javr\\.club|javnew\\.net)/.*" })
public class Javideo extends PluginForDecrypt {
    public Javideo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String filename   = null;
    private String fembedHost = null;

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
        if (br.containsHTML("<title>Watch")) { // Single link
            crawlSingleLink(crawledLinks, parameter);
        } else { // Multi links, e.g.: https://javr.club/movie-theme/uncensored-leak-jav/
            final String fpName = "Javr " + new Regex(parameter, "https://javr\\.club/(.*)").getMatch(0);
            logger.info("fpName: " + fpName);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            String block = br.getRegex("(<article .*?)page-navigation").getMatch(0);
            if (block == null) { // http://jav68.me/ ???
                block = br.getRegex("(<div class=\"movie-box\">.*?)<div class=\"right-menu\">").getMatch(0);
            }
            // final String[] items = new Regex(block, "(https://javr\\.club/[^/]/)\"").getColumn(0);
            final String[] items = br.getRegex("(https://javr\\.club/[^/]+?/)\"").getColumn(0);
            if ((items == null || items.length == 0) && crawledLinks.isEmpty()) {
                logger.warning("Decrypter broken (items regex) for link: " + parameter);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String item : items) {
                logger.info("item: " + item);
                // br.getPage(item);
                // crawlSingleLink(crawledLinks, item);
                // fp.addLinks(crawledLinks);
            }
        }
        return crawledLinks;
    }

    private void crawlSingleLink(final ArrayList<DownloadLink> crawledLinks, final String singleLink) throws Exception {
        String title = br.getRegex("<title>Watch Japanese Porn - (.*?) - JAVR.club</title>").getMatch(0);
        if (title == null) {
            logger.warning("Decrypter broken (title regex) for link: " + singleLink);
            return;
        }
        logger.info("title: " + title);
        filename = title.trim();
        String javideo = br.getRegex("allowfullscreen=[^<>]+?(http[^<>]+?)>").getMatch(0);
        javideo = javideo.replace("\\", "");
        logger.info("javideo: " + javideo);
        fembedHost = Browser.getHost(javideo);
        String file_id = new Regex(javideo, "/(?:f|v|api/sources?)/([a-zA-Z0-9_-]+)").getMatch(0);
        final PostRequest postRequest = new PostRequest("https://" + fembedHost + "/api/source/" + file_id);
        final Map<String, Object> response = JSonStorage.restoreFromString(br.getPage(postRequest), TypeRef.HASHMAP);
        if (!Boolean.TRUE.equals(response.get("success"))) {
            final DownloadLink link = createDownloadlink(javideo.replaceAll("https?://", "decryptedforFEmbedHosterPlugin://"));
            link.setAvailable(false);
            crawledLinks.add(link);
            return;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(filename.trim()));
        final List<Map<String, Object>> videos;
        if (response.get("data") instanceof String) {
            videos = (List<Map<String, Object>>) JSonStorage.restoreFromString((String) response.get("data"), TypeRef.OBJECT);
        } else {
            videos = (List<Map<String, Object>>) response.get("data");
        }
        for (final Map<String, Object> video : videos) {
            final DownloadLink link = createDownloadlink(javideo.replaceAll("https?://", "decryptedforFEmbedHosterPlugin://"));
            final String label = (String) video.get("label");
            final String type = (String) video.get("type");
            link.setProperty("label", label);
            link.setProperty("fembedid", file_id);
            link.setProperty("fembedHost", fembedHost);
            if (!StringUtils.isEmpty(title)) {
                link.setFinalFileName(title + "-" + label + "." + type);
            } else {
                link.setName(file_id + "-" + label + "." + type);
            }
            link.setAvailable(true);
            crawledLinks.add(link);
            fp.addLinks(crawledLinks);
        }
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

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}