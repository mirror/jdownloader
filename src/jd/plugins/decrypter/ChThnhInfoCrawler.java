//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLSearch;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chauthanh.info" }, urls = { "https?://[\\w\\.]*?chauthanh\\.info/(animeDownload/anime/.*?|\\w+/view/[^/]+)\\.html" })
public class ChThnhInfoCrawler extends PluginForDecrypt {
    public ChThnhInfoCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String ANIMEVIEW = "(?i)https?://[^/]+/\\w+/view/([^/]+)\\.html";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String addedurl = param.getCryptedUrl().replaceFirst("http://", "https://");
        br.setFollowRedirects(true);
        getPage(addedurl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*Licensed, no download available\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().equals("https://chauthanh.info/404")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = null;
        final boolean enableStrangeOldHandling = false;
        if (addedurl.matches(ANIMEVIEW) && enableStrangeOldHandling) {
            if (br.containsHTML("class=\"center\">Server</th>[\t\n\r ]+<th>Size</th>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            fpName = br.getRegex("<h2 itemprop=\"name\">([^<>\"]*?)</h2>").getMatch(0);
            final String[] links = br.getRegex("\\'\\.\\.(/download/[^<>\"]*?)\\'").getColumn(0);
            if (links == null || links.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String finallink : links) {
                finallink = "http://chauthanh.info/anime" + Encoding.htmlDecode(finallink);
                ret.add(createDownloadlink(finallink));
            }
        } else {
            if (br.containsHTML("The series information was not found on this server")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("Removed due to licensed\\.")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(">\\s*No files available for this series")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            fpName = br.getRegex("<title>Download anime(.*?)\\- Download Anime").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("class=\"bold1\">Download anime(.*?)</span>").getMatch(0);
                if (fpName == null) {
                    fpName = HTMLSearch.searchMetaTag(br, "og:title");
                }
            }
            String[] links = br.getRegex("<tr><td><a href=\"(/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                links = br.getRegex("\"([^\"]*/download/\\d+/.*?)\"").getColumn(0);
            }
            if (links == null || links.length == 0) {
                links = br.getRegex("(\\.\\./download/[^<>\"\\']+)").getColumn(0);
            }
            if (links == null || links.length == 0) {
                if (br.containsHTML("<th>Size</th>")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            final String[] filesizes = br.getRegex("class='center'>\\d+</td>\\s*<td>(\\d+[^<]+)</td>").getColumn(0);
            int index = 0;
            for (String finallink : links) {
                finallink = br.getURL(finallink).toString();
                if (finallink.contains("/download/") && !finallink.contains("/download/eri/")) {
                    finallink = finallink.replace("/download/", "/download/eri/");
                }
                final DownloadLink link = createDownloadlink(DirectHTTP.createURLForThisPlugin(finallink));
                link.setAvailable(true);
                final String weakFilename = Plugin.getFileNameFromURL(finallink);
                if (weakFilename != null) {
                    link.setName(Encoding.htmlDecode(weakFilename).trim().replace(".html", ""));
                }
                if (filesizes != null && filesizes.length == links.length) {
                    final String filesizeStr = filesizes[index];
                    link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                }
                ret.add(link);
                index++;
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(ret);
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    private PluginForHost plugin = null;

    private void getPage(final String parameter) throws Exception {
        getPage(br, parameter);
    }

    private void getPage(final Browser br, final String parameter) throws Exception {
        loadPlugin();
        ((jd.plugins.hoster.ChThnhInfo) plugin).setBrowser(br);
        ((jd.plugins.hoster.ChThnhInfo) plugin).getPage(parameter);
    }

    public void loadPlugin() {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("chauthanh.info");
            if (plugin == null) {
                throw new IllegalStateException(getHost() + " hoster plugin not found!");
            }
        }
    }
}