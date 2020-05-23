//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.components.UnknownVideohostingCore;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ThevideosGa extends UnknownVideohostingCore {
    public ThevideosGa(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "thevideos.ga" });// standalone domain/site, only hosting embedded content
        return ret;
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        // do not correct URLs from thevideos.ga!
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return UnknownVideohostingCore.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public boolean check_filesize_via_directurl() {
        return true;
    }

    @Override
    protected String getDllink(final DownloadLink link, final boolean isDownload) throws IOException, PluginException, InterruptedException {
        br.setCurrentURL("https://" + this.getHost() + "/" + this.getFID(link));
        final String url = "https://" + this.getHost() + "/stream" + this.getFID(link) + ".mp4";
        br.setFollowRedirects(false);
        br.getPage(url);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return br.getRedirectLocation();
    }

    @Override
    protected String getReCaptchaKey() {
        return null;
    }

    @Override
    protected String getReCaptchaKeyPairing() {
        return null;
    }

    @Override
    protected boolean usePairingMode() {
        /* 2020-05-23: Special */
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
