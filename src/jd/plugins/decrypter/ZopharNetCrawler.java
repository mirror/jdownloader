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
import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "roms.zophar.net" }, urls = { "https?://(?:www\\.)?zophar\\.net/(?!download_file)[^<>\"/]*?/[^<>\"/]*?(/[^<>\"/]+)?\\.html" })
public class ZopharNetCrawler extends PluginForDecrypt {
    static private final Pattern patternDownload = Pattern.compile("\"(https?://[^/]+/download_file/\\d+)\"", Pattern.CASE_INSENSITIVE);

    public ZopharNetCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(param.toString());
        if (!this.canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String file = br.getRegex(patternDownload).getMatch(0);
        if (file == null) {
            if (!br.containsHTML("class=\"tcat\"")) {
                logger.info("Link offline (unsupported linktype): " + parameter);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final String size = br.getRegex("<b>Filesize</b></td>.*?<td align=\"right\">(\\d+(\\.\\d+)? (KB|MB|B))</td>").getMatch(0);
        final DownloadLink dlLink = createDownloadlink(file);
        if (size != null) {
            dlLink.setDownloadSize(SizeFormatter.getSize(size));
        }
        dlLink.setName(new Regex(parameter, "/([^<>\"/]*?)\\.html").getMatch(0));
        decryptedLinks.add(dlLink);
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}