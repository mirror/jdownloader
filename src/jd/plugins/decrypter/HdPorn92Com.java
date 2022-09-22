//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hdporn92.com" }, urls = { "https?://(?:www\\.)?hdporn92\\.com/[A-Za-z0-9\\-]+/?" })
public class HdPorn92Com extends antiDDoSForDecrypt {
    public HdPorn92Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
        }
        return prepBr;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        getPage(param.getCryptedUrl());
        String encodedTitle = br.getRegex("<meta[^>]+property\\s*=\\s*\"og:title\"[^>]+content\\s*=\\s*\"([^\"]*)").getMatch(0);
        FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlOnlyDecode(encodedTitle));
        String[] additionalServers = br.getRegex("<a\\s+class=\"button\"[^>]+href=\"([^\"]*)\"[^>]+>.*?Server \\d+").getColumn(0);
        if (additionalServers != null) {
            for (String server : additionalServers) {
                decryptedLinks.add(createDownloadlink(server));
            }
        }
        String url = br.getRegex("<meta[^>]+itemprop\\s*=\\s*\"embedURL\"[^>]+content\\s*=\\s*\"([^\"]*)").getMatch(0);
        decryptedLinks.add(createDownloadlink(url));
        fp.addLinks(decryptedLinks);
        fp.setAllowInheritance(true);
        return decryptedLinks;
    }
}