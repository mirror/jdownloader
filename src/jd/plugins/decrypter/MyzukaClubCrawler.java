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
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class MyzukaClubCrawler extends antiDDoSForDecrypt {
    public MyzukaClubCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "myzuka.club", "myzuka.ru", "myzuka.org", "myzuka.fm", "myzuka.me" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:[a-z]{2}/)?Album/(\\d+)(/[A-Za-z0-9\\-]+)?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected boolean useRUA() {
        /* 2020-02-26: Try to prevent IP bans. */
        return true;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-02-26: Try to prevent IP bans. */
        return 1;
    }

    /** 2020-02-27: This service is blocking all but turkish IPs! Turkish Proxy/VPN required or every request will return 404! */
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String hostOld = Browser.getHost(param.getCryptedUrl(), false);
        final String parameter = param.getCryptedUrl().replaceFirst("(?)http://", "https://").replaceFirst(Pattern.quote(hostOld), this.getHost());
        br.setFollowRedirects(true);
        getPage(parameter);
        final String albumID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        String url_title = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        if (url_title != null) {
            /* Make this title nicer */
            url_title = url_title.replace("/", "");
            final String year = new Regex(url_title, "(\\d{4})").getMatch(0);
            if (year != null) {
                url_title = url_title.replace("-", " ");
                url_title = url_title.replace(year, "(" + year + ")");
            }
        }
        /* offline|abused */
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Альбом удален по просьбе правообладателя")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String[] info = br.getRegex("(<div id=\"playerDiv\\d+\".*?)</div>\\s*</div>\\s*</div>").getColumn(0);
        if (info == null || info.length == 0) {
            info = br.getRegex("(<div id=\"playerDiv\\d+\".*?)</a>\\s+</div>").getColumn(0);
        }
        if (info == null || info.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String fpName = br.getRegex("class=\"content__title\">\\s*?<h1>\\s*([^<>\"]+)\\s*<").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<h1>\\s*([^<>\"]+)\\s*<").getMatch(0);
        }
        if (fpName == null) {
            /* Fallback */
            fpName = url_title;
        }
        if (fpName == null) {
            /* Final fallback */
            fpName = albumID;
        }
        for (final String html : info) {
            final String url = new Regex(html, "(/Song/\\d+/[^<>\"]+)\"").getMatch(0);
            final String artist = new Regex(html, "data-artist=\"([^<>\"]+)\"").getMatch(0);
            String filesize = new Regex(html, "class=\"time\">([^<>\"]*?)<").getMatch(0);
            if (url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String filename = null;
            final String[] titles = new Regex(html, "href=\"" + Pattern.quote(url) + "\"[^>]*>([^<]+)<").getColumn(0);
            filesize = new Regex(filesize, "(\\d+(?:,\\d+)?)").getMatch(0) + "MB";
            if (titles != null && titles.length > 0) {
                final String title = titles[titles.length - 1];
                filename = "";
                if (artist != null) {
                    filename += Encoding.htmlDecode(artist) + " - ";
                }
                filename += Encoding.htmlDecode(title) + ".mp3";
            }
            final DownloadLink fina = createDownloadlink(br.getURL(Encoding.htmlDecode(url)).toString());
            if (filename != null) {
                fina.setName(filename);
            }
            if (filesize != null) {
                fina.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            fina.setAvailable(true);
            ret.add(fina);
        }
        if (fpName != null && ret.size() > 0) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(ret);
        }
        return ret;
    }
}
