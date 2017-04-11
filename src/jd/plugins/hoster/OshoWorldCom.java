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

import jd.PluginWrapper;
import jd.config.Property;
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

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "oshoworld.com" }, urls = { "https?://(?:www\\.)?oshoworlddecrypted\\.com/[^/]+/.*?\\.asp\\?album_id=\\d+" }) 
public class OshoWorldCom extends PluginForHost {

    public OshoWorldCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    public void correctDownloadLink(final DownloadLink downloadLink) {
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replace("oshoworlddecrypted.com/", "oshoworld.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = this.checkDirectLink(downloadLink, "cache");
        if (dllink == null) {
            final Form f = br.getForm(0);
            InputField i = null;
            for (final InputField inputfield : f.getInputFields()) {
                if ("trackvalue".equals(inputfield.getKey()) && inputfield.getValue().equals(downloadLink.getStringProperty("iFilename", null))) {
                    i = inputfield;
                    break;
                }
            }
            // lets remove all other files execpt the one we want, they are all prechecked.
            while (f.getInputField("trackvalue") != null) {
                f.remove("trackvalue");
            }
            // add the one we want
            f.addInputField(i);
            if (f != null && f.hasInputFieldByName("securityCode")) {
                // has captcha
                final String captcha = new Regex(br.getURL(), "https?://(?:www\\.)?oshoworld\\.com/[^/]+/").getMatch(-1) + "CAPTCHA/CAPTCHA_image.asp";
                final String code = this.getCaptchaCode(captcha, downloadLink);
                f.put("securityCode", Encoding.urlEncode(code));
            }
            br.setFollowRedirects(false);
            br.submitForm(f);
            // redirect show show correct.
            // they come in the form of http://www.oshoarchive.com/ow-english/download.php?id=T1NITy1UaGVfQXJ0X29mX0R5aW5nXzEwLm1wMw
            // the id = base64 iFilename, if we knew the /ow-english/ (for english section) was static we could theoretically bypass
            // captcha..

            dllink = br.getRedirectLocation();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }

        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("cache", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property, null);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                try {
                    // @since JD2
                    con = br2.openHeadConnection(dllink);
                } catch (final Throwable t) {
                    con = br2.openGetConnection(dllink);
                }
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
                } catch (final Throwable t) {
                }
            }
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

}
