//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "allmyvideos.net" }, urls = { "https?://(www\\.)?allmyvideos\\.net/((?:embed-)?[a-z0-9]{12}|v/v\\-[A-Za-z0-9]+)" })
public class AllMyVideosNet extends PluginForHost {

    public AllMyVideosNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.allmyvideos.net/tos.html";
    }

    private static final String  TYPE_OLD             = "https?://(www\\.)?allmyvideos\\.net/v/v\\-[A-Za-z0-9]+";

    private static final boolean SUPPORTSHTTPS        = false;
    private static final boolean SUPPORTSHTTPS_FORCED = false;

    /* Linktypes */
    private static final String  TYPE_NORMAL          = "https?://[A-Za-z0-9\\-\\.]+/[a-z0-9]{12}";
    private static final String  TYPE_EMBED           = "https?://[A-Za-z0-9\\-\\.]+/embed\\-[a-z0-9]{12}";

    private static final String  COOKIE_HOST          = "http://allmyvideos.net";
    private static final String  NICE_HOST            = COOKIE_HOST.replaceAll("(https://|http://)", "");

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String fuid = getFUIDFromURL(link);
        final String protocol;
        /* link cleanup, prefer https if possible */
        if (SUPPORTSHTTPS || SUPPORTSHTTPS_FORCED) {
            protocol = "https://";
        } else {
            protocol = "http://";
        }
        if (link.getDownloadURL().matches(TYPE_EMBED)) {
            final String corrected_downloadurl = protocol + NICE_HOST + "/" + fuid;
            final String url_embed = protocol + NICE_HOST + "/embed-" + fuid + ".html";
            /* Make sure user gets the kind of content urls that he added to JD. */
            link.setContentUrl(url_embed);
            link.setUrlDownload(corrected_downloadurl);
        }
    }

    /**
     * This site uses a modified XFS script
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        correctDownloadLink(downloadLink);
        this.setBrowserExclusive();
        br.setCookie("http://allmyvideos.net/", "lang", "english");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (downloadLink.getDownloadURL().matches(TYPE_OLD)) {
            /* Stupid - strange linktype embeds real XFS-id */
            final String real_id = br.getRegex("/builtin\\-([a-z0-9]{12})\\.html").getMatch(0);
            if (real_id == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String new_url = "http://allmyvideos.net/" + real_id;
            downloadLink.setUrlDownload(new_url);
            br.getPage(new_url);
        }
        if (br.containsHTML(">File not found<|Reason of deletion:|Reason for deletion")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("type=\"hidden\" name=\"fname\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Form download1 = getFormByKey("op", "download1");
        if (download1 != null) {
            br.submitForm(download1);
        }
        dllink = br.getRegex("\"file\" : \"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -4);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // TODO: remove this when v2 becomes stable. use br.getFormbyKey(String key, String value)
    /**
     * Returns the first form that has a 'key' that equals 'value'.
     *
     * @param key
     * @param value
     * @return
     */
    private Form getFormByKey(final String key, final String value) {
        Form[] workaround = br.getForms();
        if (workaround != null) {
            for (Form f : workaround) {
                for (InputField field : f.getInputFields()) {
                    if (key != null && key.equals(field.getKey())) {
                        if (value == null && field.getValue() == null) {
                            return f;
                        }
                        if (value != null && value.equals(field.getValue())) {
                            return f;
                        }
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private String getFUIDFromURL(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([a-z0-9]{12})$").getMatch(0);
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}