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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hellspy.cz" }, urls = { "https?://(?:www\\.|porn\\.)?hellspy\\.(?:cz|com|sk)/(?:soutez/|sutaz/)?[a-z0-9\\-]+/\\d+" })
public class HellspyCzFolder extends PluginForDecrypt {
    public HellspyCzFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        /* Check if there is the possibility that we have a multipart rar archive with single downloadable parts */
        if (!new Regex(parameter, "[^<>\"]*pa?r?t?.?[0-9]+\\-rar").matches()) {
            decryptedLinks.add(this.createDownloadlink(parameter));
            return decryptedLinks;
        }
        setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getHeaders().put("Accept-Language", "en-gb;q=0.9, en;q=0.8");
        br.setFollowRedirects(true);
        this.br.getPage(parameter);
        if (jd.plugins.hoster.HellSpyCz.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String[][] hits = br.getRegex("<tr>\\s*<th>\\s*([^<>\\\"]*)\\s*</th>.*?" + "snippet-relatedDownloadControl-[0-9]+-download\">\\s*" + "<a href=\"([^<>\"]*pa?r?t?.?[0-9]+-rar[^<>\"]*-download)\".*?" + "</td>\\s*<th>([^<>\"]*)</th>\\s*</tr>").getMatches();
        for (String[] p1 : hits) {
            final String url = "https://www.hellspy.cz" + Encoding.htmlDecode(p1[1]);
            final DownloadLink link = this.createDownloadlink(url);
            // link.setArchiveID("");
            link.setFinalFileName(p1[0]);
            link.setDownloadSize(SizeFormatter.getSize(p1[2]));
            link.setAvailable(true);
            decryptedLinks.add(link);
        }
        return decryptedLinks;
    }
}
