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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hentairead.com" }, urls = { "https?://(?:www\\.)?hentairead.com/hentai/([^/?]+)/?" })
public class HentaiReadCom extends PluginForDecrypt {
    public HentaiReadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String url = param.getCryptedUrl().replaceFirst("http:", "https:");
        br.getPage(url + "/1/");
        br.getPage(br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]+class\\s*=\\s*\"[^\"]*btn-read-now").getMatch(0));
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
        String imagesText = br.getRegex("chapter_preloaded_images = \\[(.*?)\\]").getMatch(0);
        imagesText = PluginJSonUtils.unescape(imagesText);
        imagesText = imagesText.replace("\"", "");
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int pageNumber = 1;
        final String[] images = imagesText.split(",");
        final int padLength = StringUtils.getPadLength(images.length);
        for (final String imageurl : images) {
            final DownloadLink dl = createDownloadlink("directhttp://" + imageurl);
            final String extension = getFileNameExtensionFromURL(imageurl);
            String filename = title + "_" + StringUtils.formatByPadLength(padLength, pageNumber) + extension;
            dl.setFinalFileName(filename);
            dl.setAvailable(true);
            dl._setFilePackage(fp);
            distribute(dl);
            ret.add(dl);
            pageNumber++;
        }
        return ret;
    }
}