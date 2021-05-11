//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.components.usenet.UsenetServer;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 1, names = { "simply-premium.com" }, urls = { "" })
public class SimplyPremiumCom2 extends HighWayCore {
    // public static interface HighWayMeConfigInterface extends UsenetAccountConfigInterface {
    // };
    public SimplyPremiumCom2(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.simply-premium.com/vip");
    }

    @Override
    public String getAGBLink() {
        return "https://www.simply-premium.com/terms";
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /** According to High-Way staff, Usenet SSL is unavailable since 2017-08-01 */
    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("reader.simply-premium.com", false, 119));
        ret.addAll(UsenetServer.createServerList("reader.simply-premium.com", true, 563));
        return ret;
    }
}