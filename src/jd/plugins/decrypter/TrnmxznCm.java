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
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerFilter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.TrinimixzoneCom;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "trinimixzone.com" }, urls = { "https?://(www\\.)?trinimixzone\\.com/forum/[^\\s/]+" }) 
public class TrnmxznCm extends PluginForDecrypt {

    public TrnmxznCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        TrinimixzoneCom.login(br);
        br.getPage(parameter);
        final String title = Encoding.htmlDecode(br.getRegex("<title>(.*?)</title>").getMatch(0));
        br.setFollowRedirects(false);
        try {
            final String[] posts = br.getRegex("<!-- start: postbit -->(.*?)<!-- end: postbit -->").getColumn(0);
            boolean saidThanks = false;
            for (final String post : posts) {
                if (post.contains("alerta_thx message")) {
                    // need top say thanks
                    String pid = new Regex(post, "pid(\\d+)").getMatch(0);
                    saidThanks = true;
                    if (parameter.contains("?pid=")) {
                        br.getPage(parameter + "&action=thank");
                    } else {
                        br.getPage(parameter + "?pid=" + pid + "&action=thank");
                    }
                }
            }
            if (saidThanks) {
                br.getPage(parameter);
            }
            for (final String link : HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getBaseURL())) {
                if (link.contains("trinimixzone.com")) {
                    continue;
                }
                decryptedLinks.add(createDownloadlink(link));
            }
        } catch (Throwable e) {
            logger.log(e);
        }
        if (title != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    @Override
    public LinkCrawler getCustomNextCrawler() {
        final LinkCrawler lc = new LinkCrawler(true, true);
        lc.setFilter(new LinkCrawlerFilter() {

            @Override
            public boolean dropByUrl(final CrawledLink link) {
                final PluginForHost plugin = link.gethPlugin();
                if (plugin != null) {
                    if (("ftp".equalsIgnoreCase(plugin.getHost()) || "http links".equalsIgnoreCase(plugin.getHost()))) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean dropByFileProperties(CrawledLink link) {
                return false;
            }
        });
        return lc;
    }

}