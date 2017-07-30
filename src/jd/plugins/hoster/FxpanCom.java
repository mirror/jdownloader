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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision: 35304 $", interfaceVersion = 3, names = { "fxpan.com" }, urls = { "https?://(?:www\\.)?fxpan\\.com/(?:file/\\d+|share/[a-z0-9]+)" })
public class FxpanCom extends PluginForHost {

    public FxpanCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://" + getHost() + "/";
    }

    /* Connection stuff */
    private final boolean FREE_RESUME       = false;
    private final int     FREE_MAXCHUNKS    = 1;
    private final int     FREE_MAXDOWNLOADS = -1;

    private String        fuid              = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        fuid = new Regex(link.getDownloadURL(), "/(\\d+)$").getMatch(0);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex(">([^<>]+)</h1>").getMatch(0);
        if (filename == null) {
            /* Fallback */
            filename = fuid;
        }
        String filesize = br.getRegex("文件大小：([\\d\\.]+ [BKMGT])").getMatch(0);
        link.setName(Encoding.htmlDecode(filename).trim());
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        // String dllink = checkDirectLink(downloadLink, directlinkproperty);
        // pointless connection is refused. It's like they ip firewall denyall and then unblock you with a rule onlink generation. if your
        // connection drops from server, the rule is removed and you can't resume (results in long socket timeout).
        String dllink = null;
        if (dllink == null) {
            final Form f = br.getFormbyAction("/v2/pc.php");
            if (f == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (fuid == null) {
                // value will be within form input value
                fuid = f.getInputField("id") != null ? f.getInputField("id").getValue() : null;
            }
            // this will result in redirect
            br.submitForm(f);
            // url should be /downhtml/key/id/hash.html
            {
                // ok should be ajax, not sure if its required
                final Regex regex = new Regex(br, "data\\s*:\\s*'(action=down_process&file_id='\\+file_id\\+'&md5=([a-f0-9]{32})&time=\\d+&t='\\+Math\\.random\\(\\)),");
                // note the md5 reference changes each time you dl.
                String ajaxcontent = regex.getMatch(0);
                if (ajaxcontent != null && fuid != null) {
                    ajaxcontent = ajaxcontent.replaceFirst("'\\+file_id\\+'", fuid).replaceFirst("'\\+Math\\.random\\(\\)", Math.random() + "");
                    final Browser ajax = br.cloneBrowser();
                    ajax.getHeaders().put("Accept", "text/plain, */*; q=0.01");
                    ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    ajax.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    ajax.postPage("/ajax.php", ajaxcontent);
                }
            }
            // another request
            final String next = br.getRegex("\"([^\"]+/down\\.php\\?[^\"]+)").getMatch(0);
            if (next == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(next);
            // final link is mentioned numerous times (3 times, same url)
            dllink = br.getRegex("delayURL\\(\"(.*?)\"\\)").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // short wait
            final String wait = br.getRegex("<span id=\"time\">(\\d+)</span>").getMatch(0);
            if (wait == null) {
                sleep(10500l, downloadLink);
            } else {
                sleep(Integer.parseInt(wait) * 1031l, downloadLink);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
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
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_ChineseFileHosting;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}