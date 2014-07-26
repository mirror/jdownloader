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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4sex4.com" }, urls = { "http://(www\\.)?4sex4\\.com/(tube/gallery\\-[a-z0-9\\-_]+\\.html|\\d+/.*?\\.html)" }, flags = { 0 })
public class FourSexFourCom extends PluginForDecrypt {

    public FourSexFourCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    private String                  PARAMETER      = null;
    private static final String     TYPE_GALLERY   = "http://(www\\.)?4sex4\\.com/tube/gallery\\-[a-z0-9\\-_]+\\.html";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        PARAMETER = param.toString();
        br.setFollowRedirects(true);
        br.getPage(PARAMETER);
        br.setFollowRedirects(false);
        if (PARAMETER.matches(TYPE_GALLERY)) {
            decryptGallery();
        } else {
            decryptEmbeddedVideos();
        }
        return decryptedLinks;
    }

    private void decryptGallery() throws IOException, DecrypterException {
        String fpName = br.getRegex("<title>([^<>\"]*?) Gallery photo</title>").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(PARAMETER, "4sex4\\.com/tube/gallery\\-([a-z0-9\\-_]+)\\.html").getMatch(0);
        }
        final String[] piclinks = br.getRegex("\\'(tube/galleries/[^<>\"]*?)\\'").getColumn(0);
        if (piclinks == null || piclinks.length == 0) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        for (String piclink : piclinks) {
            piclink = "directhttp://http://www.4sex4.com/" + piclink;
            final DownloadLink dl = createDownloadlink(piclink);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
    }

    private void decryptEmbeddedVideos() throws IOException, DecrypterException {
        if (br.containsHTML("megaporn\\.com/e/")) {
            logger.info("Link offline: " + PARAMETER);
            return;
        }
        String externID = br.getRegex("name=\"FlashVars\" value=\"options=(http://(www\\.)?extremetube\\.com/embed_player\\.php\\?id=\\d+)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return;
        }
        // xvideos.com 1
        externID = br.getRegex("xvideos\\.com/embedframe/(\\d+)").getMatch(0);
        /* fucking8 is probably outdated here - regex below will simply grab the available directlinks */
        if (externID == null) {
            externID = br.getRegex("fucking8\\.com/includes/player/\\?video=xv(\\d+)\"").getMatch(0);
        }
        if (externID == null) {
            externID = br.getRegex("\\?video=xv(\\d+)\\'").getMatch(0);
        }
        if (externID == null) {
            externID = br.getRegex("value=\"id_video=(\\d+)\" /><embed src=\"http://static\\.xvideos\\.com/").getMatch(0);
        }
        if (externID == null) {
            externID = br.getRegex("http://(www\\.)?xvideos\\.com/video(\\d+)").getMatch(1);
        }
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID));
            return;
        }
        externID = br.getRegex("emb\\.slutload\\.com/([A-Za-z0-9]+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://slutload.com/watch/" + externID));
            return;
        }
        externID = br.getRegex("xhamster\\.com/xembed\\.php\\?video=(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://xhamster.com/movies/" + externID + "/" + System.currentTimeMillis() + ".html"));
            return;
        }
        externID = br.getRegex("(\"|\\')(http://(www\\.)?tube8\\.com/embed/[^<>\"/]*?/[^<>\"/]*?/\\d+/?)(\"|\\')").getMatch(1);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID.replace("tube8.com/embed/", "tube8.com/")));
            return;
        }
        externID = br.getRegex("redtube\\.com/player/\"><param name=\"FlashVars\" value=\"id=(\\d+)\\&").getMatch(0);
        if (externID == null) {
            externID = br.getRegex("embed\\.redtube\\.com/player/\\?id=(\\d+)\\&").getMatch(0);
        }
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.redtube.com/" + externID);
            decryptedLinks.add(dl);
            return;
        }
        // youporn.com handling 1
        externID = br.getRegex("youporn\\.com/embed/(\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("http://www.youporn.com/watch/" + externID + "/" + System.currentTimeMillis());
            decryptedLinks.add(dl);
            return;
        }
        // Filename needed for all sites below
        String filename = br.getRegex("<title>(.*?) \\- 4sex4\\.com</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        }
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        filename = Encoding.htmlDecode(filename.trim());
        externID = br.getRegex("(https?://www\\.keezmovies\\.com/embed_player\\.php\\?v?id=\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            dl.setFinalFileName(filename);
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("shufuni\\.com/Flash/.*?flashvars=\"VideoCode=(.*?)\"").getMatch(0);
        if (externID != null) {
            if (filename == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            DownloadLink dl = createDownloadlink("http://www.shufuni.com/handlers/FLVStreamingv2.ashx?videoCode=" + externID);
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            decryptedLinks.add(dl);
            return;
        }
        // youporn.com handling 2
        externID = br.getRegex("flashvars=\"file=(http%3A%2F%2Fdownload\\.youporn\\.com[^<>\"]*?)\\&").getMatch(0);
        if (externID != null) {
            br.setCookie("http://youporn.com/", "age_verified", "1");
            br.setCookie("http://youporn.com/", "is_pc", "1");
            br.setCookie("http://youporn.com/", "language", "en");
            br.getPage(Encoding.htmlDecode(externID));
            if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
                logger.warning("FourSexFourCom -> youporn link invalid, please check browser to confirm: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            if (br.containsHTML("download\\.youporn\\.com/agecheck")) {
                logger.info("Link broken or offline: " + PARAMETER);
                return;
            }
            externID = br.getRegex("\"(http://(www\\.)?download\\.youporn.com/download/\\d+/\\?xml=1)\"").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            br.getPage(externID);
            final String finallink = br.getRegex("<location>(http://.*?)</location>").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(finallink));
            String type = br.getRegex("<meta rel=\"type\">(.*?)</meta>").getMatch(0);
            if (type == null) {
                type = "flv";
            }
            dl.setFinalFileName(filename + "." + type);
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("pornhub\\.com/embed/(\\d+)").getMatch(0);
        if (externID == null) {
            externID = br.getRegex("pornhub\\.com/view_video\\.php\\?viewkey=(\\d+)").getMatch(0);
        }
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.pornhub.com/view_video.php?viewkey=" + externID);
            decryptedLinks.add(dl);
            return;
        }
        // pornhub handling number 2
        externID = br.getRegex("name=\"FlashVars\" value=\"options=(http://(www\\.)?pornhub\\.com/embed_player(_v\\d+)?\\.php\\?id=\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("file:\"(http://fucking8\\.com/video/\\d+\\.mp4)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".mp4");
            decryptedLinks.add(dl);
            return;
        }
        // Decrypt images
        // class="embed"><div style="position:relative;z-index:1;"><img src="http://www.4sex4.com/uploads/img_1356498293.gif"
        externID = br.getRegex("<div class=\"embed\".*?img src=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(externID));
            dl.setFinalFileName(filename + "." + externID.substring(externID.lastIndexOf(".")));
            decryptedLinks.add(dl);
            return;
        }
        logger.warning("Decrypter broken for link: " + PARAMETER);
        throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}