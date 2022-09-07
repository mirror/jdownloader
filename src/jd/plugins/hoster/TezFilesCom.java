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
import org.jdownloader.plugins.components.config.Keep2shareConfigTezfiles;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tezfiles.com" }, urls = { "https?://(?:[a-z0-9\\-]+\\.)?(?:tezfiles\\.com|publish2\\.me)/(?:f(?:ile)?|preview)/([a-z0-9]{13,})(/([^/\\?]+))?(\\?site=([^\\&]+))?" })
public class TezFilesCom extends K2SApi {
    public TezFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "publish2.me".equals(host)) {
            return getHost();
        } else {
            return super.rewriteHost(host);
        }
    }

    @Override
    protected String getInternalAPIDomain() {
        return "tezfiles.com";
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
    public String[] siteSupportedNames() {
        return new String[] { "tezfiles.com", "publish2.me" };
    }

    @Override
    protected void handleErrors(Account account, DownloadLink downloadLink, Browser br, String brString, boolean subErrors) throws PluginException {
        try {
            super.handleErrors(account, downloadLink, br, brString, subErrors);
        } catch (AccountRequiredException e) {
            if (account != null && downloadLink != null) {
                throw e;
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public Class<? extends Keep2shareConfig> getConfigInterface() {
        return Keep2shareConfigTezfiles.class;
    }
}