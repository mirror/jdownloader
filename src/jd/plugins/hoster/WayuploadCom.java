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

import org.jdownloader.plugins.components.TurbobitCore;

import jd.PluginWrapper;
import jd.plugins.HostPlugin;

/** 2019-05-09: This filehost seems to be broken serverside. Download attempts will always result in http response 404. */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class WayuploadCom extends TurbobitCore {
    public WayuploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Keep this up2date! */
    public static String[] domains = new String[] { "wayupload.com" };

    public static String[] getAnnotationNames() {
        return new String[] { domains[0] };
    }

    @Override
    public boolean supports_https() {
        /* 2019-05-09: Special */
        return false;
    }

    @Override
    public boolean supports_mass_linkcheck() {
        /* 2019-05-09: Special - their mass-linkchecker is broken */
        return false;
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
        ret.add("wayupload");
        return ret.toArray(new String[0]);
    }
}