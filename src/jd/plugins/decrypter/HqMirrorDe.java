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
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hq-mirror.de" }, urls = { "https?://(?:www\\.)?hq\\-mirror\\.de/tvprogram/show/(\\d+)" })
public class HqMirrorDe extends PluginForDecrypt {
    public HqMirrorDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String showID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        String fpName = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)\" />").getMatch(0);
        if (fpName == null) {
            fpName = showID;
        }
        final String[] htmls = getHtmls(this.br);
        if (htmls == null || htmls.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleHTML : htmls) {
            final String filename = new Regex(singleHTML, "<td[^<>]*?title=\"([^<>\"]+)").getMatch(0);
            final String filesize = new Regex(singleHTML, ">(\\d+(?:,\\d{1,2})? [A-Za-z]{2,5})<").getMatch(0);
            final String linkID = new Regex(singleHTML, "#collapse\\-link\\-copy\\-(\\d+)").getMatch(0);
            if (linkID == null) {
                return null;
            }
            final DownloadLink dl = createDownloadlink(String.format("https://www.hq-mirror.de/tvprogram/show/%s#collapse-link-copy-%s", showID, linkID));
            dl.setAvailable(true);
            if (filename != null) {
                dl.setName(filename);
            }
            if (filesize != null) {
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            dl.setLinkID(linkID);
            dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    public static final String[] getHtmls(final Browser br) {
        return br.getRegex("<table class=\"fixed\\-table\">.*?</div>\\s*?</td>\\s*?</tr>").getColumn(-1);
    }

    public static final boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }
}
