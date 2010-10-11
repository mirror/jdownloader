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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagefap.com" }, urls = { "http://[\\w\\.]*?imagefap\\.com/(gallery\\.php\\?gid=.+|gallery/.+|pictures/\\d+/.{1})" }, flags = { 0 })
public class MgfpCm extends PluginForDecrypt {

    public MgfpCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replaceAll("view\\=[0-9]+", "view=2");
        if (!parameter.contains("view=2")) parameter += "&view=2";
        br.getPage(parameter);
        String galleryName = br.getRegex("<title>Porn pics of (.*?) \\(Page 1\\)</title>").getMatch(0);
        if (galleryName == null) {
            galleryName = br.getRegex("<font face=\"verdana\" color=\"white\" size=\"4\"><b>(.*?)</b></font>").getMatch(0);
            if (galleryName == null) galleryName = br.getRegex("<meta name=\"description\" content=\"Airplanes porn pics - Imagefap\\.com\\. The ultimate social porn pics site\" />").getMatch(0);
        }
        if (galleryName == null) {
            logger.warning("Gallery name could not be found!");
            return null;
        }
        String thisID = new Regex(parameter, "imagefap\\.com/pictures/(\\d+)/").getMatch(0);
        int pages = 0;
        if (thisID != null) {
            br.getPage("http://www.imagefap.com/pictures/" + thisID + "/bla?pgid=&gid=" + thisID + "&page=0&view=0");
            String[] allpages = br.getRegex("<a class=link3 href=\"\\?pgid=\\&amp;gid=\\d+\\&amp;page=(\\d+)\\&amp;").getColumn(0);
            if (allpages != null && allpages.length != 0) {
                for (String pageText : allpages) {
                    if (Integer.parseInt(pageText) > pages) pages = Integer.parseInt(pageText);
                }
            }
            if (pages > 1) logger.info("Found " + (pages + 1) + " pages, starting to decrypt...");
        }
        progress.setRange(pages);
        double counter = 0.001;
        for (int i = 0; i <= pages; i++) {
            if (pages > 1) {
                logger.info("Decrypting page " + i);
                br.getPage("http://www.imagefap.com/pictures/" + thisID + "/bla?pgid=&gid=" + thisID + "&page=" + i + "&view=0");
            }
            String links[] = br.getRegex("<a name=\"\\d+\" href=\"/image\\.php\\?id=(\\d+)\\&").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String element : links) {
                DownloadLink link = createDownloadlink("http://imagefap.com/image.php?id=" + element);
                link.setProperty("orderid", new Regex(String.format("&orderid=%.3f&", counter), "\\&orderid=0\\.(\\d+)").getMatch(0));
                link.setProperty("galleryname", galleryName);
                decryptedLinks.add(link);
                counter += 0.001;
            }
            progress.increase(1);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(galleryName.trim());
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
