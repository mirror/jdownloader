//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sendspace.com" }, urls = { "https?://(?:www\\.)?sendspace\\.com/(?:folder/[0-9a-zA-Z]+|filegroup/([0-9a-zA-Z%]+))" })
public class SendspaceComFolder extends PluginForDecrypt {

    public SendspaceComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.getPage(parameter);
        if (br.containsHTML("(404 Page Not Found|It has either been moved)")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (parameter.contains("/filegroup/")) {
            // return pro or file links
            final String[] results = br.getRegex("<div class=\"groupedFile\">.*?</div>\\s*</div>").getColumn(-1);
            if (results != null && results.length > 0) {
                for (final String result : results) {
                    final Regex fs = new Regex(result, "<h4><b>(.*?)</b>\\s*(.*?)</h4>");
                    final String filename = fs.getMatch(0);
                    final String filesize = fs.getMatch(1);
                    final String url = new Regex(result, "<a [^>]*href=(\"|'|)(.*?)\\1").getMatch(1);
                    final DownloadLink dl = createDownloadlink(Request.getLocation(url, br.getRequest()));
                    dl.setName(Encoding.htmlDecode(filename));
                    dl.setDownloadSize(SizeFormatter.getSize(filesize.trim().replaceAll(",", ".")));
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                }
                final String fpName = "Multiple Downloads " + JDHash.getCRC32(Encoding.urlDecode(new Regex(parameter, this.getSupportedLinks()).getMatch(0), false));
                if (fpName != null) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName.trim());
                    fp.addLinks(decryptedLinks);
                }
            }
        } else {
            // folder
            String[] files = br.getRegex("<td class=\"dl\" nowrap><a href=\"(.*?)\" title=").getColumn(0);
            for (String file : files) {
                decryptedLinks.add(createDownloadlink(file));
            }
        }
        final String fpName = br.getRegex("Folder: <b>(.*?)</b>").getMatch(0);

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}