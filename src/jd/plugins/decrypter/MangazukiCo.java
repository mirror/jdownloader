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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mangazuki.co" }, urls = { "https?://(?:www\\.|raws\\.)?mangazuki\\.co/(?:read|manga)/[^/]+/\\d+(?:\\.\\d+)?" })
public class MangazukiCo extends antiDDoSForDecrypt {
    public MangazukiCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: MangaPictureCrawler */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter + "/1");
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains("/manga")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpname = br.getRegex("<title>\\s*(.*?) - Page \\d+\\s*</title>").getMatch(0);
        final Regex urlinfo = new Regex(parameter, "/manga/([^/]+)/(.+)");
        final String url_name = urlinfo.getMatch(0);
        final String url_chapter = urlinfo.getMatch(1);
        String ext = null;
        final FilePackage fp = FilePackage.getInstance();
        if (fpname != null) {
            fp.setName(fpname);
        } else {
            fp.setName(url_name + "_Chapter_" + url_chapter);
        }
        final String[] images = br.getRegex("<img .*?data-src=' (https?://[^<>']+) '").getColumn(0);
        if (images == null || images.length == 0) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final int padLength = StringUtils.getPadLength(images.length);
        int page = 1;
        for (final String url_image : images) {
            if (this.isAbort()) {
                return decryptedLinks;
            }
            final String page_formatted = String.format(Locale.US, "%0" + padLength + "d", page);
            if (ext == null) {
                /* No general extension given? Get it from inside the URL. */
                ext = getFileNameExtensionFromURL(url_image, ".jpg");
            }
            String filename = null;
            if (fpname != null) {
                filename = fpname + "_" + page_formatted + ext;
            } else {
                filename = url_name + "_Chapter_" + url_chapter + "_" + page_formatted + ext;
            }
            DownloadLink dl = createDownloadlink(url_image);
            dl._setFilePackage(fp);
            dl.setFinalFileName(filename);
            // dl.setContentUrl(page_url);
            dl.setLinkID(filename);
            dl.setAvailable(true);
            // logger.info("Debug info: Creating: " + url_image);
            decryptedLinks.add(dl);
            distribute(dl);
            page++;
        }
        return decryptedLinks;
    }
}
