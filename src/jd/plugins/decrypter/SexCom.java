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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sex.com" }, urls = { "http://(www\\.)?sex\\.com/(pin/\\d+/|picture/\\d+)" }, flags = { 0 })
public class SexCom extends PluginForDecrypt {

    public SexCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("/pin/", "/picture/");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">Page Not Found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String filename = br.getRegex("<title>([^<>\"]*?) \\| Sex Videos and Pictures \\| Sex\\.com</title>").getMatch(0);
        if (filename == null || filename.length() <= 2) filename = br.getRegex("addthis:title=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null || filename.length() <= 2) filename = br.getRegex("property=\"og:title\" content=\"([^<>]*?)\\-  Pin #\\d+ \\| Sex\\.com\"").getMatch(0);
        if (filename == null || filename.length() <= 2) filename = br.getRegex("<div class=\"pin\\-header navbar navbar\\-static\\-top\">[\t\n\r ]+<div class=\"navbar\\-inner\">[\t\n\r ]+<h1>([^<>]*?)</h1>").getMatch(0);
        if (filename == null || filename.length() <= 2) filename = new Regex(parameter, "(\\d+)/?$").getMatch(0);
        filename = Encoding.htmlDecode(filename.trim());
        filename = filename.replace("#", "");
        String externID = br.getRegex("<div class=\"from\">From <a rel=\"nofollow\" href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("<link rel=\"image_src\" href=\"(http[^<>\"]*?)\"").getMatch(0);
        // For .gif images
        if (externID == null) externID = br.getRegex("<div class=\"image_frame\">[\t\n\r ]+<img alt=\"\" title=\"\" src=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + externID.substring(externID.lastIndexOf(".")));
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