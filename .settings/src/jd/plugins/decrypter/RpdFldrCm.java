//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 16426 $", interfaceVersion = 2, names = { "rapidfolder.com" }, urls = { "http://[\\w\\.]*?rapidfolder\\.com/\\??[\\w\\-]+" }, flags = { 0 })
public class RpdFldrCm extends PluginForDecrypt {

    // DEV NOTES
    // has redirects from old link type to new.
    // all links are present within page
    // no folder name/title only shown as 'Title: No information.'

    public RpdFldrCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie("http://rapidfolder.com", "lang", "english");
        br.setFollowRedirects(true);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        prepBrowser();
        br.getPage(parameter);
        if (!br.containsHTML(">Choose the files you want to download:<")) {
            logger.warning("Invalid URL: " + parameter);
            return null;
        }
        String[] links = br.getRegex("window\\.open\\(\\'(https?[^\\']+)").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Possible Plugin error, with finding links: " + parameter);
            return null;
        }
        if (links != null && links.length != 0) {
            for (String link : links)
                decryptedLinks.add(createDownloadlink(link));
        }
        return decryptedLinks;
    }
}
