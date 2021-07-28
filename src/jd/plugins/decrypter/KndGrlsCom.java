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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kindgirls.com" }, urls = { "https?://(?:www\\.)?kindgirls\\.com/(?:gallery|girls|video)\\.php\\?id=\\d+" })
public class KndGrlsCom extends PluginForDecrypt {
    public KndGrlsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_GALLERY = "https?://[^/]+/gallery\\.php\\?id=(\\d+)";
    private static final String TYPE_GIRLS   = "https?://[^/]+/girls\\.php\\?id=(\\d+)";
    private static final String TYPE_VIDEO   = "https?://[^/]+/video\\.php\\?id=(\\d+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.getCryptedUrl().replaceFirst("http:", "https:");
        final String page = br.getPage(parameter);
        if (new Regex(parameter, TYPE_GALLERY).matches()) { // it's a gallery
            return decryptGalleryLinks(param, br);
        } else if (new Regex(parameter, TYPE_GIRLS).matches()) { // it's a girl's gallery
            return decryptGirlsGalleryCollection(param, page);
        } else if (new Regex(parameter, TYPE_VIDEO).matches()) { // it's a video
            return decryptVideoLinks(param, br);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private ArrayList<DownloadLink> decryptGalleryLinks(final CryptedLink param, final Browser br) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Sorry, gallery not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String[] links = br.getRegex("\"(https?://gals\\.kindgirls\\.com/[^<>\"]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            return null;
        }
        for (final String thumbnail : links) {
            final String finallink;
            final Regex thumbnailToFullsize = new Regex(thumbnail, "(https?://gals\\.[^/]+/[^/]+/[^/]+)/[^/]+/([^/+]+)");
            if (thumbnailToFullsize.matches()) {
                finallink = thumbnailToFullsize.getMatch(0) + "/" + thumbnailToFullsize.getMatch(1);
            } else {
                /* Fallback */
                finallink = thumbnail;
            }
            final DownloadLink dlLink = createDownloadlink(finallink);
            // rename files if required. Fixes alpha numeric sorting issues
            Regex regex = new Regex(dlLink.getName(), "(.*_)(\\d\\.[a-zA-Z0-9]+)$");
            if (regex.matches()) {
                dlLink.setFinalFileName(regex.getMatch(0) + "0" + regex.getMatch(1));
            }
            dlLink.setAvailable(true);
            decryptedLinks.add(dlLink);
        }
        String girlsname = br.getRegex("<h3>.*?<a href='/girls/[a-zA-Z0-9 _\\-/]+'>([a-zA-Z0-9 _\\-]+)</a>.*?</h3>").getMatch(0);
        if (girlsname == null) {
            girlsname = br.getRegex("<div id='up_izq'><h3>([a-zA-Z0-9 _\\-]+)</h3>").getMatch(0);
        }
        if (girlsname == null) {
            /* 2020-10-02 */
            girlsname = br.getRegex("<h3> <a href=\\'/girls/[^\\']+\\'>([^<>\"]+)</a>  </h3>").getMatch(0);
        }
        if (girlsname == null) {
            /* Fallback --> Use GalleryID */
            girlsname = new Regex(br.getURL(), TYPE_GALLERY).getMatch(0);
        }
        if (girlsname != null) {
            final String galleryID = new Regex(br.getURL(), "/(\\d+)/?$").getMatch(0);
            String packagename = girlsname.trim();
            if (galleryID != null) {
                packagename += "_" + galleryID;
            }
            FilePackage fp = FilePackage.getInstance();
            fp.setName("Kindgirls - " + packagename);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptGirlsGalleryCollection(final CryptedLink param, String page) throws IOException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String[] urls = HTMLParser.getHttpLinks(br.toString(), br.getURL());
        for (final String url : urls) {
            if (url.matches(TYPE_GALLERY)) {
                decryptedLinks.add(this.createDownloadlink(url));
            }
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptVideoLinks(final CryptedLink param, final Browser br) throws PluginException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Video not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String link = br.getRegex("(\"|\\')(https?://vids\\.kindgirls\\.com/[^<>\"]*?)(\"|\\')").getMatch(1);
        if (link == null) {
            logger.severe("Variable 'link' not found, Please report issue to JDownloader Developement.");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
        return decryptedLinks;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}