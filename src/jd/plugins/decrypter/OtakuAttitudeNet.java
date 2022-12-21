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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class OtakuAttitudeNet extends PluginForDecrypt {
    public OtakuAttitudeNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "otaku-attitude.net" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-z0-9\\-]+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final GetRequest getRequest = br.createGetRequest(param.getCryptedUrl());
        final URLConnectionAdapter con = this.br.openRequestConnection(br.createGetRequest(param.getCryptedUrl()));
        try {
            if (this.looksLikeDownloadableContent(con)) {
                final DownloadLink direct = getCrawler().createDirectHTTPDownloadLink(getRequest, con);
                ret.add(direct);
                return ret;
            } else {
                br.followConnection();
            }
        } finally {
            con.disconnect();
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String contentID = br.getRegex("class=\"fiche_id\"[^>]*>(\\d+)</span>").getMatch(0);
        if (contentID == null) {
            logger.info("Failed to find any downloadable content");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("<h1 itemprop=\"name\">([^<]+)<a").getMatch(0);
        if (fpName == null) {
            /* Fallback: Grab title from URL */
            fpName = new Regex(br._getURL().getPath(), "/(.+)\\.html$").getMatch(0);
            if (fpName != null) {
                fpName = fpName.replace("-", " ");
            }
        }
        final String[] downloadIDs = br.getRegex("class=\"download(?: cell_impaire)?\" id=\"(\\d+)\"").getColumn(0);
        if (downloadIDs.length == 0) {
            /* Plugin broken or there is no downloadable content */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filenameBase = null;
        /* Crawl subtitles packed as .rar/.zip archives */
        final String[] bonusDownloadsHTMLs = br.getRegex("<tr class=\"download cell_impaire bonus\".*?class=\"cell cell_bonus\">\\d+ fois</td>").getColumn(-1);
        for (final String bonusDownloadsHTML : bonusDownloadsHTMLs) {
            final String downloadID = new Regex(bonusDownloadsHTML, "id=\"(\\d+)\"").getMatch(0);
            if (downloadID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String filename = new Regex(bonusDownloadsHTML, "class=\"cell cell_bonus\"[^>]*>([^<]+)</td>").getMatch(0);
            final String filesize = new Regex(bonusDownloadsHTML, "class=\"cell cell_bonus\">(\\d+ Mo)</td>").getMatch(0);
            final String url = br.getURL(getDirectDownloadurl(contentID, downloadID)).toString();
            final DownloadLink subtitle = this.createDownloadlink("directhttp://" + url);
            if (filename != null) {
                filename = Encoding.htmlDecode(filename).trim();
                if (filenameBase == null && filename.contains("_")) {
                    filenameBase = filename.split("_")[0];
                }
                subtitle.setName(filename);
            }
            if (filesize != null) {
                subtitle.setDownloadSize(SizeFormatter.getSize(filesize.replace("o", "b")));
            }
            subtitle.setAvailable(true);
            ret.add(subtitle);
        }
        final int padLength = StringUtils.getPadLength(downloadIDs.length);
        /* Crawl video clips */
        for (final String downloadID : downloadIDs) {
            final String downloadIDFormatted = StringUtils.formatByPadLength(padLength, Integer.parseInt(downloadID));
            final String url = br.getURL(getDirectDownloadurl(contentID, downloadID)).toString();
            final DownloadLink video = this.createDownloadlink("directhttp://" + url);
            video.setChunks(5);// max chunks by default
            /* Try to set temporary filename. Mimic original filenames as close as possible as we cannot know them in beforehand. */
            if (filenameBase != null) {
                if (fpName != null) {
                    video.setName(filenameBase + "_" + fpName + "_-_" + downloadIDFormatted + ".mp4");
                } else {
                    video.setName(filenameBase + "_-_" + downloadIDFormatted + ".mp4");
                }
            } else if (fpName != null) {
                video.setName(fpName + "_" + downloadIDFormatted + ".mp4");
            }
            final String filesize = br.getRegex("<strong>" + downloadIDFormatted + "</strong></td><td class=\"cell\">[^<]*</td><td class=\"cell\">[^<]*</td><td class=\"cell\">[^<]*</td><td class=\"cell\">(\\d+ Mo)</td>").getMatch(0);
            if (filesize != null) {
                video.setDownloadSize(SizeFormatter.getSize(filesize.replace("o", "b")));
            }
            video.setAvailable(true);
            video.setProperty(DirectHTTP.FORCE_NOCHUNKS, true);
            ret.add(video);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        return ret;
    }

    private String getDirectDownloadurl(final String contentID, final String downloadID) {
        return "/launch-download-2-" + contentID + "-ddl-" + downloadID + ".html";
    }
}
