//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploader.jp" }, urls = { "https?://u[a-z0-9]\\.getuploader\\.com/([a-z0-9\\-_]+)/download/(\\d+)" })
public class UploaderJp extends antiDDoSForHost {
    public UploaderJp(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.uploader.jp/rule.html";
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0) + "_" + new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        final Form form = br.getFormByInputFieldKeyValue("q", "age_confirmation");
        if (form != null) {
            submitForm(form);
        }
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("(?i)>\\s*オリジナル</span><span class=\"right\">([^<>\"]*?)</span>").getMatch(0);
        String filesize = br.getRegex("(?i)>\\s*ファイル</span><span class=\"right\">download \\(([^<>\"]*?)\\)</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<th>オリジナル</th>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex("<th>\\s*容量</th>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        }
        if (filename == null) {
            /* Fallback */
            link.setName(this.getFID(link));
        } else {
            link.setFinalFileName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        final String md5 = br.getRegex("MD5</label>\\s*?<input[^<>]+value=\"([a-f0-9]{32})\"").getMatch(0);
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)404 File Not found<|Page not found");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        Form form = getContinueForm(br);
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (form.hasInputFieldByName("password")) {
            link.setPasswordProtected(true);
            String passCode = link.getDownloadPassword();
            if (passCode == null) {
                passCode = getUserInput("Password?", link);
            }
            form.put("password", Encoding.urlEncode(passCode));
            if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(form)) {
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
            submitForm(form);
            form = getContinueForm(br);
            if (form != null && form.hasInputFieldByName("password")) {
                if (link.getDownloadPassword() != null) {
                    link.setDownloadPassword(null);
                }
                throw new PluginException(LinkStatus.ERROR_RETRY, "Password wrong!");
            }
            link.setDownloadPassword(passCode);
            // standard download
            submitForm(form);
        } else {
            // standard download
            link.setPasswordProtected(false);
            if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(form)) {
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
            submitForm(form);
        }
        String dllink = br.getRegex("\"(https?://d(?:ownload|l)(x|\\d+)\\.getuploader\\.com/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503 too many connections", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    private Form getContinueForm(final Browser br) {
        Form form = br.getFormbyProperty("name", "agree");
        if (form == null) {
            final Form[] forms = br.getForms();
            if (forms != null && forms.length == 1) {
                /* Only one form available -> Select this one */
                form = forms[0];
            }
        }
        return form;
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