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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wiadomosci.pless.pl" }, urls = { "https?://(\\w+\\.)?pless\\.pl/galeria/\\d+/\\d+" })
public class WiadomosciPlessPl extends PluginForDecrypt {
    public WiadomosciPlessPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String addedlink = param.getCryptedUrl();
        br.setFollowRedirects(true);
        // creates HTTP GET to listening URLs
        br.getPage(addedlink + "/?p=lista");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // look for a name for the package
        final String title = br.getRegex("<title>(.*?)</title>").getMatch(0);
        // now look for all the images we want..
        final String[] links = br.getRegex("data-url=\"(https?://gal\\.pless\\.pl/[^\"]+\\.jpe?g[^\"]*)\"").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // this does task for each link found
        for (final String singleLink : links) {
            // create downloadlink for each image
            final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(singleLink));
            // set fast linkcheck otherwise it will take for ever!
            dl.setAvailable(true);
            // add downloadlink to array which returns
            ret.add(dl);
        }
        // set file package name, this places all links into one package in JDownloader.
        final FilePackage fp = FilePackage.getInstance();
        if (title != null) {
            fp.setName(Encoding.htmlDecode(title).trim());
        } else {
            /* Fallback */
            fp.setName(br._getURL().getPath());
        }
        fp.addLinks(ret);
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}