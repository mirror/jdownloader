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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.KhinsiderCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KhinsiderComCrawler extends PluginForDecrypt {
    public KhinsiderComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "khinsider.com" });
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

    private static final String PATTERN_ALBUM_DOWNLOAD            = "https?://downloads\\.%s/cp/add_album_TODO_FIXME_/(\\d+)$";
    private static final String PATTERN_ALBUM_STREAM              = "https://downloads\\.%s/([^/]+)/album/([\\w\\-]+)$";
    private static final String PATTERN_ALBUM_SINGLE_TRACK_STREAM = "https://downloads\\.%s/([^/]+)/album/([\\w\\-]+)/([^/#\\?]+)";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            final String hostsPattern = buildHostsPatternPart(domains);
            ret.add(String.format(PATTERN_ALBUM_DOWNLOAD, hostsPattern) + "|" + String.format(PATTERN_ALBUM_STREAM, hostsPattern) + "|" + String.format(PATTERN_ALBUM_SINGLE_TRACK_STREAM, hostsPattern));
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final KhinsiderCom hosterplugin = (KhinsiderCom) this.getNewPluginForHostInstance(this.getHost());
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        final String thishost = this.getHost();
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final Pattern singleTrackPattern = Pattern.compile(String.format(PATTERN_ALBUM_SINGLE_TRACK_STREAM, Pattern.quote(thishost)));
        final Pattern officialAlbumDownloadPattern = Pattern.compile(String.format(PATTERN_ALBUM_DOWNLOAD, Pattern.quote(thishost)));
        final Regex albumDownload = new Regex(contenturl, officialAlbumDownloadPattern);
        final Regex albumStream = new Regex(contenturl, String.format(PATTERN_ALBUM_STREAM, Pattern.quote(thishost)));
        final Regex albumSingleTrackStream = new Regex(contenturl, singleTrackPattern);
        if (albumDownload.patternFind() && account == null) {
            throw new AccountRequiredException();
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (account != null) {
            hosterplugin.login(account, false);
        }
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            /* Check if we got a direct-URL. */
            con = br.openGetConnection(contenturl);
            if (this.looksLikeDownloadableContent(con)) {
                final DownloadLink direct = getCrawler().createDirectHTTPDownloadLink(br.getRequest(), con);
                ret.add(direct);
            } else {
                br.followConnection();
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (albumDownload.patternFind()) {
            if (br.containsHTML(">\\s*Unfortunately, due to large server expenses we are no longer able")) {
                /* Donator account required to download this content */
                throw new AccountRequiredException();
            }
        } else if (albumStream.patternFind()) {
            final String albumTitleSlug = albumStream.getMatch(1);
            String officialAlbumDownloadurl = null;
            final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
            for (final String url : urls) {
                if (new Regex(url, singleTrackPattern).patternFind()) {
                    ret.add(this.createDownloadlink(url));
                } else if (new Regex(url, officialAlbumDownloadPattern).patternFind()) {
                    officialAlbumDownloadurl = url;
                }
            }
            if (officialAlbumDownloadurl != null && account != null && account.getType() == AccountType.PREMIUM) {
                /* Paid / Premium / Donator account available -> Return only official .zip downloadlink. */
                ret.clear();
                ret.add(this.createDownloadlink(officialAlbumDownloadurl));
            }
            final String urlThumbnail = br.getRegex("href=\"(https?://[^\"]+/folder\\.[a-z0-9]+)\" target=\"_blank\">").getMatch(0);
            if (urlThumbnail != null) {
                final DownloadLink thumbnail = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(urlThumbnail));
                thumbnail.setAvailable(true);
                ret.add(thumbnail);
            }
            String title = br.getRegex("<h2>([^<]+)</h2>").getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            if (title != null) {
                title = Encoding.htmlDecode(title).trim();
                fp.setName(title);
            } else {
                /* Fallback */
                fp.setName(albumTitleSlug.replace("-", " ").trim());
            }
            fp.addLinks(ret);
        } else if (albumSingleTrackStream.patternFind()) {
            final String[] directurls = br.getRegex("\"(https?://[^\"]+)\"><span class=\"songDownloadLink\"").getColumn(0);
            final String[] filesizes = br.getRegex("\\((\\d+\\.\\d{2} (?:KB|MB|GB))\\)").getColumn(0);
            int index = 0;
            for (final String directurl : directurls) {
                final DownloadLink direct = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl));
                final String filenameFromURL = Plugin.getFileNameFromURL(directurl);
                if (filenameFromURL != null) {
                    /*
                     * URL may redirect to another URL so we are in danger of loosing the track number which is usually containing in the
                     * filenames we can see at this stage.
                     */
                    if (StringUtils.endsWithCaseInsensitive(filenameFromURL, "flac") || StringUtils.endsWithCaseInsensitive(filenameFromURL, "mp3")) {
                        /* Use filename we currently have as final filename. */
                        direct.setFinalFileName(filenameFromURL);
                        direct.setProperty(DirectHTTP.FIXNAME, filenameFromURL);
                    } else {
                        /* Allow filename to change later. */
                        direct.setName(filenameFromURL);
                    }
                }
                /* Set filesize if possible */
                if (filesizes != null && filesizes.length == directurls.length) {
                    final String filesizeStr = filesizes[index];
                    direct.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                }
                direct.setAvailable(true);
                ret.add(direct);
                index++;
            }
        } else {
            /* Unsupported URL -> Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}
