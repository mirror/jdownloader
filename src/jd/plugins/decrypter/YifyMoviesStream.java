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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yifymovies.stream" }, urls = { "https?://(www\\.)?yifymovies\\.stream/watch/.+" })
public class YifyMoviesStream extends antiDDoSForDecrypt {
    public YifyMoviesStream(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<li[^>]+class=\"selected\"[^>]*>\\s*<a[^>]*>\\s*<span[^>]+class=\"name\"[^>]+title=\"([^\"]+)\"[^>]*>").getMatch(0);
        if (StringUtils.isEmpty(fpName)) {
            fpName = br.getRegex("<title>Watch\\s+([^<]+)\\s+Online\\s+Free\\s+YTS").getMatch(0);
        }
        ArrayList<String> links = new ArrayList<String>();
        Collections.addAll(links, br.getRegex("var\\s+link_server_\\w+\\s*=\\s*\"([^\"]+)\"\\s*;").getColumn(0));
        Collections.addAll(links, br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]*>\\s*<span[^>]+class\\s*=\\s*\"name\"").getColumn(0));
        for (String link : links) {
            link = Encoding.htmlDecode(link).replaceAll("^//", "https://");
            if (link.startsWith("/")) {
                link = br.getURL(link).toString();
            }
            decryptedLinks.add(createDownloadlink(link));
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, true);
            fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}