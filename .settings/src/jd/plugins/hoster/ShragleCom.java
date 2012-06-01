//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 16628 $", interfaceVersion = 2, names = { "OUTDATEDshragle.com" }, urls = { "NEVERUSEMEAGAINIMOUTDATED" }, flags = { 0 })
public class ShragleCom extends PluginForHost {

    public ShragleCom(final PluginWrapper wrapper) {
        super(wrapper);

    }

    @Override
    public String getAGBLink() {
        return "http://www.shragle.com/about/imprint";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException, IOException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}