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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "readallcomics.com" }, urls = { "https?://(?:www\\.)?readallcomics\\.com/(?:category/)?[^/]+/?" })
public class ReadAllComics extends antiDDoSForDecrypt {
    public ReadAllComics(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        br.setFollowRedirects(true);
        getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("<title>\\s*([^<]+)\\s+&#124;\\s+Read\\s+All\\s+Comics\\s+Online").getMatch(0);
        String itemName = new Regex(contenturl, "/(?:category/)?([^/]+)/?").getMatch(0);
        fpName = (StringUtils.isEmpty(fpName) ? Encoding.htmlDecode(itemName) : Encoding.htmlDecode(fpName)).replaceAll("…", "").trim();
        final FilePackage fp = FilePackage.getInstance();
        if (StringUtils.containsIgnoreCase(contenturl, "/category/")) {
            String linkSection = br.getRegex("<ul class=\"list-story\">([^$]+)</ul>").getMatch(0);
            String[] chapters = new Regex(linkSection, "href=[\"\']([^\"\']+)[\"\']").getColumn(0);
            if (chapters != null && chapters.length > 0) {
                if (StringUtils.isNotEmpty(fpName)) {
                    fp.setName(Encoding.htmlDecode(fpName).trim());
                    fp.setAllowMerge(true);
                    fp.setAllowInheritance(true);
                }
                for (String chapter : chapters) {
                    final DownloadLink dl = createDownloadlink(Encoding.htmlOnlyDecode(chapter));
                    fp.add(dl);
                    distribute(dl);
                    ret.add(dl);
                }
            }
        } else {
            String linkSection = br.getRegex("<div[^>]+data-wpusb-component\\s*=\\s*\"[^\"]*buttons-section[^\"]*\"[^>]*>([^$]+)<div[^>]+data-wpusb-component\\s*=\\s*\"[^\"]*buttons-section[^\"]*\"[^>]*>").getMatch(0);
            if (linkSection == null) {
                linkSection = br.getRegex("name\\s*=\\s*\"IL_IN_ARTICLE\"(.*?)name\\s*=\\s*\"IL_IN_ARTICLE\"").getMatch(0);
            }
            String[] images = br.getRegex("<img[^<]*src=\"(https?://[^\"]+\\d{2,}\\.jpg)").getColumn(0);
            if (images == null || images.length == 0) {
                /* 2nd type */
                images = br.getRegex("src=\"(https?://[^\"]+)\"\s*alt=\"[^\"]* Page \\d+\"").getColumn(0);
                if (images == null || images.length == 0) {
                    /* 3rd type / wider attempt */
                    images = br.getRegex("<img[^<]*src=\"(https?://[^\"]+)").getColumn(0);
                }
            }
            if (images == null || images.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            fp.setName(Encoding.htmlDecode(fpName));
            final int padlength = StringUtils.getPadLength(images.length);
            int page = 1;
            for (String image : images) {
                String page_formatted = String.format(Locale.US, "%0" + padlength + "d", page++);
                image = Encoding.htmlOnlyDecode(image);
                final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(image));
                String ext = getFileNameExtensionFromURL(image, ".jpg");
                dl.setFinalFileName(fpName + "_" + page_formatted + ext);
                dl.setAvailable(true);
                fp.add(dl);
                distribute(dl);
                ret.add(dl);
            }
        }
        return ret;
    }
}