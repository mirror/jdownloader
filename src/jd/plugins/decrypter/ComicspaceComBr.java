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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "comicspace.com.br" }, urls = { "https?://(?:www\\.)?comicspace\\.com\\.br/manga/[a-z0-9\\-]+/\\d+" })
public class ComicspaceComBr extends antiDDoSForDecrypt {
    public ComicspaceComBr(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: MangaPictureCrawler */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String extension_fallback = ".png";
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 500 });
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Regex urlinfo = new Regex(parameter, "/manga/([a-z0-9\\-]+)/(\\d+)");
        final String url_chapter = urlinfo.getMatch(1);
        final String url_name = urlinfo.getMatch(0);
        String ext = null;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(url_chapter + "_" + url_name);
        final String[] images = this.br.getRegex("class=\"img\\-responsive\"[^>]*?data\\-src=(?:\"|\\')([^<>\"\\']+)(?:\"|\\')").getColumn(0);
        if (images == null || images.length == 0) {
            return null;
        }
        final int padLength = getPadLength(images.length);
        int page = 1;
        for (String finallink : images) {
            if (this.isAbort()) {
                return decryptedLinks;
            }
            finallink = finallink.trim();
            final String page_formatted = String.format(Locale.US, "%0" + padLength + "d", page);
            ext = getFileNameExtensionFromURL(finallink, extension_fallback);
            if (ext == null) {
                ext = extension_fallback;
            }
            final String filename = url_chapter + "_" + url_name + "_" + page_formatted + ext;
            final DownloadLink dl = this.createDownloadlink(finallink);
            dl._setFilePackage(fp);
            dl.setFinalFileName(filename);
            // dl.setContentUrl(page_url);
            dl.setLinkID(filename);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            distribute(dl);
            page++;
        }
        return decryptedLinks;
    }

    private final int getPadLength(final int size) {
        if (size < 10) {
            return 1;
        } else if (size < 100) {
            return 2;
        } else if (size < 1000) {
            return 3;
        } else if (size < 10000) {
            return 4;
        } else if (size < 100000) {
            return 5;
        } else if (size < 1000000) {
            return 6;
        } else if (size < 10000000) {
            return 7;
        } else {
            return 8;
        }
    }
}
