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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keezmovies.com" }, urls = { "http://(www\\.)?keezmovies\\.com/(video|embed)/[\\w\\-]+" }, flags = { 0 })
public class KeezMoviesComDecrypter extends PluginForDecrypt {

    public KeezMoviesComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Using playerConfig script */
    /* Tags: playerConfig.php */
    // EmbedDecrypter 0.1.3

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("/embed/", "/video/");
        final DownloadLink decryptedMainlink = createDownloadlink(parameter.replace("keezmovies.com/", "keezmoviesdecrypted.com/"));
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String filename = br.getRegex("<span class=\"fn\" style=\"display:none\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?) \\- KeezMovies\\.com</title>").getMatch(0);
        }
        decryptedLinks = jd.plugins.decrypter.PornEmbedParser.findEmbedUrls(this.br, filename);
        if (decryptedLinks != null && decryptedLinks.size() > 0) {
            return decryptedLinks;
        }
        /* No external url found --> Video must be selfhosted. */
        decryptedLinks = new ArrayList<DownloadLink>();
        decryptedLinks.add(decryptedMainlink);
        return decryptedLinks;
    }

}
