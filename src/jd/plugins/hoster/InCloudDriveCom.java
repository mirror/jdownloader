//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "inclouddrive.com" }, urls = { "https?://(www\\.)?inclouddrive\\.com/(link_download/\\?token=[A-Za-z0-9=_]+|(#/)?(file_download|file|link)/[0-9a-zA-Z=_-]+)" }, flags = { 0 })
public class InCloudDriveCom extends PluginForHost {

    // DEV NOTE:
    // links are not correctable to a standard url format

    public InCloudDriveCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.inclouddrive.com/#/terms_condition";
    }

    private String[] hashTag;
    private Browser  ajax = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        try {
            br.setAllowedResponseCodes(400, 500);
        } catch (final Throwable e) {
        }
        br.getPage(link.getDownloadURL());
        setFUID(link);
        if ("link".equals(hashTag[0])) {
            ajaxPostPage("https://www.inclouddrive.com/index.php/link", "user_id=&user_loged_in=no&link_value=" + Encoding.urlEncode(hashTag[1]));
        } else {
            ajaxPostPage("https://www.inclouddrive.com/index.php/" + hashTag[0] + "/" + hashTag[1], "user_id=");
        }
        if (ajax.containsHTML(">A Database Error Occurred<|This link has been removed from system.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = ajax.getRegex("class=\"propreties-file-count\">[\t\n\r ]+<b>([^<>\"]+)</b>").getMatch(0);
        final String filesize = ajax.getRegex(">Total size:</span><span class=\"propreties-dark-txt\">([^<>\"]+)</span>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(encodeUnicode(Encoding.htmlDecode(filename.trim())));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            final String uplid = ajax.getRegex("uploader_id=\"(\\d+)\"").getMatch(0);
            final String fileid = ajax.getRegex("file_id=\"(\\d+)\"").getMatch(0);
            if (uplid == null || fileid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ajaxPostPage("https://www.inclouddrive.com/index.php/download_page_captcha", "type=yes");
            final int repeat = 5;
            for (int i = 1; i <= repeat; i++) {
                final String code = getCaptchaCode("https://www.inclouddrive.com/captcha/php/captcha.php", downloadLink);
                ajaxPostPage("https://www.inclouddrive.com/captcha/php/check_captcha.php", "captcha_code=" + Encoding.urlEncode(code));
                if (ajax.toString().equals("not_match") && i + 1 != repeat) {
                    continue;
                } else if (ajax.toString().equals("not_match") && i + 1 == repeat) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    break;
                }
            }
            ajaxPostPage("https://www.inclouddrive.com/index.php/get_download_server/download_page_link", "contact_id=" + uplid + "&table_id=" + fileid);
            dllink = ajax.toString();
            if (dllink == null || !dllink.startsWith("http")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // dllink = Encoding.htmlDecode(dllink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private void setFUID(final DownloadLink dl) throws PluginException {
        hashTag = new Regex(br.getURL(), "/(link_download)/\\?token=([A-Za-z0-9=_]+)").getRow(0);
        if (hashTag == null) {
            hashTag = new Regex(br.getURL(), "/(?:#/)?(file_download|file|link)/([0-9a-zA-Z_=-]+)").getRow(0);
        }
        if (hashTag == null || hashTag.length != 2) {
            logger.warning("Can not determin hashTag");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (hashTag[1] != null) {
            try {
                dl.setLinkID(hashTag[1]);
            } catch (final Throwable e) {
            }
        }
    }

    private void ajaxPostPage(final String url, final String param) throws Exception {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("Connection-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.postPage(url, param);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    /* Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}