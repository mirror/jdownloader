//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.nutils.encoding.HTMLEntities;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 20458 $", interfaceVersion = 2, names = { "4link.in" }, urls = { "https?://(www\\.)?4link\\.in/[a-zA-Z0-9]+" }, flags = { 0 })
public class MFS_ShortUrlScript extends PluginForDecrypt {

    /**
     * This class supports, http://mfscripts.com/short_url_script.html template.
     *
     * @author raztoki
     */
    public MFS_ShortUrlScript(PluginWrapper wrapper) {
        super(wrapper);
    }

    // default setters, don't change.
    private boolean supportsHTTPS = true;
    private boolean hasCaptcha    = false;

    /**
     * use to define hasCaptcha, and supportsHTTPS, only required when site doesn't match default setters
     * 
     * @param link
     * @throws PluginException
     */
    private void setConstants(final CryptedLink link) throws PluginException {
        final String host = Browser.getHost(link.toString());
        if (host == null || "".equalsIgnoreCase(host)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (host.contains("4link.in")) {
            supportsHTTPS = false;
            hasCaptcha = false;
        }

    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        setConstants(param);
        String parameter = param.toString().replaceFirst("^https?://", (supportsHTTPS ? "https://" : "http://"));
        br.getPage(parameter);
        String frame = br.getRegex("<frame src=\"(interstitualAdTop\\.php\\?url=\\d+)\"").getMatch(0);
        if (frame == null) {
            logger.warning("Plugin Defect " + parameter);
            return null;

        }
        br.getPage(frame);
        String link = br.getRegex("<a href=\"(.*?)\" class=[^>]+>skip advert\\s*></a>").getMatch(0);
        if (link == null) {
            logger.warning("Possible Plugin Defect: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(HTMLEntities.unhtmlentities(link)));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) throws PluginException {
        setConstants(link);
        return hasCaptcha;
    }

}