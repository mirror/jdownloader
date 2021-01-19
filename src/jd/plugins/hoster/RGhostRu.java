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
package jd.plugins.hoster;

import java.io.IOException;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

//rghost.ru by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rghost.net" }, urls = { "https?://(?:[a-z0-9]+\\.)?(?:rghost\\.(?:net|ru)|rgho\\.st)/(.+)" })
public class RGhostRu extends PluginForHost {
    private static final String PWTEXT              = "id=\"password_field\"";
    private static final String type_private_all    = "http://([a-z0-9]+\\.)?[^/]+/private/.+";
    private static final String type_private_direct = "http://([a-z0-9]+\\.)?[^/]+/private/[A-Za-z0-9]+/[a-f0-9]{32}/.+";
    private static final String type_normal_direct  = "http://([a-z0-9]+\\.)?[^/]+/[A-Za-z0-9]+/.+";

    public RGhostRu(PluginWrapper wrapper) {
        super(wrapper);
        // this host blocks if there is no timegap between the simultan
        // downloads so waittime is 3,5 sec right now, works good!
        this.setStartIntervall(3500l);
    }

    @Override
    public String getAGBLink() {
        return "http://rgho.st/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String rewriteHost(final String host) {
        if ("rgho.st".equals(getHost()) || "rghost.ru".equals(getHost())) {
            if (host == null || "rgho.st".equals(host) || "rghost.ru".equals(host)) {
                return "rghost.net";
            }
        }
        return super.rewriteHost(host);
    }

    public void correctDownloadLink(DownloadLink link) {
        String newlink = link.getPluginPatternMatcher().replaceAll("([a-z0-9]+)?rghost\\.(?:net|ru)/", "rgho.st/");
        if (newlink.matches(type_private_direct) || (newlink.matches(type_normal_direct) && !newlink.matches(type_private_all))) {
            /* Directlinks --> Change to normal links */
            newlink = newlink.substring(0, newlink.lastIndexOf("/"));
        } else {
            /* Picture view-links */
            if (newlink.endsWith(".view")) {
                newlink = newlink.substring(0, newlink.lastIndexOf("."));
            }
        }
        newlink = newlink.replaceFirst(Regex.escape(Browser.getHost(newlink)), this.getHost());
        link.setPluginPatternMatcher(newlink);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 409, 503 });
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 409) {
            /* Cloudflare DNS issue --> In this case definitly offline! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "503 Server maintenance");
        } else if (!br.containsHTML("abuses\\?file=" + Regex.escape(getFID(link)))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]+) — RGhost — file sharing</title>").getMatch(0);
        String filesize = br.getRegex("<i class=\"nowrap\">\\(([^<>\"]+)\\)<").getMatch(0);
        // will pick up the first filesize mentioned.. as last resort fail over.
        if (filesize == null) {
            filesize = br.getRegex("(?i)([\\d\\.]+ ?(KB|MB|GB))").getMatch(0);
        }
        String md5 = br.getRegex("<b>MD5</b></td><td>(.*?)</td></tr>").getMatch(0);
        if (md5 == null) {
            md5 = br.getRegex("(?i)MD5((<[^>]+>)+?([\r\n\t ]+)?)+?([a-z0-9]{32})").getMatch(3);
        }
        if (md5 == null) {
            md5 = br.getRegex("(?i)MD5.*?([a-z0-9]{32})").getMatch(0);
        }
        if (md5 != null) {
            link.setMD5Hash(md5.trim());
        }
        String sha1 = br.getRegex("<b>SHA1</b></td><td>(.*?)</td></tr>").getMatch(0);
        if (sha1 == null) {
            sha1 = br.getRegex("(?i)SHA1((<[^>]+>)+?([\r\n\t ]+)?)+?([a-z0-9]{40})").getMatch(3);
        }
        if (sha1 == null) {
            sha1 = br.getRegex("(?i)SHA1.*?([a-z0-9]{32})").getMatch(0);
        }
        if (sha1 != null) {
            link.setSha1Hash(sha1.trim());
        }
        if (filename != null) {
            link.setName(filename);
        }
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        /* Leave this here - filename- and filesize can be present although the url is offline! */
        if (this.br.containsHTML(">File is deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(PWTEXT)) {
            link.getLinkStatus().setStatusText("This file is password protected");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        String dllink = getDownloadlink();
        String passCode = null;
        if (dllink == null && br.containsHTML(PWTEXT)) {
            Form pwform = br.getFormbyKey("password");
            if (pwform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            pwform.put("password", passCode);
            /* Correct some stuff */
            pwform.remove("commit");
            pwform.put("commit", "Get+link");
            pwform.put("utf8", "%E2%9C%93");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            br.getHeaders().put("Accept", "*/*;q=0.5, text/javascript, application/javascript, application/ecmascript, application/x-ecmascript");
            // br.getHeaders().put("X-CSRF-Token", "");
            br.submitForm(pwform);
            /* Correct html */
            br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
            dllink = getDownloadlink();
            if (dllink == null && br.containsHTML(PWTEXT)) {
                link.setProperty("pass", null);
                logger.info("DownloadPW wrong!");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    private String getDownloadlink() {
        String dllink = br.getRegex("\"([^\"]*/download/[^<>\"]*?)\"").getMatch(0);
        return dllink;
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