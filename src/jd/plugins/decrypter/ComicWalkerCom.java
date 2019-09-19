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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "comic-walker.com" }, urls = { "https?://(www\\.)?comic-walker\\.com/(viewer|contents/detail)/.+" })
public class ComicWalkerCom extends antiDDoSForDecrypt {
    public ComicWalkerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>([^<]+)\\s+-\\s+無料コミック\\s+ComicWalker").getMatch(0);
        if (parameter.contains("/contents/detail/")) {
            String[] links = br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]*/viewer/[^\"]+)\"").getColumn(0);
            if (links != null && links.length > 0) {
                for (String link : links) {
                    decryptedLinks.add(createDownloadlink(br.getURL(Encoding.htmlDecode(link)).toString()));
                }
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        } else if (parameter.contains("/viewer/")) {
            String[] apiData = br.getRegex("data-api-endpoint-url\\s*=\\s*\"([^\"]+)\"[^>]+data-episode-id\\s*=\\s*\"([^\"]+)\"").getRow(0);
            if (apiData == null || apiData.length < 2) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "No images found");
            }
            final String apiURL = Encoding.htmlDecode(apiData[0]) + "/api/v1/comicwalker/episodes/" + Encoding.htmlDecode(apiData[1]) + "/frames";
            final Browser br2 = br.cloneBrowser();
            getPage(br2, apiURL);
            final String[] images = br2.getRegex("\\s*\"source_url\"\\s*:\\s*\"([^\"]+)\"").getColumn(0);
            final int padlength = getPadLength(images.length);
            final FilePackage fp = FilePackage.getInstance();
            String ext = null;
            int page = 1;
            for (String image : images) {
                String imageURL = "directhttp://" + image.replace("\\", "");
                String page_formatted = String.format(Locale.US, "%0" + padlength + "d", page);
                if (ext == null) {
                    /* No general extension given? Get it from inside the URL. */
                    ext = getFileNameExtensionFromURL(imageURL, ".jpg");
                }
                String filename = "" + Encoding.htmlDecode(fpName.trim()) + "_" + page_formatted + ext;
                DownloadLink dl = createDownloadlink(imageURL);
                dl._setFilePackage(fp);
                dl.setFinalFileName(filename);
                dl.setLinkID(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                distribute(dl);
                page++;
            }
        }
        return decryptedLinks;
    }

    private final int getPadLength(final int size) {
        return String.valueOf(size).length();
    }
}