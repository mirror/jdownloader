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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hentairead.com" }, urls = { "https?://(?:www\\.)?hentairead.com/hentai/([^/?]+)/?" })
public class HentaiReadCom extends PluginForDecrypt {
    public HentaiReadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        String url = param.getCryptedUrl().replaceFirst("(?i)^http://", "https://");
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += "1/";
        br.getPage(url);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String readNowURL = br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]+class\\s*=\\s*\"[^\"]*btn-read-now").getMatch(0);
        if (readNowURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(readNowURL);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String pageListText = br.getRegex("<select[^>]+id\\s*=\\s*\\\"single-pager\\\"[^>]+>([^$]+)</select>").getMatch(0);
        if (pageListText.contains("</select>")) {
            pageListText = pageListText.substring(0, pageListText.indexOf("</select>"));
        }
        String title = br.getRegex("property\\s*=\\s*\"og:title\"[^>]+content\\s*=\\s*\"([^\"]+)\\s+-\\s+Read hentai Doujinshi online for free").getMatch(0);
        if (StringUtils.isEmpty(title)) {
            title = br.getRegex("property\\s*=\\s*\"og:title\"[^>]+content\\s*=\\s*\\\"(?:Reading\\s+)?([^\\\"]+)\\s+-\\s+[\\w\\s]+online for free").getMatch(0);
        }
        final String urlSlug = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setAllowMerge(true);
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
        } else {
            /* Fallback */
            title = urlSlug.replace("-", " ").trim();
        }
        fp.setName(title);
        /* Similar to porncomixinfo.net */
        final String imagesJsonArrayText = br.getRegex("chapter_preloaded_images = (\\[[^\\]]+\\])").getMatch(0);
        if (imagesJsonArrayText == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final List<Map<String, Object>> imagesmaps = (List<Map<String, Object>>) restoreFromString(imagesJsonArrayText, TypeRef.OBJECT);
        int page = 1;
        final int padLength = imagesmaps.size();
        for (final Map<String, Object> imagesmap : imagesmaps) {
            final String imageurl = imagesmap.get("src").toString();
            final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(imageurl));
            String assumedContenturl = br._getURL().getPath().replaceFirst("/english/p/\\d+", "");
            if (!assumedContenturl.endsWith("/")) {
                assumedContenturl += "/";
            }
            assumedContenturl += "english/p/" + page;
            if (br.containsHTML(Pattern.quote(assumedContenturl))) {
                /* St nice URL for user when he uses "open in browser" action. */
                dl.setContentUrl(br.getURL(assumedContenturl).toExternalForm());
            }
            final String extension = getFileNameExtensionFromURL(imageurl);
            String filename = title + "_" + StringUtils.formatByPadLength(padLength, page) + extension;
            dl.setFinalFileName(filename);
            dl.setAvailable(true);
            dl._setFilePackage(fp);
            ret.add(dl);
            page++;
        }
        return ret;
    }
}