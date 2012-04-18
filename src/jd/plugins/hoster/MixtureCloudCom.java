//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.config.Property;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mixturecloud.com" }, urls = { "http://[\\w\\.]*?mixture(cloud|audio|doc|file|image|video)\\.com/(audio|doc|download|image|video)=[A-Za-z0-9]+" }, flags = { 0 })
public class MixtureCloudCom extends PluginForHost {

    // free: 1maxdl * 1 chunk
    // protocol: They have HTTPS certificate but httpd not setup correctly
    // captchatype: recaptcha
    // other: Multiple domains all redirect back to 'sub.mixturecloud.com/' uids
    // are transferable between each (sub)?domain & section. All links have
    // recaptcha with this one size fits all download method.

    public MixtureCloudCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        final String uid = new Regex(link.getDownloadURL(), "mixture(cloud|audio|doc|file|image|video)\\.com/(audio|doc|download|image|video)=(.+)").getMatch(2);
        link.setUrlDownload("http://file.mixturecloud.com/download=" + uid);
    }

    private static final String PREMIUMONLY         = "File access is limited to users with unlimited account<";
    // Neuer lang String für jedes lugin mit sonem Handling? Doof!
    private static final String PREMIUMONLYUSERTEXT = JDL.L("plugins.hoster.mixturecloudcom", "Only downloadable for premium users");

    @Override
    public String getAGBLink() {
        return "http://file.mixturecloud.com/terms";
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://mixturecloud.com/", "lang", "de");
        br.setCookie("http://mixturecloud.com/", "cc", "DE");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>404 Not Found<|The requested document was not found on this server|<h3>Keine Seite unter dieser Adresse</h3>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = null;
        String filesize = null;
        if (br.containsHTML(PREMIUMONLY)) {
            /* you must put a boolean into it and not a string ;) */
            link.setProperty("onlypremium", true);
            filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?) \\- MixtureCloud\\.com").getMatch(0);
            filesize = br.getRegex("<h5>Size : ([^<>\"]*?)</h5>").getMatch(0);
            if (filename != null) {
                link.setName(Encoding.htmlDecode(filename.trim()));
            }
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Download only works with an account", PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
                /* VALUE_ID_PREMIUM_ONLY not existing in old stable */
            }
            /* fallback old method */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Download only works with an account");
        } else {
            /* remove the property */
            link.setProperty("onlypremium", Property.NULL);
            filename = br.getRegex("(?i)<meta property=\"og:title\" content=\"(.*?) mixturecloud\\.com \"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("(?i)<title>(.*?) - mixturecloud\\.com</title>").getMatch(0);
                if (filename == null) filename = br.getRegex("<h2>[\r\n\t]+(.*?)[\r\n\t]+</h2>").getMatch(0);
            }
            filesize = br.getRegex("Originalgröße : <span style=\"font\\-weight:bold\">(.*?)</span>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("(?i)([\\d\\.]+ (MB|GB))").getMatch(0);
            }
        }
        if (filesize == null) {
            logger.warning("MixtureCloud: Couldn't find filesize. Please report this to the JDownloader Development Team.");
            logger.warning("MixtureCloud: Continuing...");
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.postPage(downloadLink.getDownloadURL(), "slow_download=1");
        String reconnectWait = br.getRegex(">Warten auf (\\d+) Sekunden").getMatch(0);
        if (reconnectWait == null) reconnectWait = br.getRegex(">Sie haben bis (\\d+) Sekunden warten").getMatch(0);
        if (reconnectWait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWait) * 1001l);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            for (int i = 0; i <= 5; i++) {
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                rc.setCode(c);
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) continue;
                break;
            }
        } else {
            logger.warning("MixtureCloud: Couldn't find captchatype. If this causes problems, please report this to the JDownloader Development Team.");
            logger.warning("MixtureCloud: Continuing...");
        }
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new DecrypterException(DecrypterException.CAPTCHA);
        String dllink = new Regex(br.toString().replace("\\", ""), "download icon blue \" href=\"(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = "http://file.mixturecloud.com/" + dllink;
        /** Waittime can be skipped */
        // int wait = 20;
        // final String waittime =
        // br.getRegex("jQuery\\(function\\(\\)\\{[\t\n\r ]+for \\(i=(\\d+);i > ").getMatch(0);
        // if (waittime != null) wait = Integer.parseInt(waittime);
        // sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL().contains("mixturecloud.com/downloadunavailable=") || br.containsHTML("Die Datei existiert nicht")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.getURL().equals("http://www.mixturecloud.com/")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public boolean canHandle(final DownloadLink dl, final Account account) {
        if (account == null && dl.getBooleanProperty("onlypremium")) {
            /* this host only supports premium downloads */
            return false;
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}