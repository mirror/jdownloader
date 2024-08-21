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
import jd.nutils.JDHash;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nopremium.pl" }, urls = { "" })
public class NoPremiumPl extends RapideoCore {
    protected static MultiHosterManagement mhm = new MultiHosterManagement("nopremium.pl");

    public NoPremiumPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + getHost() + "/offer");
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/tos";
    }

    @Override
    protected MultiHosterManagement getMultiHosterManagement() {
        return mhm;
    }

    @Override
    protected String getAPIBase() {
        return "https://crypt." + getHost() + "/";
    }

    @Override
    protected String getAPISiteParam() {
        return "nopremium";
    }

    @Override
    protected String getPasswordAPI(Account account) {
        return JDHash.getSHA1(JDHash.getMD5(account.getPass()));
    }

    @Override
    protected boolean useAPI() {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}