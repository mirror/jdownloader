//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 0 $", interfaceVersion = 2, names = { "rajce.idnes.cz" }, urls = { "http://.*\\.rajce\\.idnes\\.cz/([^/]+/$|[^/]+/?#.*|[^/]*\\?.+|/?$)" }, flags = { 0 })
public class DnsCz extends PluginForDecrypt {

    public DnsCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://img\\d+\\.rajce.*";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }

        while (parameter != null) {
            parsePage(decryptedLinks, parameter);

            parameter = br.getRegex("<a href=\"([^\"<>]*?page=\\d+[^\"<>]*?)\">[^<>]*?\\»</a>").getMatch(0);

        }
        return decryptedLinks;

    }

    /**
     * @param decryptedLinks
     * @param parameter
     * @throws IOException
     */
    protected void parsePage(ArrayList<DownloadLink> decryptedLinks, String parameter) throws IOException {
        br.getPage(parameter);
        decryptAlbumPage(decryptedLinks, br);

        String[] albums = br.getRegex("<a class=\"albumName\" href=\"(https?://.*?.rajce.idnes.cz/[^/]+/)\">").getColumn(0);
        if (albums != null) {
            for (String albumUrl : albums) {
                Browser clone = br.cloneBrowser();
                clone.getPage(albumUrl);
                decryptAlbumPage(decryptedLinks, clone);
            }
        }
    }

    /**
     * @param decryptedLinks
     * @param br
     * @throws IOException
     */
    protected void decryptAlbumPage(ArrayList<DownloadLink> decryptedLinks, Browser br) throws IOException {

        String albumUserName = br.getRegex("<h1.*?id=\"albumlistUserName\">(.*?)<").getMatch(0);

        String albumName = br.getRegex("<h2 id=\"albumName\">(.*?)</h2>").getMatch(0);
        if (albumName != null && albumUserName != null) {
            albumName = albumName.trim();
            albumUserName = albumUserName.trim();
            String[][] galleryLinks = br.getRegex("<a id=\"p_(\\d+)\" href=\"(https?://img\\d+.rajce.idnes.cz[^\"]+)\".*?title=\"([^\"]*)").getMatches();

            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(albumName) + " by " + Encoding.htmlDecode(albumUserName));

            for (String[] link : galleryLinks) {
                DownloadLink dl = createDownloadlink(link[1]);
                dl.setProperty("album", albumName);
                dl.setProperty("user", albumUserName);
                dl.setProperty("title", link[2]);
                dl.setAvailableStatus(AvailableStatus.TRUE);
                fp.add(dl);
                decryptedLinks.add(dl);
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}