//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class XupIn extends PluginForHost {
    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "xup.in", "xup.to" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/dl,(\\d+)(/([^/]+)/?)?");
        }
        return ret.toArray(new String[0]);
    }

    public XupIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://www.xup.in/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
            final String fid = urlinfo.getMatch(0);
            final String filenameURL = urlinfo.getMatch(2);
            if (filenameURL != null) {
                link.setName(filenameURL);
            } else {
                link.setName(fid);
            }
        }
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("Datei existiert nicht")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        String filesize = null;
        if (link.getPluginPatternMatcher().contains("xup.raidrush.ws/")) {
            filename = br.getRegex("<title>XUP - Download (.*?) \\| ").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h1>XUP - Download (.*?) \\| ").getMatch(0);
            }
            filesize = br.getRegex("Size</font></td>[\t\n\r ]+<td>(\\d+)</td>").getMatch(0);
        } else {
            filename = br.getRegex("<legend>.*?<.*?>Download:(.*?)</.*?>").getMatch(0);
            filesize = br.getRegex("File Size:(.*?)</li>").getMatch(0);
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        link.setName(Encoding.htmlDecode(filename).trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        Form dlform = br.getForm(0);
        if (dlform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String passCode = null;
        if (dlform.hasInputFieldByName("vpass")) {
            link.setPasswordProtected(Boolean.TRUE);
            if (link.getDownloadPassword() == null) {
                passCode = getUserInput(null, link);
            } else {
                /* Use saved password */
                passCode = link.getDownloadPassword();
            }
            dlform.put("vpass", passCode);
        }
        final String captchafieldid = "vchep";
        if (dlform.hasInputFieldByName(captchafieldid)) {
            final String code = this.getCaptchaCode("http://www0.xup.in/captcha.php", link);
            dlform.put(captchafieldid, code);
        }
        dlform.remove(null);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlform, false, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            String page = br.followConnection(true);
            if (page.contains("richtige Passwort erneut ein")) {
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.hoster.xupin.errors.passwrong", "Password wrong"));
            } else if (br.containsHTML(">Die Sicherheitsfrage wurde falsch eingegeben|/captcha\\.php\"")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                logger.warning("Unexpected error occured");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (passCode != null) {
            link.setDownloadPassword(passCode);
        }
        dl.startDownload();
    }

    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        /* Do not allow multihost usage. */
        return link.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}