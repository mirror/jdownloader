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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kindgirls.com" }, urls = { "https?://(?:www\\.)?kindgirls\\.com/(video|gallery|girls)/([^/]+)(/[^/]+(/\\d+/?)?)?" })
public class KndGrlsCom extends PluginForDecrypt {
    public KndGrlsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_GALLERY = "https?://(?:www\\.)?kindgirls\\.com/gallery/([^/]+)(/[^/]+(/\\d+/?)?)?";
    private static final String TYPE_GIRLS   = "https?://(?:www\\.)?kindgirls\\.com/girls/([^/]+)(/[^/]+(/\\d+/?)?)?";
    private static final String TYPE_VIDEO   = "https?://(?:www\\.)?kindgirls\\.com/video/[^/]+)(/[^/]+(/\\d+/?)?)?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> crawledLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http:", "https:");
        final String page = br.getPage(parameter);
        if (new Regex(parameter, TYPE_GALLERY).matches()) { // it's a gallery
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Sorry, gallery not found")) {
                crawledLinks.add(createOfflinelink(parameter));
                return crawledLinks;
            }
            return decryptGalleryLinks(br);
        } else if (new Regex(parameter, TYPE_GIRLS).matches()) { // it's a girl's gallery
            // collection
            return decryptGirlsGalleryCollection(page);
        } else if (new Regex(parameter, TYPE_VIDEO).matches()) { // it's a video
            return decryptVideoLinks(br);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private ArrayList<DownloadLink> decryptGirlsGalleryCollection(String page) throws IOException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String[] urls = br.getRegex("\\'(/gallery/[^\\']+)\\'").getColumn(0);
        for (String url : urls) {
            url = br.getURL(url).toString();
            decryptedLinks.add(this.createDownloadlink(url));
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptVideoLinks(Browser br) throws PluginException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Video not found")) {
            logger.info("Link offline: " + br.getURL());
            return decryptedLinks;
        }
        String link = br.getRegex("\\'flashvars\\',\\'\\&amp;file=(https?://[^<>\"]*?)\\&amp;volume=\\d+\\'\\)").getMatch(0);
        if (link == null) {
            link = br.getRegex("(\"|\\')(https?://vids\\.kindgirls\\.com/[^<>\"]*?)(\"|\\')").getMatch(1);
        }
        if (link == null || link.length() == 0) {
            logger.severe("Variable 'link' not found, Please report issue to JDownloader Developement.");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        decryptedLinks.add(createDownloadlink("directhttp://" + Encoding.htmlDecode(link)));
        String girlsname = br.getRegex("<h3>Video *. *<a href=\\'/girls/[a-zA-Z0-9 _\\-/]+\\'>([a-zA-Z0-9\\- _]+)</a>.*").getMatch(0);
        if (girlsname != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName("Kindgirls - " + girlsname.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptGalleryLinks(Browser br) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String[] links = br.getRegex("\"https?://gals\\.kindgirls\\.com/([^<>\"]*?)\"[^<>]*?target=\"_blank\"").getColumn(0);
        if (links == null || links.length == 0) {
            return null;
        }
        for (String finallink : links) {
            final DownloadLink dlLink = createDownloadlink("directhttp://https://gals.kindgirls.com/" + finallink.replace("thumbnails/tn", ""));
            // rename files if required. Fixes alpha numeric sorting issues
            Regex regex = new Regex(dlLink.getName(), "(.*_)(\\d\\.[a-zA-Z0-9]+)$");
            if (regex.matches()) {
                dlLink.setFinalFileName(regex.getMatch(0) + "0" + regex.getMatch(1));
            }
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
            /* Fallback */
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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}