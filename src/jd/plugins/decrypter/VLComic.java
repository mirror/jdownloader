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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vlcomic.com" }, urls = { "https?://(www\\.)?vlcomic\\.com/read/[^/\\s]+(?:/[^/\\s]+)?" })
public class VLComic extends antiDDoSForDecrypt {
    public VLComic(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        final String fpName = br.getRegex("<title>\\s*(?:Comic\\s+)?([^<]+)\\s*<").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (StringUtils.isNotEmpty(fpName)) {
            fp.setName(Encoding.htmlDecode(fpName.trim()));
        }
        final String[] links = br.getRegex("<a[^>]+class\\s*=\\s*\"ch-name\"[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]*>").getColumn(0);
        if (links != null && links.length > 0) {
            for (String link : links) {
                link = Encoding.htmlDecode(link).trim();
                if (link.startsWith("/") && !link.startsWith("//")) {
                    link = br.getURL(link).toString();
                }
                DownloadLink dl = createDownloadlink(link);
                fp.add(dl);
                decryptedLinks.add(dl);
            }
        }
        String imageBlock = br.getRegex("<div class='comics'>\\s*([^§]+)\\s*</div>").getMatch(0);
        if (StringUtils.isEmpty(imageBlock)) {
            imageBlock = br.getRegex("<div[^>]+class\\s*=\\s*\"chapter-main\"[^>]*>([^§]+)<!--\\s*Composite\\s*Start\\s*-->").getMatch(0);
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final String[] images = new Regex(imageBlock, "<img[^>]+src\\s*=\\s*\"([^\"]+)\"[^>]*>").getColumn(0);
            if (images == null || images.length == 0) {
                String link = new Regex(imageBlock, "<a[^>]+href\\s*=\\s*[\\\"']((?!#)[^\\\"']+)[\\\"'][^>]*>").getMatch(0);
                if (StringUtils.isNotEmpty(link)) {
                    link = Encoding.htmlDecode(link).trim();
                    if (link.startsWith("/") && !link.startsWith("//")) {
                        link = br.getURL(link).toString();
                    }
                    DownloadLink dl = createDownloadlink(link);
                    decryptedLinks.add(dl);
                }
            }
            if (images != null && images.length > 0) {
                if (StringUtils.isEmpty(fpName)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String chapter_name = Encoding.htmlDecode(fpName.trim());
                final int padlength = getPadLength(images.length);
                int page = 1;
                String ext = null;
                for (String image : images) {
                    String page_formatted = String.format(Locale.US, "%0" + padlength + "d", page);
                    image = Encoding.htmlDecode(image);
                    DownloadLink dl = createDownloadlink("directhttp://" + image);
                    if (ext == null) {
                        ext = getFileNameExtensionFromURL(image, ".jpg");
                    }
                    dl.setFinalFileName(chapter_name + "_" + page_formatted + ext);
                    fp.add(dl);
                    distribute(dl);
                    decryptedLinks.add(dl);
                    page++;
                }
            }
        }
        return decryptedLinks;
    }

    private int getPadLength(final int size) {
        return String.valueOf(size).length();
    }
}