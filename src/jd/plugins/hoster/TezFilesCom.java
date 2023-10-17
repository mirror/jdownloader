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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdownloader.plugins.components.config.Keep2shareConfig;
import org.jdownloader.plugins.components.config.Keep2shareConfigTezfiles;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginDependencies;
import jd.plugins.decrypter.Keep2ShareCcDecrypter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { Keep2ShareCcDecrypter.class })
public class TezFilesCom extends K2SApi {
    public TezFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(Keep2ShareCcDecrypter.domainsTezfilesAndPublish2);
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        final String[] supportedDomains = buildSupportedNames(getPluginDomains());
        final String[] supportedNamesExtra = new String[] { "tezfiles", "publish2" };
        final String[] supportedNames = new String[supportedDomains.length + supportedNamesExtra.length];
        int index = 0;
        for (final String domain : supportedDomains) {
            supportedNames[index] = domain;
            index++;
        }
        for (final String supportedNameExtra : supportedNamesExtra) {
            supportedNames[index] = supportedNameExtra;
            index++;
        }
        return supportedNames;
    }

    public static String[] getAnnotationUrls() {
        return K2SApi.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public String rewriteHost(final String host) {
        return this.rewriteHost(getPluginDomains(), host);
    }

    @Override
    protected String getInternalAPIDomain() {
        return "tezfiles.com";
    }

    private static Map<String, Object> HOST_MAP = new HashMap<String, Object>();

    @Override
    protected Map<String, Object> getHostMap() {
        return HOST_MAP;
    }

    @Override
    protected boolean fetchAdditionalAccountInfo(final Account account, final AccountInfo ai, final Browser br, final String auth_token) {
        // untested, had no test account with lifetime status
        return false;
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