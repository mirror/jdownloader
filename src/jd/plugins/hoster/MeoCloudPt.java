//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.MeocloudPtFolder;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class MeoCloudPt extends PluginForHost {
    public MeoCloudPt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://meocloud.pt/";
    }

    public static List<String[]> getPluginDomains() {
        return MeocloudPtFolder.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (int i = 0; i < pluginDomains.size(); i++) {
            /* Add dummy entries as all items get added via crawler -> No pattern needed here. */
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    private final String PROPERTY_DIRECTURL = "directurl";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getPluginPatternMatcher());
            if (this.looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                link.setProperty(PROPERTY_DIRECTURL, con.getURL().toString());
            } else {
                br.followConnection(true);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        if (MeocloudPtFolder.isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("/dl/zipdir/[a-z0-9\\-]+/.*?/([^<>\"/]*?)\\?(public|download)=").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"pick_file\" value=\"/([^<>\"]*?)\">").getMatch(0);
        }
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final Form pwform = MeocloudPtFolder.getPasswordProtectedForm(br);
        if (pwform != null) {
            /* 2020-02-18: PW protected URLs are not yet supported. */
            link.setPasswordProtected(true);
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected links are not yet supported: Contact support and ask for implementation", 8 * 60 * 1000l);
        } else {
            link.setPasswordProtected(false);
        }
        String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (dllink == null) {
            /* Backwards compatibility for items added before/until inclding revision 41776. */
            dllink = br.getRegex("\"(https?://[a-z0-9\\.]+/dl/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                final String publ = new Regex(link.getDownloadURL(), "meocloud\\.pt/link/([a-z0-9\\-]+)/").getMatch(0);
                dllink = "https://cld.pt/dl/download/" + publ + "/" + Encoding.urlEncode(link.getName()) + "?public=" + publ + "&download=true";
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}