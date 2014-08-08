//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision: 24808 $", interfaceVersion = 2, names = { "xxxfile.to" }, urls = { "http://(www\\.)?xxxfile\\.to/download/clips/[a-z0-9\\-]+\\.html" }, flags = { 0 })
public class XxxFileTo extends PluginForDecrypt {

    /**
     * @author ohgod
     * @author raztoki
     * */
    public XxxFileTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        br.getPage(parameter);
        br.setFollowRedirects(true);

        final String content = br.getRegex("<div class=\"news-text clearfix\">(.*?)<div align=\"center\">").getMatch(0);

        if (content == null) {
            return null;
        }

        // test zum extracten des releasenames

        final String packageName = new Regex(content, "alt=\"(.*?)\"").getMatch(0);

        final String[] links = new Regex(content, "<a href=\"(https?://.*?)\"").getColumn(0);

        if (links == null || links.length == 0) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        for (final String link : links) {
            if (!link.matches("https?://(www\\.)?xxxfile\\.to/.+")) {
                decryptedLinks.add(createDownloadlink(link));
            }
        }

        final String[] imgs = br.getRegex("(https?://([\\w\\.]+)?pixhost\\.org/show/[^\"]+)").getColumn(0);
        if (links != null && links.length != 0) {
            for (final String img : imgs) {
                decryptedLinks.add(createDownloadlink(img));
            }
        }

        if (packageName != null) {
            FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(packageName);
            filePackage.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}