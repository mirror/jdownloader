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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision: 41168 $", interfaceVersion = 2, names = { "5movies.to" }, urls = { "https?://(www\\.)?5movies\\.to/(movie|tv|directlink)/.+" })
public class FiveMoviesTo extends antiDDoSForDecrypt {
    public FiveMoviesTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<meta property=\"og:title\" content=\"Watch ([^\"]+) Online Free").getMatch(0);
        if (StringUtils.containsIgnoreCase(parameter, "/directlink/")) {
            postPage(br.getURL(), "chtc=Click%20Here%20to%20Continue");
            String redirectLink = br.getRedirectLocation() == null ? br.getURL() : br.getRedirectLocation();
            decryptedLinks.add(createDownloadlink(redirectLink));
        }
        String[] links = null;
        links = br.getRegex("<li class=\"download\"><a href=\"([^\"]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("<div class=\"Season\"([^$]*)<div class=\"sidebar\">").getColumn(0);
            if (links != null && links.length > 0) {
                links = HTMLParser.getHttpLinks(links[0], "");
            }
        }
        if (links != null && links.length > 0) {
            for (final String link : links) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(br.getURL(link).toString())));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}