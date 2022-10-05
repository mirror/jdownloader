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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sznpaste.net" }, urls = { "https?://(?:www\\.)?sznpaste\\.net/[A-Za-z0-9]+" })
public class SznpasteNet extends PluginForDecrypt {
    public SznpasteNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        /*
         * This is not a classi pastebin it is more like a link cryptor so in this case we do not really want to preserver the plain text
         * but only parse the URLs from it.
         */
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.PASTEBIN };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String url = param.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getPage(url);
        if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"error\\-page\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String plaintxt = br.getRegex("<div class=\"base\\-block\">(.*?)</div>\\s*?</div>").getMatch(0);
        if (plaintxt == null) {
            /* Fallback */
            logger.info("Failed to find exact html, scanning full html code of website for downloadable content");
            plaintxt = br.toString();
        }
        if (plaintxt == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Find URLs inside plaintext/html code */
        final String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links == null || links.length == 0) {
            logger.info("Found no links in link: " + url);
            return ret;
        }
        logger.info("Found " + links.length + " links in total.");
        for (final String externalurl : links) {
            if (this.canHandle(url)) {
                final DownloadLink link = createDownloadlink(externalurl);
                ret.add(link);
            }
        }
        return ret;
    }
}