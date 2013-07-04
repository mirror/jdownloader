//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangatraders.com" }, urls = { "http://(www\\.)?mangatraders\\.com/manga/series/\\d+" }, flags = { 0 })
public class MngTrdCm extends PluginForDecrypt {

    public MngTrdCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookie("http://www.mangatraders.com", "language", "english");
        br.setFollowRedirects(false);
        br.getPage(parameter);
        // return error message for invalid url
        if (br.containsHTML(">Error \\- Page Not Found<")) {
            logger.warning("Invalid URL: " + parameter);
            return decryptedLinks;
        }

        // Set package name and prevent null field from creating plugin errors
        String fpName = br.getRegex("<title>Manga Traders \\- (.*?)</title>").getMatch(0);
        if (fpName == null) fpName = "Untitled";
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);

        // First getPage listening regex
        String[] links = br.getRegex("<a href=\"/view/file/(\\d+)\" class=\"link20\">").getColumn(0);
        String[] pages = br.getRegex("<a href=\"(/manga/series/\\d+/page/\\d+/)\">").getColumn(0);

        // Catch first page for links
        if (links == null || links.length == 0) links = br.getRegex("\"/download/file/(\\d+)?\"").getColumn(0);
        if ((links == null || links.length == 0) && (pages == null || pages.length == 0)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (links != null && links.length != 0) {
            for (String dl : links)
                decryptedLinks.add(createDownloadlink("http://www.mangatraders.com/download/file/" + dl));
        }

        // Catch for the first page and links within subsequence pages. Instead of loading back into the plugin as this creates multiple
        // packages of the same name (==bad|dirty). Might need to adjust in the future. As far as I could tell all pages are shown on the
        // first page.
        if (pages != null && pages.length != 0) {
            for (String page : pages) {
                br.getPage(page);
                links = br.getRegex("<a href=\"/view/file/(\\d+)\" class=\"link20\">").getColumn(0);
                if (links == null || links.length == 0) links = br.getRegex("\"/download/file/(\\d+)?\"").getColumn(0);
                if ((links == null || links.length == 0)) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (links != null && links.length != 0) {
                    for (String dl : links)
                        decryptedLinks.add(createDownloadlink("http://www.mangatraders.com/download/file/" + dl));
                }
            }
        }

        if (decryptedLinks.size() > 0) {
            fp.addLinks(decryptedLinks);
            return decryptedLinks;
        } else {
            return null;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}