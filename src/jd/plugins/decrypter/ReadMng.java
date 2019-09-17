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
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "readmng.com" }, urls = { "https?://(?:www\\.)?readmng\\.com/[^/]+(?:/\\d+/)?" })
public class ReadMng extends antiDDoSForDecrypt {
    public ReadMng(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        if (new Regex(parameter, "https?://(?:www\\.)?readmng\\.com/[^/]+/[^/]+").matches()) {
            parameter = new Regex(parameter, "(https?://(?:www\\.)?readmng\\.com/[^/]+/\\d+)").getMatch(0) + "/all-pages";
        }
        getPage(parameter);
        final String url_name = new Regex(parameter, "https?://(?:www\\.)?readmng\\.com/([^/]+)").getMatch(0);
        final String url_chapter = new Regex(parameter, "https?://(?:www\\.)?readmng\\.com/[^/]+/(\\d+)").getMatch(0);
        final String fpName = br.getRegex("<title>(?:Read\\s+)?([^<]+)\\s+-\\s+Read").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setProperty("ALLOW_MERGE", true);
        if (url_chapter == null) {
            String[] links = br.getRegex("<li>\\s*<a href=\"([^\"]+/" + Pattern.quote(url_name) + "/[^\"]+)").getColumn(0);
            if (links != null && links.length > 0) {
                for (String link : links) {
                    decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
                }
            }
            if (fpName != null) {
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        } else {
            final String[] images = br.getRegex("<img[^>]+src=\"([^\"]+)\"[^>]+class=\"img-responsive\"[^>]*>").getColumn(0);
            final String[] chapters = br.getRegex(" <option value=\"([^\"]+" + Pattern.quote(url_name) + "/\\d+[^\"]*)\"").getColumn(0);
            final String url_chapter_formatted = String.format(Locale.US, "%0" + getPadLength(chapters.length) + "d", Integer.parseInt(url_chapter));
            if (fpName != null) {
                fp.setName(fpName);
            } else {
                fp.setName(url_name + "_Chapter_" + url_chapter_formatted);
            }
            final int padlength = getPadLength(images.length);
            String ext = null;
            int page = 1;
            for (String image : images) {
                String page_formatted = String.format(Locale.US, "%0" + padlength + "d", page);
                if (ext == null) {
                    /* No general extension given? Get it from inside the URL. */
                    ext = getFileNameExtensionFromURL(image, ".jpg");
                }
                String filename = url_name + "_" + url_chapter_formatted + "_" + page_formatted + ext;
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
        return decryptedLinks;
    }

    private final int getPadLength(final int size) {
        return String.valueOf(size).length();
    }
}