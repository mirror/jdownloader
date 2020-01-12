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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "newepisodes.co" }, urls = { "https?://(www\\.)?(newepisodes\\.co|watchfilms\\.me)/watch-[^/]+/\\d+.+" })
public class NewEpisodesCo extends antiDDoSForDecrypt {
    public NewEpisodesCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>(?:Watch\\s+)?([^<]+)(Episodes Online)?").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (StringUtils.isNotEmpty(fpName)) {
            fp.setName(Encoding.htmlDecode(fpName.trim()));
        }
        String[] mirrorIDs = br.getRegex("<li[^>]+class\\s*=\\s*\"[^\"]*playlist_entry[^\"]*\"[^>]+id\\s*=\\s*\"\\s*([^\"]+)\\s*\"[^>]*>").getColumn(0);
        if (mirrorIDs != null && mirrorIDs.length > 0) {
            final Browser br2 = br.cloneBrowser();
            for (String mirrorID : mirrorIDs) {
                mirrorID = Encoding.htmlDecode(mirrorID);
                getPage(br2, br2.getURL("/embed/" + mirrorID).toString());
                String link = br2.getRegex("<iframe[^>]+src\\s*=\\s*\"\\s*([^\"]+)\\s*\"").getMatch(0);
                if (link != null && link.length() > 0) {
                    DownloadLink dl = createDownloadlink(Encoding.htmlDecode(link));
                    fp.add(dl);
                    distribute(dl);
                }
            }
        } else {
            String[] links = br.getRegex("<div[^>]+data-type\\s*=\\s*\"[^\"]*show[^\"]*\"[^>]+class\\s*=\\s*\"list-item[^>]+>\\s*<a[^>]+href\\s*=\\s*\"[^\"]*(/watch-[^\"]+)\"[^>]+class\\s*=\\s*\"[^\"]*item-url[^\"]*\"[^>]*>").getColumn(0);
            if (links != null && links.length > 0) {
                for (String link : links) {
                    link = link.trim().replaceAll("^//", "https://");
                    if (link.startsWith("/") || !link.startsWith("http")) {
                        link = br.getURL(link).toString();
                    }
                    ret.add(createDownloadlink(link));
                }
                fp.addLinks(ret);
            }
        }
        return ret;
    }
}