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
import java.util.Collections;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "etsy.com" }, urls = { "https?://www\\.?etsy\\.com/[^/]+/listing/([^/]+)(/[^/]+)?" })
public class Etsy extends antiDDoSForDecrypt {
    public Etsy(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> links = new ArrayList<String>();
        getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.br.setFollowRedirects(true);
        String title = br.getRegex("<h1[^>]+data-buy-box-listing-title\\s*=\\s*\"true\"[^>]*>\\s*([^<]+)\\s*").getMatch(0);
        if (StringUtils.isEmpty(title)) {
            title = br.getRegex("<title>\\s*([^<]+)\\s+-\\s+Etsy\\.\\w+").getMatch(0);
        }
        if (StringUtils.isEmpty(title)) {
            String[] nameMatches = new Regex(param.getCryptedUrl(), "https?://www\\.?etsy\\.com/[^/]+/listing/([^/]+)(/[^/]+)?").getColumn(0);
            if (nameMatches.length > 1) {
                title = nameMatches[1];
            } else {
                title = nameMatches[0];
            }
        }
        // Detail page images
        Collections.addAll(links, br.getRegex("<img[^>]+class\\s*=\\s*\"[^\"]*carousel[^\"]*\"[^>]+data-src-zoom-image\\s*=\\s*\"([^\"]+)\"[^>]*>").getColumn(0));
        for (String link : links) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
        }
        if (StringUtils.isNotEmpty(title)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(title).trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
