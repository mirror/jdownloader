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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "isubsmovies.com" }, urls = { "https?://(www\\.)?isubsmovies\\.com/(?:tvserie|episode|movie)/watch-.+" })
public class ISubsMovies extends antiDDoSForDecrypt {
    public ISubsMovies(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>\\s*Watch\\s+([^<]+)\\s+Online(?:\\s+for\\s+free)?\\s+with\\s+Subtitles").getMatch(0);
        ArrayList<String> links = new ArrayList<String>();
        String seasonBlock = br.getRegex("(<div[^>]+class\\s*=\\s*\"container[^\"]+seasons[^$]+)<div[^>]+class\\s*=\\s*\"container\\s+").getMatch(0);
        if (StringUtils.isNotEmpty(seasonBlock)) {
            Collections.addAll(links, new Regex(seasonBlock, "<a[^>]+href\\s*=\\s*\"(/episode/[^\"]+)\"").getColumn(0));
        }
        if (StringUtils.containsIgnoreCase(parameter, "/episode/") || StringUtils.containsIgnoreCase(parameter, "/movie/")) {
            Browser br2 = br.cloneBrowser();
            br2.getRequest().getHeaders().put("X-Requested-With", "XMLHttpRequest");
            postPage(br2, "/dbquery.php?action=loadPlayer", "");
            String[] embedSources = br2.getRegex("src\\s*=\\s*[\\\\\"]+([^\"]+)").getColumn(0);
            if (embedSources != null && embedSources.length > 0) {
                for (String embedSource : embedSources) {
                    embedSource = embedSource.replace("\\", "");
                    Collections.addAll(links, embedSource);
                    String subtitlePath = new Regex(embedSource, "subtitles=([^&$]+)").getMatch(0);
                    if (StringUtils.isNotEmpty(subtitlePath)) {
                        Collections.addAll(links, subtitlePath);
                        getPage(br2, subtitlePath);
                        subtitlePath = br2.getRegex("\"link\"\\s*:\\s*\"([^\"]+)\"").getMatch(0);
                        if (StringUtils.isNotEmpty(subtitlePath)) {
                            subtitlePath = subtitlePath.replace("\\", "");
                            Collections.addAll(links, "/dbquery.php?action=downloadSubtitles&url=" + Encoding.urlEncode(subtitlePath));
                        }
                    }
                }
            }
        }
        if (links != null && links.size() > 0) {
            links = new ArrayList<String>(new LinkedHashSet<String>(links));
            for (String link : links) {
                link = Encoding.htmlDecode(link);
                decryptedLinks.add(createDownloadlink(processPrefixSlashes(br, link)));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, true);
            fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String processPrefixSlashes(Browser br, String link) throws IOException {
        link = link.trim().replaceAll("^//", "https://");
        if (link.startsWith("/") || !link.startsWith("http")) {
            link = br.getURL(link).toString();
        }
        return link;
    }
}