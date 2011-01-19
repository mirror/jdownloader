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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "duckload.com" }, urls = { "http://[\\w\\.]*?duckload\\.com/(download/[a-z0-9]+(/.+)?|(divx|play|dl)/[a-zA-Z0-9\\.-]+|[a-zA-Z0-9\\.]+\\.html)" }, flags = { 2 })
public class DuckLoad extends PluginForHost {

    private static final String UA = RandomUserAgent.generate();

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
            this.br.getHeaders().put("User-Agent", UA);
            final StringBuilder sb = new StringBuilder();
            for (final DownloadLink d : urls) {
                if (sb.length() > 0) {
                    sb.append(";");
                }
                sb.append(d.getDownloadURL());
            }
            this.br.postPage("http://www.duckload.com/jDownloader/checkOnlineStatus.php", "list=" + Encoding.urlEncode(sb.toString()));
            final String[] results = Regex.getLines(this.br.toString());
            for (int i = 0; i < results.length; i++) {
                final String result = results[i];
                final DownloadLink dllink = urls[i];
                final String[] values = result.split("\\;\\s*");
                this.trim(values);
                if ("SUCCESS".equals(values[0])) {
                    dllink.setAvailable(true);
                    if (values.length > 2) {
                        final long size = Long.parseLong(values[2]);
                        final String name = values[3];
                        final String md5 = values[4];
                        dllink.setMD5Hash(md5);
                        dllink.setFinalFileName(name);
                        dllink.setDownloadSize(size);
                    } else {
                        dllink.setAvailable(false);
                    }
                } else {
                    dllink.setAvailable(false);
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        this.br.postPage("http://www.duckload.com/jDownloader/getAccountDetails.php", "jd_uname=" + Encoding.urlEncode(account.getUser()) + "&jd_pass=" + Encoding.urlEncode(account.getPass()));
        final String[] data = this.br.toString().split("\\;\\s*");
        this.trim(data);
        if (data.length != 5 || !"SUCCESS".equals(data[0])) {
            ai.setStatus("Invalid");
            account.setValid(false);
            return ai;
        }
        // int accountID = Integer.parseInt(data[1]);
        final boolean isPremium = "true".equals(data[3]);
        final long validUntil = Long.parseLong(data[4]);
        ai.setValidUntil(validUntil * 1000);
        if (isPremium && !ai.isExpired()) {
            account.setValid(true);
            ai.setStatus("Valid");
            ai.setUnlimitedTraffic();
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
        br.forceDebug(true);
        this.requestFileInformation(downloadLink);
        final String[] values = this.br.postPage("http://www.duckload.com/jDownloader/getFree.php", "link=" + Encoding.urlEncode(downloadLink.getDownloadURL())).toString().split("\\;\\s*");
        this.trim(values);
        if (values.length != 3 || !"SUCCESS".equals(values[0])) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, values[0]); }
        this.sleep(Long.parseLong(values[1]) * 1000l, downloadLink);

        final String finallink = this.br.postPage("http://www.duckload.com/jDownloader/getFreeEncrypt.php", "crypt=" + values[2]);
        if (!finallink.startsWith("http://") || finallink.startsWith("ERROR; ")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, finallink); }
        this.dl = BrowserAdapter.openDownload(this.br, downloadLink, finallink, true, 1);
        if (!this.dl.getConnection().isContentDisposition()) {
            this.br.followConnection();
            if ("http://www.duckload.com/".equals(br.getURL())) {
                br.postPage("http://www.duckload.com/jDownloader/reportError.php", "link=" + Encoding.urlEncode(downloadLink.getDownloadURL()) + "&premium=false&msg=redirectToHome");

                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Servererror. Try later!");

            }
            if (this.br.getRequest().getUrl().toString().contentEquals("http://" + this.br.getHost() + "/")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        this.requestFileInformation(downloadLink);
        this.br.forceDebug(true);
        this.br.setFollowRedirects(true);
        final String link = downloadLink.getDownloadURL();
        String finalLink = downloadLink.getStringProperty("finallink", null);
        downloadLink.setProperty("finallink", null);
        boolean usedCachedLink = false;
        if (finalLink != null) {
            try {
                /* try saved one first */
                this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, finalLink, true, 0);
                if (!this.dl.getConnection().isContentDisposition()) {
                    /* saved link no longer valid */
                    usedCachedLink = false;
                } else {
                    /* saved link valid and use it */
                    usedCachedLink = true;
                }
            } catch (final Throwable e) {
                usedCachedLink = false;
            } finally {
                if (usedCachedLink == false) {
                    try {
                        this.dl.getConnection().disconnect();
                    } catch (final Throwable e2) {
                    }
                }
            }

        }
        if (usedCachedLink == false) {
            /* create new link */
            finalLink = this.br.postPage("http://www.duckload.com/jDownloader/getPremium.php", "link=" + Encoding.urlEncode(link) + "&jd_uname=" + Encoding.urlEncode(account.getUser()) + "&jd_pass=" + Encoding.urlEncode(account.getPass()));
            if (!finalLink.startsWith("http:") || finalLink.startsWith("ERROR; ")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, finalLink); }
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, finalLink, true, 0);
            if (!this.dl.getConnection().isContentDisposition()) {
                this.br.followConnection();
                if ("http://www.duckload.com/".equals(br.getURL())) {
                    br.postPage("http://www.duckload.com/jDownloader/reportError.php", "link=" + Encoding.urlEncode(downloadLink.getDownloadURL()) + "&premium=true&msg=redirectToHome");

                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Servererror. Try later!");

                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        downloadLink.setProperty("finallink", finalLink);
        this.dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) { return AvailableStatus.UNCHECKABLE; }
        if (!link.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return link.getAvailableStatus();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.setProperty("finallink", null);
    }

    private void trim(final String[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
        }
    }
}