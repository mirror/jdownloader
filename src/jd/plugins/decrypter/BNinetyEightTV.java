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
import java.util.HashSet;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "b98.tv" }, urls = { "https?://(?:www\\.)?b98\\.tv/(?:videos_categories|video)/([^/]+)/?.*" })
public class BNinetyEightTV extends antiDDoSForDecrypt {
    public BNinetyEightTV(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String stub = new Regex(parameter, "/(?:videos_categories|video)/([^/]+)/?").getMatch(0);
        if (StringUtils.isEmpty(stub) || stub.toLowerCase().matches("(cartoons|series|studios)")) {
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>\\s*([^<]+)(?:\\s+\\|\\s+B98\\.TV)?").getMatch(0);
        final Set<String> links = new HashSet<String>();
        Collections.addAll(links, br.getRegex("file\\s*:\\s*[\"']([^\"']+)[\"']").getColumn(0));
        if (!links.isEmpty()) { // video page
            for (String link : links) {
                decryptedLinks.add(createDownloadlink(link));
            }
        } else { // list page
            Collections.addAll(links, br.getRegex("<a\\s+class\\s*=\\s*\"[^\"]*page-numbers[^\"]*\"[^>]*href\\s*=\\s*\"([^\"]*)\"").getColumn(0));
            Collections.addAll(links, br.getRegex("<div[^>]*class=\"image-holder\"[^>]*>\\s*<a[^>]*href=\"([^\"]+)\"[^>]*>").getColumn(0));
            for (String link : links) {
                link = br.getURL(link).toString();
                decryptedLinks.add(createDownloadlink(link));
            }
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}