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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kindgirls.com" }, urls = { "http://(www\\.)?kindgirls\\.com/(video|gallery|girls)/([a-zA-Z0-9_\\-]+)(/[a-zA-Z0-9_\\-]+(/\\d+/?)?)?" }, flags = { 0 })
public class KndGrlsCom extends PluginForDecrypt {

    public KndGrlsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.toString();
        final String page = br.getPage(parameter);
        if (parameter.contains("com/gallery")) { // it's a gallery
            if (br.containsHTML(">Sorry, gallery not found")) {
                logger.info("Link offline: " + parameter);
                return new ArrayList<DownloadLink>();
            }
            return decryptGalleryLinks(br);
        } else if (parameter.contains("com/girls")) { // it's a girl's gallery
                                                      // collection
            return decryptGirlsGalleryCollection(page);
        } else if (parameter.contains("com/video")) { // it's a video
            return decryptVideoLinks(br);
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private ArrayList<DownloadLink> decryptGirlsGalleryCollection(String page) throws IOException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        Regex girlGalleriesRex = new Regex(page, "<h4>Photo Galleries</h4>((<div class=\"gallery_list\"><a title='[a-zA-Z0-9, \\-_]+' href='/gallery/[a-zA-Z0-9_\\-/]+'><img src='[a-zA-Z0-9_\\.-/]+' alt='[a-zA-Z0-9 _\\-]+' border='0'><br /> *\\d+ photos</a></div>)+)");

        String[][] matches = girlGalleriesRex.getMatches();

        for (String[] match : matches) {
            for (String gallerymatch : match) {
                Regex galleryDetailRex = new Regex(gallerymatch, "<div class=\"gallery_list\"><a title='([a-zA-Z0-9, \\-_]+)' href='(/gallery/[a-zA-Z0-9_\\-/]+)'><img src='[a-zA-Z0-9_\\.-/]+' alt='[a-zA-Z0-9 _\\-]+' border='0'><br /> *(\\d+) photos</a></div>");
                String link = "http://www.kindgirls.com" + galleryDetailRex.getMatch(1);
                Browser galleryBrowser = br.cloneBrowser();
                galleryBrowser.getPage(link);
                for (DownloadLink currentLink : decryptGalleryLinks(galleryBrowser)) {
                    decryptedLinks.add(currentLink);
                }
            }
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptVideoLinks(Browser br) throws PluginException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String link = br.getRegex("\\'flashvars\\',\\'\\&amp;file=(http://[^<>\"]*?)\\&amp;volume=\\d+\\'\\)").getMatch(0);
        if (link == null) link = br.getRegex("(\"|\\')(http://vids\\.kindgirls\\.com/[^<>\"]*?)(\"|\\')").getMatch(1);
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
        final String[] links = br.getRegex("\"http://gals\\.kindgirls\\.com/([^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String finallink : links) {
            final DownloadLink dlLink = createDownloadlink("directhttp://http://gals.kindgirls.com/" + finallink.replace("thumbnails/tn", ""));
            // rename files if required. Fixes alpha numeric sorting issues
            Regex regex = new Regex(dlLink.getName(), "(.*_)(\\d\\.[a-zA-Z0-9]+)$");
            if (regex.matches()) {
                dlLink.setFinalFileName(regex.getMatch(0) + "0" + regex.getMatch(1));
            }
            decryptedLinks.add(dlLink);
        }
        final String girlsname = br.getRegex("<h3>Photo.*<a href=\\'/girls/[a-zA-Z0-9 _\\-/]+\\'>([a-zA-Z0-9 _\\-]+)</a>.*</h3>").getMatch(0);
        if (girlsname != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName("Kindgirls - " + girlsname.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}