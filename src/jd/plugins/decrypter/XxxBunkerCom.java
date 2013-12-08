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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

//EmbedDecrypter 0.1
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xxxbunker.com" }, urls = { "http://(www\\.)?xxxbunker\\.com/[a-z0-9_\\-]+" }, flags = { 0 })
public class XxxBunkerCom extends PluginForDecrypt {

    public XxxBunkerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

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
                return decryptedLinks;
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (br.containsHTML(">FILE NOT FOUND<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.getURL().equals("http://xxxbunker.com/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        if (br.containsHTML(">your video is being loaded, please wait")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) filename = br.getRegex("class=vpVideoTitle><h1 itemprop=\"name\">([^<>\"]*?)</h1>").getMatch(0);
        // filename needed for all IDs below here
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = Encoding.htmlDecode(filename.trim());
        String externID = null;
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
            externID = new Regex(remoteembedcode, "\"(http://(www\\.)?hardsextube\\.com/[^<>\"]*?)\"").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink(externID));
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
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final Browser br2 = br.cloneBrowser();
        externID = br.getRegex("file%3D(http[^<>\"]*?)%26amp%").getMatch(0);
        if (externID != null) {
            externID = Encoding.deepHtmlDecode(externID);
            br2.getPage(externID);
            boolean success = true;
            externID = br2.getRedirectLocation();
            if (externID == null) {
                success = false;
            }
            if (success && checkDirectLink(externID)) {
                final DownloadLink dl = createDownloadlink(externID + ".jdeatme");
                dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
                decryptedLinks.add(dl);
                return decryptedLinks;
            } else {
                externID = br.getRegex("player\\.swf\\?config=(http%3A%2F%2Fxxxbunker\\.com%2FplayerConfig\\.php%3F[^<>\"]*?)\"").getMatch(0);
                if (externID != null) {
                    br2.getPage(Encoding.htmlDecode(externID));
                    String relayurl = br2.getRegex("<relayurl>([^<>\"]*?)</relayurl>").getMatch(0);
                    externID = br2.getRegex("<file>(http[^<>\"]*?)</file>").getMatch(0);
                    if (relayurl == null) relayurl = externID;
                    if (relayurl == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    relayurl = Encoding.Base64Decode(relayurl);
                    final DownloadLink dl = createDownloadlink(relayurl + ".jdeatme");
                    dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                }
            }
        }
        if (externID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private boolean checkDirectLink(final String directlink) {
        try {
            final Browser br2 = br.cloneBrowser();
            URLConnectionAdapter con = br2.openGetConnection(directlink);
            if (con.getContentType().contains("html") || con.getLongContentLength() == -1) { return false; }
            con.disconnect();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}