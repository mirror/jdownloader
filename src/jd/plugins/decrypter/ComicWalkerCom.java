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

import org.appwork.utils.StringUtils;
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
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "comic-walker.com" }, urls = { "https?://(www\\.)?comic-walker\\.com/(viewer|contents/detail)/.+" })
public class ComicWalkerCom extends antiDDoSForDecrypt {
    public ComicWalkerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("<title>([^<]+)\\s+-\\s+無料コミック\\s+ComicWalker").getMatch(0);
        if (param.getCryptedUrl().contains("/contents/detail/")) {
            final String[] links = br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]*/viewer/[^\"]+)\"").getColumn(0);
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
        } else if (param.getCryptedUrl().contains("/viewer/")) {
            final String apiBaseURL = br.getRegex("data-api-endpoint-urls?\\s*=\\s*\\'\\{\"nc\":\"(https?://[^\"]+)\"").getMatch(0);
            final String episode_id = PluginJSonUtils.getJson(br, "episode_id");
            if (apiBaseURL == null || episode_id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "No images found");
            }
            final FilePackage fp = FilePackage.getInstance();
            if (fpName != null) {
                fp.setName(fpName);
            } else {
                /* Fallback */
                fp.setName(episode_id);
            }
            final String apiURL = apiBaseURL + "/api/v1/comicwalker/episodes/" + Encoding.htmlDecode(episode_id) + "/frames";
            final Browser br2 = br.cloneBrowser();
            getPage(br2, apiURL);
            final String[] images = br2.getRegex("\\s*\"source_url\"\\s*:\\s*\"([^\"]+)\"").getColumn(0);
            final int padlength = StringUtils.getPadLength(images.length);
            String ext = null;
            int page = 1;
            for (String image : images) {
                String imageURL = "directhttp://" + image.replace("\\", "");
                String page_formatted = String.format(Locale.US, "%0" + padlength + "d", page);
                if (ext == null) {
                    /* No general extension given? Get it from inside the URL. */
                    ext = getFileNameExtensionFromURL(imageURL, ".jpg");
                }
                final String filename = "" + Encoding.htmlDecode(fpName.trim()) + "_" + page_formatted + ext;
                final DownloadLink dl = createDownloadlink(imageURL);
                dl._setFilePackage(fp);
                dl.setFinalFileName(filename);
                dl.setLinkID(this.getHost() + "://" + filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                distribute(dl);
                page++;
            }
        }
        return decryptedLinks;
    }
}