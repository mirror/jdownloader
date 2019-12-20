//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
//
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.plugins.HostPlugin;

import org.jdownloader.plugins.components.TurbobitCore;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class TurboBitNet extends TurbobitCore {
    public TurboBitNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Keep this up2date! */
    public static String[]   domains      = new String[] { "turbobit.net", "ifolder.com.ua", "turo-bit.net", "depositfiles.com.ua", "dlbit.net", "hotshare.biz", "sibit.net", "turbobit.ru", "xrfiles.ru", "turbabit.net", "filedeluxe.com", "filemaster.ru", "файлообменник.рф", "turboot.ru", "kilofile.com", "twobit.ru", "forum.flacmania.ru", "filhost.ru", "fayloobmennik.com", "rapidfile.tk", "turbo.to", "cccy.su", "turbo-bit.net", "turbobit.cc", "turbobit.pw", "turbo.to" };
    /* Setting domains */
    protected final String[] user_domains = new String[] { "turbo.to", "turbobit.net", "turbobit.pw" };

    @Override
    protected String getConfiguredDomain() {
        String ret = null;
        final String default_domain = "turbo.to";
        final int selected_domain_index = getPluginConfig().getIntegerProperty(TurbobitCore.SETTING_PREFERRED_DOMAIN, -1);
        if (selected_domain_index == 0) {
            ret = default_domain;
        } else {
            try {
                ret = user_domains[selected_domain_index];
            } catch (final Throwable de) {
                /* Return default */
                ret = default_domain;
            }
        }
        return ret;
    }

    public static String[] getAnnotationNames() {
        return new String[] { domains[0] };
    }

    @Override
    public int minimum_pre_download_waittime_seconds() {
        /* 2019-05-11 */
        return 60;
    }

    /**
     * returns the annotation pattern array
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/([A-Za-z0-9]+(/[^<>\"/]*?)?\\.html|download/free/[a-z0-9]+|/?download/redirect/[A-Za-z0-9]+/[a-z0-9]+)" };
    }

    private static String getHostsPattern() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        final String hosts = "https?://(?:www\\.|new\\.|m\\.)?" + "(?:" + pattern.toString() + ")";
        return hosts;
    }

    @Override
    public String[] siteSupportedNames() {
        final List<String> ret = new ArrayList<String>(Arrays.asList(domains));
        ret.add("turbobit");
        return ret.toArray(new String[0]);
    }
    // @Override
    // protected void setConfigElements() {
    // // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(),
    // // TurbobitCore.SETTING_PREFERRED_DOMAIN, user_domains, "Select preferred domain").setDefaultValue(0));
    // super.setConfigElements();
    // }
}