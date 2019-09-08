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
import java.util.Arrays;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision: 41221 $", interfaceVersion = 3, names = { "vidcloud.icu" }, urls = { "https?://(www\\d*\\.)?(?:vidcloud\\.icu|k-vid\\.net)/(?:videos/|streaming\\.php\\?|download\\?).+" })
public class VidCloudIcu extends antiDDoSForDecrypt {
    public VidCloudIcu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>Watch\\s+([^\"]+)(?:\\sat Vidcloud)").getMatch(0);
        String[] links = null;
        if (parameter.contains("streaming.php")) {
            String[] activeLinks = br.getRegex("(?:file: \"|window.urlVideo = ')([^\"']+)[\"\']").getColumn(0);
            String[] otherLinks = br.getRegex("<li class=\"linkserver\" data-status=\"1\" data-video=\"([^\"]+)\"").getColumn(0);
            ArrayList list = new ArrayList(Arrays.asList(activeLinks));
            list.addAll(Arrays.asList(otherLinks));
            links = (String[]) list.toArray(new String[list.size()]);
        } else if (new Regex(parameter, "/download\\?.+").matches()) {
            String linkBlock = br.getRegex("<div class=\"content_c\">([^$]+)<footer>").getMatch(0);
            links = HTMLParser.getHttpLinks(linkBlock, null);
        } else {
            links = br.getRegex("<div class=\"play-video\">\\s+<iframe src=\"([^\"]+)\"").getColumn(0);
        }
        if (links != null && links.length > 0) {
            for (String link : links) {
                if (link.startsWith("//")) {
                    link = br.getURL(link).toString();
                }
                decryptedLinks.add(createDownloadlink(link));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.setProperty("ALLOW_MERGE", true);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}