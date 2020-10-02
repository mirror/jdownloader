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
package jd.plugins.hoster;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "doci.pl" }, urls = { "docidecrypted://.+" })
public class DociPl extends PluginForHost {
    public DociPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://doci.pl/regulations-pl";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("docidecrypted://", "https://"));
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);
    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 410 });
        return br;
    }

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        setDownloadlinkInformation(this.br, link);
        return AvailableStatus.TRUE;
    }

    public static void setDownloadlinkInformation(final Browser br, final DownloadLink link) {
        final String url_filename = new Regex(link.getDownloadURL(), "[^:/]+://[^/]+/(.+)").getMatch(0).replace("/", "_") + ".pdf";
        String filename = br.getRegex("class=\"content\"\\s*?><section><h1>([^<>\"]+)</h1>").getMatch(0);
        if (filename == null) {
            /* Fallback */
            filename = url_filename;
        }
        String filesize = br.getRegex("<td>\\s*Rozmiar\\s*:\\s*</td>\\s*<td>\\s*([^<>\"]+)\\s*<").getMatch(0);
        filename = Encoding.htmlDecode(filename).trim();
        link.setName(filename);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
    }

    public static String getDocumentID(final Browser br) {
        String docid = br.getRegex("id=\"file\\-download\"[^<>]*?data\\-file\\-id=(\\d+)").getMatch(0);
        if (docid == null) {
            docid = br.getRegex("stream\\.[^/]+/pdf/(\\d+)").getMatch(0);
        }
        return docid;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            // final String docid = getDocumentID(this.br);
            final String fidStr = br.getRegex("data-file-id=(\\d+)").getMatch(0);
            if (fidStr == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final int fid = Integer.parseInt(fidStr);
            final boolean useNewWay = true;
            if (useNewWay) {
                /* 2020-10-02 */
                final Map<String, Object> postData = new HashMap<String, Object>();
                postData.put("code", "");
                postData.put("download_from_dir", 0);
                postData.put("file_id", fid);
                postData.put("item_id", fid);
                postData.put("item_type", 1);
                postData.put("menu_visible", 0);
                postData.put("no_headers", 1);
                br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                br.postPageRaw("/download/payment_info", JSonStorage.serializeToJson(postData));
                dllink = PluginJSonUtils.getJson(br, "download_url");
                final String time = PluginJSonUtils.getJson(br, "time");
                if (StringUtils.isEmpty(dllink) || StringUtils.isEmpty(time)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink += fidStr + "/" + time;
            } else {
                /* This seems to be needed to view a document on thei website - not (yet) useful for downloading! */
                final String rcKey = br.getRegex("data-rcp=\"([^\"]+)\"").getMatch(0);
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, rcKey).getToken();
                final Map<String, Object> postData = new HashMap<String, Object>();
                postData.put("file_id", fidStr);
                postData.put("file_size", link.getView().getBytesTotal());
                postData.put("file_extension", Plugin.getFileNameExtensionFromString(link.getName(), "mobi"));
                postData.put("rc", recaptchaV2Response);
                br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                br.postPageRaw("/file/file_data/show", JSonStorage.serializeToJson(postData));
                dllink = PluginJSonUtils.getJson(br, "url");
                if (!StringUtils.isEmpty(dllink)) {
                    /* Check for: docs.google.com/viewer?embedded=true&url=http... */
                    final UrlQuery query = UrlQuery.parse(dllink);
                    final String embeddedURL = query.get("url");
                    if (!StringUtils.isEmpty(embeddedURL)) {
                        dllink = embeddedURL;
                    }
                }
            }
            /* 2020-09-21: Old way without captcha was easier */
            // dllink = String.format("http://stream.%s/pdf/%s", this.br.getHost(), docid);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        }
        final String final_server_filename = getFileNameFromHeader(dl.getConnection());
        if (final_server_filename != null) {
            link.setFinalFileName(Encoding.htmlDecode(final_server_filename));
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    link.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                link.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}