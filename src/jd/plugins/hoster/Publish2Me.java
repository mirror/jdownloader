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

import org.jdownloader.plugins.components.config.Keep2shareConfig;
import org.jdownloader.plugins.components.config.Keep2shareConfigPublish2;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "publish2.me" }, urls = { "https?://(?:[a-z0-9\\-]+\\.)?publish2\\.me/file/([a-z0-9]{13,})(/([^/\\?]+))?(\\?site=([^\\&]+))?" })
public class Publish2Me extends K2SApi {
    public Publish2Me(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "publish2.me" };
    }

    @Override
    protected String getInternalAPIDomain() {
        return "publish2.me";
    }

    @Override
    protected void setConstants(final Account account) {
        if (account != null) {
            if (account.getType() == AccountType.FREE) {
                // free account
                chunks = 1;
                resumes = true;
                isFree = true;
                directlinkproperty = "freelink2";
            } else {
                // premium account
                chunks = 0;
                resumes = true;
                isFree = false;
                directlinkproperty = "premlink";
            }
            logger.finer("setConstants = " + account.getUser() + " @ Account Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        } else {
            // free non account
            chunks = 1;
            resumes = true;
            isFree = true;
            directlinkproperty = "freelink1";
            logger.finer("setConstants = Guest Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        }
    }

    @Override
    protected String getReCaptchaV2WebsiteKey() {
        return "6LcYcN0SAAAAABtMlxKj7X0hRxOY8_2U86kI1vbb";
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public Class<? extends Keep2shareConfig> getConfigInterface() {
        return Keep2shareConfigPublish2.class;
    }
}