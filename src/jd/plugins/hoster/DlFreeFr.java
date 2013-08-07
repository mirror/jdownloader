//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dl.free.fr" }, urls = { "http://(www\\.)?dl\\.free\\.fr/(getfile\\.pl\\?file=/[\\w]+|[\\w]+/?)" }, flags = { 0 })
public class DlFreeFr extends PluginForHost {

    private enum CaptchaTyp {
        image,
        audio,
        video,
        notDetected;
    }

    public DlFreeFr(PluginWrapper wrapper) {
        super(wrapper);
    }

    private boolean HTML = false;

    @Override
    public String getAGBLink() {
        return "http://dl.free.fr/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private boolean breakCaptcha(Form form, DownloadLink downloadLink) throws Exception {
        if (!form.containsHTML("Adyoulike\\.create")) return false;
        HashMap<String, String> c = new HashMap<String, String>();
        for (String[] s : form.getRegex("\"([^\\{\"]+)\":\"([^,\"]+)\"").getMatches()) {
            c.put(s[0], s[1]);
        }
        if (c == null || c.size() == 0) return false;

        /* create challenge url */
        final Browser ayl = br.cloneBrowser();
        ayl.getPage("http://api-ayl.appspot.com/challenge?key=" + c.get("key") + "&env=" + c.get("env") + "&callback=Adyoulike.g._jsonp_" + (int) (Math.random() * (99999 - 10000) + 10000));
        final String[][] allValues = ayl.getRegex("\"([^\\{\\}\"]+)\":\"?([^,\"\\}\\{]+)\"?").getMatches();
        if (allValues == null || allValues.length == 0) return false;
        for (String[] s : allValues) {
            c.put(s[0], s[1]);
        }

        String cType = c.get("medium_type");
        cType = cType == null ? "notDetected" : cType;
        cType = cType.split("/")[0];
        String instructions = null, cCode = null;

        if (ayl.getRegex("adyoulike\":\\{\"disabled\":(true)").matches()) {
            br.submitForm(form);
            return true;
        }
        /* Only available in France */
        if ("notDetected".equals(cType) && c.containsKey("disabled") && "true".equals(c.get("disabled"))) throw new PluginException(LinkStatus.ERROR_FATAL, "Only available in France. Please use a french proxy!");

        switch (CaptchaTyp.valueOf(cType)) {
        case image:
            ayl.setFollowRedirects(true);
            instructions = c.get("instructions_visual");
            // Captcha also broken via browser
            if (c.get("token") == null) { throw new PluginException(LinkStatus.ERROR_FATAL, "No captcha shown, please contact the dl.free.fr support!"); }
            final String responseUrl = "http://api-ayl.appspot.com/resource?token=" + c.get("token") + "&env=" + c.get("env");
            if (instructions != null) {
                cCode = new Regex(instructions, "Recopiez « (.*?) » ci\\-dessous").getMatch(0);
                if (cCode == null) {
                    final File captchaFile = this.getLocalCaptchaFile();
                    Browser.download(captchaFile, ayl.openGetConnection(responseUrl));
                    cCode = getCaptchaCode(null, captchaFile, downloadLink);
                }
            } else {
                final File captchaFile = this.getLocalCaptchaFile();
                Browser.download(captchaFile, ayl.openGetConnection(responseUrl));
                cCode = getCaptchaCode(null, captchaFile, downloadLink);
            }
            break;
        case audio:
            break;
        case video:
            break;
        case notDetected:
            break;
        default:
            logger.warning("Unknown captcha typ: " + cType);
            return false;
        }
        if (cCode == null) { return false; }
        form.put("_ayl_captcha_engine", "adyoulike");
        form.put("_ayl_response", cCode);
        form.put("_ayl_utf8_ie_fix", "%E2%98%83");
        form.put("_ayl_env", c.get("env"));
        form.put("_ayl_token_challenge", c.get("token"));
        form.put("_ayl_tid", c.get("tid"));
        br.submitForm(form);
        return true;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (HTML) {
            logger.info("InDirect download");
            br.setFollowRedirects(false);
            if (br.containsHTML("Trop de slots utilis")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
            final Form captchaForm = br.getForm(1);
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String filename = br.getRegex(Pattern.compile("Fichier:</td>.*?<td.*?>(.*?)<", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
            String filesize = br.getRegex(Pattern.compile("Taille:</td>.*?<td.*?>(.*?)soit", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

            // Old
            // /* special captcha handling */
            // boolean isCaptchaResolved = false;
            // for (int i = 0; i < 5; i++) {
            // isCaptchaResolved = breakCaptcha(captchaForm, downloadLink);
            // if (isCaptchaResolved) break;
            // }
            // if (!isCaptchaResolved) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

            // Really old
            // PluginForHost recplug =
            // JDUtilities.getPluginForHost("DirectHTTP");
            // jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP)
            // recplug).getReCaptcha(br);
            // rc.setForm(captchaForm);
            // String id =
            // br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\\'").getMatch(0);
            // // No captcha displayed but we have to tner it->Hoster bug
            // if (id == null &&
            // br.containsHTML("Valider et t\\&eacute;l\\&eacute;charger le fichier"))
            // throw new
            // PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE,
            // "Server error", 30 * 60 * 1000l);
            // rc.setId(id);
            // rc.load();
            // rc.getForm().put("_ayl_captcha_engine", "recaptcha");
            // rc.getForm().put("_ayl_utf8_ie_fix", "%E2%98%83");
            // rc.getForm().put("_ayl_env", "prod");
            // rc.getForm().put("_ayl_token_challenge", "undefined");
            // rc.getForm().put("_ayl_tid", "undefined");
            // File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            // String c = getCaptchaCode(cf, downloadLink);
            // rc.setCode(c);
            // if
            // (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)"))
            // throw new PluginException(LinkStatus.ERROR_CAPTCHA);

            final String file = br.getRegex("type=\"hidden\" name=\"file\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (file == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.postPage("http://dl.free.fr/getfile.pl", "submit=Valider+et+t%C3%A9l%C3%A9charger+le+fichier&file=" + Encoding.urlEncode(file));
            final String dlLink = br.getRedirectLocation();
            if (dlLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlLink, true, 1);
        } else {
            logger.info("Direct download");
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 1);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.getURL().contains("overload")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(downloadLink.getDownloadURL());
            if (con.isContentDisposition()) {
                downloadLink.setFinalFileName(Plugin.getFileNameFromHeader(con));
                downloadLink.setDownloadSize(con.getLongContentLength());
                return AvailableStatus.TRUE;
            } else {
                br.followConnection();
                HTML = true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        String filename = br.getRegex(Pattern.compile("Fichier:</td>.*?<td.*?>(.*?)<", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("Taille:</td>.*?<td.*?>(.*?)soit", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll("o", "byte").replaceAll("Ko", "Kb").replaceAll("Mo", "Mb").replaceAll("Go", "Gb")));
        return AvailableStatus.TRUE;
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}