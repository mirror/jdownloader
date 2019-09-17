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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "psychoplay.co" }, urls = { "https?://(?:www\\.)?psychoplay\\.co/comics/[^/]+(?:/\\d+/\\d+)?" })
public class PsychoPlay extends antiDDoSForDecrypt {
    public PsychoPlay(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        final String url_name = new Regex(parameter, "/comics/([^/]+)").getMatch(0);
        final String url_volume = new Regex(parameter, "/comics/[^/]+/(\\d+)").getMatch(0);
        final String url_chapter = new Regex(parameter, "/comics/[^/]+/\\d+/(\\d+)").getMatch(0);
        final String fpName = br.getRegex("<title>([^<]+)").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setProperty("ALLOW_MERGE", true);
        if (url_chapter == null) {
            String[] links = br.getRegex("<a href=\"([^\"]+/" + Pattern.quote(url_name) + "/[^\"]+)\" class=\"item-author[^\"]+\">").getColumn(0);
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
            final String[] images = br.getRegex("\"public_path\":\"([^\"]+)\"").getColumn(0);
            final String url_volume_formatted = String.format(Locale.US, "%03d", Integer.parseInt(url_volume));
            final String url_chapter_formatted = String.format(Locale.US, "%03d", Integer.parseInt(url_chapter));
            if (fpName != null) {
                fp.setName(fpName);
            } else {
                fp.setName(url_name + "_Volume_" + url_volume_formatted + "_Chapter_" + url_chapter_formatted);
            }
            final int padlength = getPadLength(images.length);
            String ext = null;
            int page = 1;
            for (String image : images) {
                image = image.replace("\\", "");
                String page_formatted = String.format(Locale.US, "%0" + padlength + "d", page);
                if (ext == null) {
                    /* No general extension given? Get it from inside the URL. */
                    ext = getFileNameExtensionFromURL(image, ".jpg");
                }
                String filename = url_name + "_" + url_volume_formatted + "_" + url_chapter_formatted + "_" + page_formatted + ext;
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