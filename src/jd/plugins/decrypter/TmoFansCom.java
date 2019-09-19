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
import java.util.Locale;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tmofans.com" }, urls = { "https?://(?:www\\.)?tmofans\\.com/(?:library/[^/]+/\\d+|viewer/\\w+)/.+" })
public class TmoFansCom extends antiDDoSForDecrypt {
    public TmoFansCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        if (parameter.endsWith("/paginated")) {
            parameter = parameter.replace("/paginated", "/cascade");
        }
        getPage(parameter);
        final String fpName = br.getRegex("<title>\\s*([^<]+)\\s+-\\s+TuMangaOnline").getMatch(0);
        String[] downloadLinks = br.getRegex("<a[^>]*href\\s*=\\s*\"([^\"]+)\"[^>]*><i[^>]*class\\s*=\\s*\"[^\"]*fa-cloud-download-alt[^\"]*\"[^>]*>").getColumn(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        if (downloadLinks != null && downloadLinks.length > 0) {
            for (String link : downloadLinks) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
            }
        }
        String[] readLinks = br.getRegex("<a[^>]*href\\s*=\\s*\"([^\"]+)\"[^>]*>\\s*<i[^>]*class\\s*=\\s*\"[^\"]*fa-play[^\"]*\"[^>]*>").getColumn(0);
        if (readLinks != null && readLinks.length > 0) {
            final Browser br2 = br.cloneBrowser();
            fp.setProperty("ALLOW_MERGE", true);
            for (String link : readLinks) {
                link = Encoding.htmlDecode(link);
                if (link.contains("/goto/")) {
                    getPage(br2, link);
                    link = br2.getRedirectLocation() == null ? br2.getURL() : br2.getRedirectLocation();
                }
                if (link.endsWith("/paginated")) {
                    link = link.replace("/paginated", "/cascade");
                }
                DownloadLink dl = createDownloadlink(link);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
            }
        }
        String[] imageLinks = br.getRegex("<img[^>]*src\\s*=\\s*\"([^\"]+)\"[^>]+class\\s*=\\s*\"[^\"]*viewer-image[^\"]*\"[^>]*>").getColumn(0);
        if (imageLinks != null && imageLinks.length > 0) {
            final String imageNumberFormat = "%0" + getPadLength(imageLinks.length) + "d";
            int page = 1;
            String ext = null;
            for (String image : imageLinks) {
                String page_formatted = String.format(Locale.US, imageNumberFormat, page);
                if (ext == null) {
                    /* No general extension given? Get it from inside the URL. */
                    ext = getFileNameExtensionFromURL(image, ".jpg");
                }
                String filename = fpName.toString() + "_" + page_formatted + ext;
                DownloadLink dl = createDownloadlink(image);
                dl._setFilePackage(fp);
                dl.setFinalFileName(filename);
                dl.setLinkID(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                distribute(dl);
                page++;
            }
        }
        if (decryptedLinks.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "No links/images found");
        }
        return decryptedLinks;
    }

    private final int getPadLength(final int size) {
        return String.valueOf(size).length();
    }
}
