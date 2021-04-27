//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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
package org.jdownloader.plugins.components;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

public class YetiShareCoreNew extends YetiShareCore {
    public YetiShareCoreNew(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * other: Special <br />
     */
    @Override
    protected boolean usesNewYetiShareVersion() {
        return true;
    }

    @Override
    public String getPurchasePremiumURL() {
        return this.getMainPage() + "/register";
    }

    protected String getAccountNameSpaceLogin() {
        return "/account/login";
    }

    @Override
    protected String getAccountNameSpaceHome() {
        return "/account";
    }

    @Override
    protected String getAccountNameSpaceUpgrade() {
        return "/upgrade";
    }

    @Override
    protected String getAccountNameSpaceLogout() {
        return "/account/logout";
    }
    /*
     * TODO: 2021-04-22: Seems like all new YetiShare hosts do not require "www." by default. Some don't have a redirect in place and this
     * fail e.g.: https://www.fhscript.com/
     */
    // @Override
    // public boolean requires_WWW() {
    // return false;
    // }

    @Override
    protected boolean isOfflineWebsiteAfterLinkcheck() {
        return this.br.containsHTML(">Status:</span>\\s*<span>\\s*(Deleted|Usunięto)\\s*</span>");
    }

    @Override
    public void checkErrors(Browser br, final DownloadLink link, final Account account) throws PluginException {
        super.checkErrors(br, link, account);
        /* 2020-10-12 */
        final String waittimeBetweenDownloadsStr = br.getRegex(">\\s*You must wait (\\d+) minutes? between downloads").getMatch(0);
        if (waittimeBetweenDownloadsStr != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Wait between downloads", Integer.parseInt(waittimeBetweenDownloadsStr) * 60 * 1001l);
        }
    }
}