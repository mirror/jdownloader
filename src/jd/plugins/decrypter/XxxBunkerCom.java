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
        String externID = br.getRegex("amfdata=http%3A%2F%2F141\\.\\d+\\.\\d+\\.\\d+%2Fflashservices%2Fgateway\\.php%7C(\\d+)%7Cdefault%7C\\&amp;").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID));
            return decryptedLinks;
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
        externID = br.getRegex("file%3D(http[^<>\"]*?)%26amp%").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(externID));
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (externID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}