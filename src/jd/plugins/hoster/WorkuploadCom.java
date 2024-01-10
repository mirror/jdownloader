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

import java.io.IOException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "workupload.com" }, urls = { "https?://(?:www\\.|en\\.)?workupload\\.com/(?:file|start)/([A-Za-z0-9]+)" })
public class WorkuploadCom extends PluginForHost {
    public WorkuploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://workupload.com/tos";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = -1;

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
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 410 });
        final String fileID = this.getFID(link);
        br.getPage("https://" + this.getHost() + "/file/" + fileID);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410 || this.br.containsHTML("img/404\\.jpg\"|>Whoops\\! 404|> Datei gesperrt")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isAntiBotCaptchaBlocked(br)) {
            /* 2023-03-20: Added detection for this but captcha handling is still missing. */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Anti bot block");
        }
        final String sha256 = br.getRegex("(?i)([A-Fa-f0-9]{64})\\s*\\(SHA256\\)").getMatch(0);
        if (sha256 != null) {
            link.setSha256Hash(sha256);
        }
        if (isPasswordProtected(br)) {
            link.setPasswordProtected(true);
            /* Small trick to obtain filename for password protected files */
            final Browser brc = br.cloneBrowser();
            brc.getPage(br.getURL("/report/" + fileID));
            final String filename = brc.getRegex("<b>\\s*Datei\\s*: ([^<]+)</b>").getMatch(0);
            if (filename != null) {
                link.setName(Encoding.htmlDecode(filename).trim());
            } else {
                logger.warning("Failed to find filename");
            }
        } else {
            link.setPasswordProtected(false);
            String filename = br.getRegex("<td>\\s*Dateiname\\s*:\\s*</td><td>([^<>\"]*?)<").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"intro\">[\n\t\r ]*?<b>([^<>\"]+)</b>").getMatch(0);
            }
            String filesize = br.getRegex("<td>Dateigröße:</td><td>([^<>\"]*?)<").getMatch(0);
            if (filename == null || filesize == null) {
                Regex filenameSize = br.getRegex("<p class=\"intro\">[\n\t\r ]*?<b>(.*?)</b>[^\n\t\r <>\"]*?(\\d+(?:\\.\\d+)? ?(KB|MB|GB))[^\n\t\r <>\"]*?");
                if (filename == null) {
                    filename = filenameSize.getMatch(0);
                }
                if (filesize == null) {
                    filesize = filenameSize.getMatch(1);
                }
            }
            if (filesize == null) {
                filesize = br.getRegex("(\\d+(?:\\.\\d+)? ?(KB|MB|GB))").getMatch(0);
            }
            if (filesize == null) {
                filesize = br.getRegex("(\\d+(?:\\.\\d+)? ?(?:B(?:ytes?)?))").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            }
            if (filename != null) {
                link.setName(Encoding.htmlDecode(filename).trim());
            } else {
                logger.warning("Failed to find filename");
            }
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            } else {
                logger.warning("Failed to find filesize");
            }
        }
        return AvailableStatus.TRUE;
    }

    private boolean isPasswordProtected(final Browser br) {
        return br.containsHTML("id=\"passwordprotected_file_password\"");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        final String first_url = this.br.getURL();
        // String dllink = checkDirectLink(downloadLink, directlinkproperty);
        String dllink = null;
        if (dllink == null) {
            if (isPasswordProtected(br)) {
                String passCode = link.getDownloadPassword();
                if (passCode == null) {
                    passCode = getUserInput("Password?", link);
                }
                br.postPage(this.br.getURL(), "passwordprotected_file%5Bpassword%5D=" + Encoding.urlEncode(passCode) + "&passwordprotected_file%5Bsubmit%5D=&passwordprotected_file%5Bkey%5D=" + this.getFID(link));
                if (isPasswordProtected(br)) {
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else {
                    /* User entered valid password */
                    link.setDownloadPassword(passCode);
                }
            }
            br.getPage("/start/" + this.getFID(link));
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.getPage("/api/file/getDownloadServer/" + this.getFID(link));
            dllink = PluginJSonUtils.getJsonValue(this.br, "url");
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
        }
        this.br.getHeaders().put("Referer", first_url);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.getURL().contains("/file/")) {
                logger.info("Final downloadurl redirected to main url");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.setFilenameFix(true);
        link.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private boolean isAntiBotCaptchaBlocked(final Browser br) {
        return br.containsHTML("class=\"fa fa-shield-check\"");
    }
    // public void handleAntiBot(final Browser br) throws PluginException {
    // if (isAntiBotCaptchaBlocked(br)) {
    // /* 2023-03-20: Added detection for this but captcha handling is still missing. */
    // final String result = getAntibotResult(this);
    // throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Anti bot block");
    // }
    // }
    //
    // public static String getAntibotResult(final Plugin plugin) {
    // final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(plugin);
    // final ScriptEngine engine = manager.getEngineByName("javascript");
    // final StringBuilder sb = new StringBuilder();
    // sb.append("async function sha256(message, find, i) { const msgBuffer = new TextEncoder().encode(message); const hashBuffer = await
    // crypto.subtle.digest('SHA-256', msgBuffer); const hashArray = Array.from(new Uint8Array(hashBuffer)); const hashHex = hashArray.map(b
    // => b.toString(16).padStart(2, '0')).join(''); if(find.includes(hashHex)){ return i; } }");
    // sb.append("function getresult(res) { let found = 0; let i = 0; var captcharesult = ''; while (i < res.data.range) {
    // sha256(res.data.puzzle + i, res.data.find, i).then(function(s){ if(typeof s !== \"undefined\"){ captcharesult = captcharesult + s + '
    // '); found++; if(found == res.data.find.length){ break; } } }); i++; } return captcharesult; }");
    // sb.append("var finalresult = getresult();");
    // try {
    // final String jsCorrected = sb.toString().replace("async ", " ").replace("await ", "").replace("let ", "var ").replace("const ", "var
    // ");
    // System.out.print(jsCorrected);
    // engine.eval(jsCorrected);
    // return engine.get("finalresult").toString();
    // } catch (final Exception e) {
    // if (plugin != null) {
    // plugin.getLogger().info(e.toString());
    // }
    // e.printStackTrace();
    // return null;
    // }
    // }
    // public static void main(String[] args) throws InterruptedException, MalformedURLException {
    // System.out.print("Captcharesult = " + getAntibotResult(null));
    // }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}