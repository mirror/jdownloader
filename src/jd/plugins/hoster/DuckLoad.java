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

package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "duckload.com" }, urls = { "http://[\\w\\.]*?(duckload\\.com|youload\\.to)/(download/[a-z0-9]+(/.+)?|(divx|play|dl)/[a-zA-Z0-9\\.-]+|[a-zA-Z0-9\\.]+\\.html)" }, flags = { 2 })
public class DuckLoad extends PluginForHost {

    public DuckLoad(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.duckload.com/Shop&duck=220191");
        this.setStartIntervall(3000l);
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {

            this.setBrowserExclusive();
            this.br.getHeaders().put("User-Agent", RandomUserAgent.generate());
            final StringBuilder sb = new StringBuilder();

            for (DownloadLink d : urls) {
                if (sb.length() > 0) sb.append(";");

                sb.append(d.getDownloadURL());
            }

            String[] results = Regex.getLines(br.postPage("http://www.duckload.com/jDownloader/checkOnlineStatus.php", "list=" + sb.toString()).toString());
            for (int i = 0; i < results.length; i++) {
                String result = results[i];
                DownloadLink dllink = urls[i];
                String[] values = result.split("\\;\\s*");
                if ("SUCCESS".equals(values[0])) {
                    dllink.setAvailable(true);
                    if (values.length > 2) {
                        long size = Long.parseLong(values[2]);
                        String name = values[3];
                        String md5 = values[4];

                        dllink.setMD5Hash(md5);
                        dllink.setName(name);
                        dllink.setDownloadSize(size);
                    }
                } else {
                    dllink.setAvailable(false);

                }
            }
            // for (final DownloadLink dllink : links) {
            // final String id = this.getID(dllink);
            // final String hit[] =
            // br.getRegex("source\">.*?<span>.*?filesonic.com/file/" + id +
            // ".*?fileName\">.*?<span>(.*?)<.*?fileSize\">.*?<span>(.*?)<").getRow(0);
            // if (hit != null && hit.length == 2 && hit[1].length() > 2) {
            // dllink.setFinalFileName(hit[0].trim());
            // dllink.setDownloadSize(Regex.getSize(hit[1]));
            // dllink.setAvailable(true);
            // } else {
            // dllink.setAvailable(false);
            // }
            // }

        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();

        br.postPage("http://www.duckload.com/jDownloader/getAccountDetails.php", "jd_uname=" + account.getUser() + "&jd_pass=" + account.getPass());

        String[] data = br.toString().split("\\;\\s*");

        if (!"SUCCESS".equals(data[0])) {
            ai.setStatus("Logins are wrong");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        // int accountID = Integer.parseInt(data[1]);
        boolean isPremium = "true".equals(data[3]);
        long validUntil = Long.parseLong(data[4]);
        ai.setValidUntil(validUntil * 1000);
        if (isPremium) {
            account.setValid(true);
            ai.setStatus("Valid");
        } else {
            ai.setStatus("No Premium Account");
            account.setValid(false);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://duckload.com/impressum.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        this.requestFileInformation(downloadLink);

        String[] values = br.postPage("http://www.duckload.com/jDownloader/getFree.php", "link=" + downloadLink.getDownloadURL()).toString().split("\\;\\s*");

        if (!"SUCCESS".equals(values[0])) throw new PluginException(LinkStatus.ERROR_FATAL, values[0]);
        this.sleep(Long.parseLong(values[1]) * 1000l, downloadLink);

        String finallink = br.postPage("http://www.duckload.com/jDownloader/getFreeEncrypt.php", "crypt=" + values[2]);
        if (br.toString().startsWith("ERROR; ")) { throw new PluginException(LinkStatus.ERROR_FATAL, br.toString().substring(7)); }
        this.dl = BrowserAdapter.openDownload(this.br, downloadLink, finallink, false, -2);
        if (!dl.getConnection().isContentDisposition()) {
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // this.dl.setFilenameFix(true);
        this.dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        this.requestFileInformation(downloadLink);

        this.br.forceDebug(true);
        final String link = downloadLink.getDownloadURL();
        if (link == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        this.br.setFollowRedirects(true);
        /*
         * Used cached link. it can be used for resume as long as ip did not
         * change
         */
        String finalLink = downloadLink.getStringProperty("finallink", null);
        boolean usedCachedLink = true;
        if (finalLink == null) {
            usedCachedLink = false;
            br.postPage("http://www.duckload.com/jDownloader/getPremium.php", "link=" + link + "&jd_uname=" + account.getUser() + "&jd_pass=" + account.getPass());

            if (br.toString().startsWith("ERROR; ")) { throw new PluginException(LinkStatus.ERROR_FATAL, br.toString().substring(7)); }
            finalLink = br.toString();
            downloadLink.setProperty("finallink", finalLink);
        }
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, finalLink, true, 0);
        if (!this.dl.getConnection().isContentDisposition()) {
            this.logger.warning("The final dllink seems not to be a file!");
            this.br.followConnection();
            if (this.br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>)")) {
                //
                if (usedCachedLink) {
                    downloadLink.setProperty("finallink", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {

        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) return AvailableStatus.UNCHECKABLE;
        return link.getAvailableStatus();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.setProperty("finallink", null);

    }
}