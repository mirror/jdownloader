//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
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

import java.util.List;

import org.jdownloader.downloader.hds.HDSDownloader;
import org.jdownloader.plugins.components.hds.HDSContainer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "f4m" }, urls = { "f4ms?://.+?(\\.f4m?(\\?.+)?|$)" })
public class GenericF4M extends PluginForHost {
    public GenericF4M(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.GENERIC };
    }

    @Override
    public String getHost(DownloadLink link, Account account, boolean includeSubdomain) {
        if (link != null) {
            return Browser.getHost(link.getDownloadURL(), includeSubdomain);
        }
        return super.getHost(link, account, includeSubdomain);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        if (link.getPluginPatternMatcher().startsWith("f4m")) {
            final String url = "http" + link.getPluginPatternMatcher().substring(3);
            link.setPluginPatternMatcher(url);
        }
    }

    @Override
    public boolean isResumeable(DownloadLink link, Account account) {
        return true;
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String getReferer(final DownloadLink link) {
        final String referOld = link.getStringProperty("Referer"); // backward compatibility
        if (referOld != null) {
            return referOld;
        } else {
            return link.getReferrerUrl();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        final String cookiesString = downloadLink.getStringProperty("cookies");
        if (cookiesString != null) {
            final String host = Browser.getHost(downloadLink.getPluginPatternMatcher());
            br.setCookies(host, Cookies.parseCookies(cookiesString, host, null));
        }
        final String referer = getReferer(downloadLink);
        if (referer != null) {
            br.getPage(referer);
        }
        br.getPage(downloadLink.getPluginPatternMatcher());
        br.followRedirect();
        final HDSContainer container = HDSContainer.read(downloadLink);
        if (container == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final List<HDSContainer> all = HDSContainer.getHDSQualities(br);
        if (all == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final HDSContainer hit = HDSContainer.getBestMatchingContainer(all, container);
        if (hit == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            hit.write(downloadLink);
            final HDSDownloader dl = new HDSDownloader(downloadLink, br, hit.getFragmentURL());
            this.dl = dl;
            dl.setEstimatedDuration(hit.getDuration());
            dl.startDownload();
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (link != null) {
            link.removeProperty(HDSDownloader.RESUME_FRAGMENT);
        }
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }
}