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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "lhtranslation.net" }, urls = { "https?://(?:www\\.)?lhtranslation\\.(?:com|net)/read\\-([a-z0-9\\-]+)\\-chapter\\-(\\d+)\\.html" })
public class Lhtranslation extends antiDDoSForDecrypt {
    public Lhtranslation(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: MangaPictureCrawler */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String extension_fallback = ".jpg";
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Regex urlinfo = new Regex(parameter, this.getSupportedLinks());
        String name_chapter = br.getRegex("<h1><font color=\"white\">([^<>\"]+) Chapter \\d+</font></h1>").getMatch(0);
        final String url_chapter = urlinfo.getMatch(1);
        final String url_name = urlinfo.getMatch(0);
        String ext = null;
        final String[] images = br.getRegex("data-original='(https?://[^<>\"\\']+)'").getColumn(0);
        if (images == null || images.length == 0) {
            return null;
        }
        if (name_chapter == null) {
            /* Fallback */
            name_chapter = url_name;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(name_chapter + "_" + url_chapter);
        final int padLength = StringUtils.getPadLength(images.length);
        int page = 0;
        for (final String finallink : images) {
            page++;
            final String page_formatted = String.format(Locale.US, "%0" + padLength + "d", page);
            if (finallink == null) {
                return null;
            }
            ext = getFileNameExtensionFromURL(finallink, extension_fallback);
            if (ext == null) {
                ext = extension_fallback;
            }
            final String filename = name_chapter + "_" + url_chapter + "_" + page_formatted + ext;
            final DownloadLink dl = this.createDownloadlink(finallink);
            dl._setFilePackage(fp);
            dl.setFinalFileName(filename);
            dl.setLinkID(filename);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            distribute(dl);
        }
        return decryptedLinks;
    }
}
