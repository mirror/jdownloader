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
import org.jdownloader.plugins.components.config.Keep2shareConfigFileboom;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileboom.me" }, urls = { "https?://(?:[a-z0-9\\-]+\\.)?(?:fboom|fileboom)\\.me/(?:file|preview)/([a-z0-9]{13,})(/([^/\\?]+))?(\\?site=([^\\&]+))?" })
public class FileBoomMe extends K2SApi {
    public FileBoomMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected boolean enforcesHTTPS() {
        return true;
    }

    @Override
    public String[] siteSupportedNames() {
        // keep2.cc no dns
        return new String[] { "fileboom.me", "fboom.me" };
    }

    @Override
    protected String getInternalAPIDomain() {
        return "fboom.me";
    }

    @Override
    protected void setConstants(final Account account) {
        super.setConstants(account);
        if (account != null) {
            if (account.getType() == AccountType.FREE) {
                // free account
                chunks = 1;
                resumes = true;
            } else {
                // premium account
                chunks = 0;
                resumes = true;
            }
            logger.finer("setConstants = " + account.getUser() + " @ Account Download :: Type = " + account.getType() + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        } else {
            // free non account
            chunks = 1;
            resumes = true;
            logger.finer("setConstants = Guest Download :: upperChunks = " + chunks + ", Resumes = " + resumes);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        super.resetLink(link);
    }

    @Override
    public Class<? extends Keep2shareConfig> getConfigInterface() {
        return Keep2shareConfigFileboom.class;
    }
}