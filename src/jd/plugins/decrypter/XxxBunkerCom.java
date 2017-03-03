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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xxxbunker.com" }, urls = { "http://(www\\.)?xxxbunker\\.com/[a-z0-9_\\-]+" })
public class XxxBunkerCom extends PornEmbedParser {

    public XxxBunkerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */

    private static final String INVALIDLINKS = "http://(www\\.)?xxxbunker\\.com/(search|javascript|tos|flash|footer|display|videoList|embedcode_|categories|newest|toprated|mostviewed|pornstars|forgotpassword|ourfavorites|signup|contactus|community|tags)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(parameter);
            if (con.getResponseCode() == 404) {
                logger.info("Link offline: " + parameter);
                decryptedLinks.add(getOffline(parameter));
                return decryptedLinks;
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (br.containsHTML(">FILE NOT FOUND<|>this video is no longer available")) {
            decryptedLinks.add(getOffline(parameter));
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        } else if (br.getURL().equals("http://xxxbunker.com/")) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        } else if (br.containsHTML(">your video is being loaded, please wait")) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        } else if (br.containsHTML("<strong>SITE MAINTENANCE</strong>")) {
            logger.info("Site maintenance, cannot decrypt link: " + parameter);
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=vpVideoTitle><h1 itemprop=\"name\">([^<>\"]*?)</h1>").getMatch(0);
        }
        if (filename == null) {
            filename = new Regex(parameter, "xxxbunker\\.com/(.+)").getMatch(0);
        }
        // filename needed for all IDs below here
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = Encoding.htmlDecode(filename.trim());

        String externID = null;
        String externID2 = null;
        String externID3 = null;
        String embedcode = br.getRegex(">localembedcode=\\'([^<>\"]*?)\\'").getMatch(0);
        if (embedcode != null) {
            embedcode = Encoding.htmlDecode(externID);
            externID = new Regex(embedcode, "gateway\\.php%7C(\\d+)%7Cdefault%7C\\&").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID));
                return decryptedLinks;
            }
        }
        String remoteembedcode = br.getRegex("remoteembedcode=\\'([^<>\"]*?)\\'").getMatch(0);
        if (remoteembedcode != null) {
            remoteembedcode = Encoding.htmlDecode(remoteembedcode);
            final String html_before = this.br.toString();
            br.getRequest().setHtmlCode(remoteembedcode);
            decryptedLinks.addAll(findEmbedUrls(filename));
            if (decryptedLinks.size() > 0) {
                return decryptedLinks;
            }
            br.getRequest().setHtmlCode(html_before);
        }
        externID3 = br.getRegex("lvid=(\\d+)").getMatch(0);
        if (externID3 != null) {
            br.getPage("http://xxxbunker.com/videoPlayer.php?videoid=" + externID3 + "&autoplay=true&ageconfirm=true&title=true&html5=false&hasflash=true&r=" + System.currentTimeMillis());
            externID = br.getRegex("\\&amp;file=(http[^<>\"]*?\\.(?:flv|mp4))").getMatch(0);
            if (externID != null) {
                final DownloadLink dl = createDownloadlink(parameter.replace("xxxbunker.com/", "xxxbunkerdecrypted.com/"));
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
        }
        externID = br.getRegex("postbackurl=([^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            externID = Encoding.Base64Decode(Encoding.htmlDecode(externID));
            con = br.openGetConnection(externID);
            if (!con.getContentType().contains("html")) {
                con.disconnect();
            } else {
                br.followConnection();
                con.disconnect();
                br.getPage(Encoding.Base64Decode(externID));
                externID = br.getRedirectLocation();
                if (externID == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(externID));
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("%26amp%3Bpostbackurl%3D([^<>\"]*?)%26amp").getMatch(0);
        if (externID == null) {
            externID = br.getRegex("%26amp%3Bfile%3D(http[^<>\"]*?)%26amp").getMatch(0);
        }
        if (externID != null) {
            externID = Encoding.deepHtmlDecode(externID);
            if (!StringUtils.startsWithCaseInsensitive(externID, "http")) {
                externID = Encoding.Base64Decode(externID);
            }
            externID2 = new Regex(externID, "(\\d+)\\.x\\.xvideos\\.com/").getMatch(0);
            if (externID2 != null) {
                decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID2));
                return decryptedLinks;
            }
            boolean success = true;
            if (success) {
                externID = "directhttp://" + externID;
                /* Do not use directhttp or jdeatme here, it will fail! */
                final DownloadLink dl = createDownloadlink(externID);
                dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
                decryptedLinks.add(dl);
                return decryptedLinks;
            } else {
                externID = br.getRegex("player\\.swf\\?config=(http%3A%2F%2Fxxxbunker\\.com%2FplayerConfig\\.php%3F[^<>\"]*?)\"").getMatch(0);
                if (externID != null) {
                    final DownloadLink dl = createDownloadlink(parameter.replace("xxxbunker.com/", "xxxbunkerdecrypted.com/"));
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                }
            }
        }
        logger.warning("Decrypter broken for link: " + parameter);
        return null;
    }

    private DownloadLink getOffline(final String parameter) {
        final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        return offline;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}