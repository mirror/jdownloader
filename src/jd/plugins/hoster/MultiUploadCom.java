package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

/**
 * Hoster belongs to uploadking.com & uploadhere.com, uses similar code for some
 * parts
 */
@HostPlugin(revision = "$Revision: 15712 $", interfaceVersion = 2, names = { "multiupload.com" }, urls = { "http://(www\\.)?multiuploaddecrypted\\.com/([A-Z0-9]{2}_[A-Z0-9]+|[0-9A-Z]+)" }, flags = { 0 })
public class MultiUploadCom extends PluginForHost {

    private String domain = null;

    public MultiUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://multiupload.com/terms";
    }

    /** All links come from a decrypter */
    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("multiuploaddecrypted.com/", "multiupload.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            /* domain redirection */
            domain = new Regex(br.getRedirectLocation(), "http.*?//.*?(multiupload\\..*?)/").getMatch(0);
            if (domain == null) {
                domain = "multiupload.com";
            }
            br.getPage(br.getRedirectLocation());
        }
        if (domain == null) domain = "multiupload.com";
        if (br.containsHTML(">Unfortunately, the link you have clicked is not available")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex match = br.getRegex("font\\-size:19px; color:#FFFFFF;\">([^/<>\"]+)<font style=\"color:#002B55;\">\\(([^/<>\"]+)\\)</font></div>");
        if (match.count() == 0) match = br.getRegex("width:788px; text\\-align:center; font\\-size:19px; color:#000000;\">([^/<>\"]+)<font style=\"color:#666666;\">\\(([^/<>\"]+)\\)</font></div>");
        if (match.count() > 0) {
            link.setDownloadSize(SizeFormatter.getSize(match.getMatch(1)));
            link.setFinalFileName(match.getMatch(0).trim());
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setReadTimeout(2 * 60 * 1000);
        String dllink = link.getStringProperty("freelink");
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    link.setProperty("freelink", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                link.setProperty("freelink", Property.NULL);
                dllink = null;
            }
        }
        if (dllink == null) {
            final String rcID = br.getRegex("Recaptcha\\.create\\(\"([^/<>\"]+)\"").getMatch(0);
            if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final Form cForm = new Form();
            cForm.setMethod(MethodType.POST);
            final String fid = new Regex(link.getDownloadURL(), "multiupload\\.[a-z]{2,3}/(.+)").getMatch(0);
            final String action = "http://www." + domain + "/" + fid + "?c=" + fid;
            cForm.setAction(action);
            br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(cForm);
            rc.setId(rcID);
            for (int i = 0; i <= 5; i++) {
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, link);
                br.postPage(action, "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
                if (br.containsHTML("\"response\":\"0\"")) continue;
                break;
            }
            dllink = br.getRegex("href\":\"(http:.*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = dllink.replace("\\", "");
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        URLConnectionAdapter urlConnection = dl.getConnection();
        if (!urlConnection.isContentDisposition() && urlConnection.getHeaderField("Cache-Control") != null) {
            urlConnection.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        link.setProperty("freelink", dllink);
        this.dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}