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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileserving.com" }, urls = { "http://(www\\.)?fileserving\\.com/files/[a-zA-Z0-9]+" }, flags = { 0 })
public class FileServingCom extends PluginForHost {

    public FileServingCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    public String FILEIDREGEX = "fileserving\\.com/files/([a-zA-Z0-9]+)(http:.*)?";

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // All links should look the same to get no problems with regexing them
        // later
        link.setUrlDownload("http://www.fileserving.com/files/" + getID(link));
    }

    private String getID(final DownloadLink link) {
        return new Regex(link.getDownloadURL(), this.FILEIDREGEX).getMatch(0);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = downloadLink.getStringProperty("freelink");
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty("freelink", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty("freelink", Property.NULL);
                dllink = null;
            }
        }
        if (dllink == null) {
            br.getPage(downloadLink.getDownloadURL());
            final String fid = new Regex(downloadLink.getDownloadURL(), "fileserving\\.com/files/([a-zA-Z0-9]+)").getMatch(0);
            final String sid = br.getRegex("sid:\\'(\\d+)\\'").getMatch(0);
            final String server = br.getRegex("server:\\'([^<>\"\\']+)\\'").getMatch(0);
            final String rcID = br.getRegex("k=([^\\'<>\"]+)\"").getMatch(0);
            if (fid == null || sid == null || server == null || rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            Browser xmlBrowser = br.cloneBrowser();
            xmlBrowser.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(new Form());
            rc.setId(rcID);
            for (int i = 0; i <= 5; i++) {
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                xmlBrowser.getPage("http://www.fileserving.com/Index/verifyRecaptcha?fid=" + fid + "&sid=" + sid + "&server=" + server + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
                if (xmlBrowser.containsHTML("\"error\"")) continue;
                break;
            }
            if (xmlBrowser.containsHTML("\"error\"")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            final String correctedXml = xmlBrowser.toString().replace("\\", "");
            dllink = new Regex(correctedXml, "Your download is ready\\! <a href=\"(http:[^<>\"]+)\"").getMatch(0);
            if (dllink == null) dllink = new Regex(correctedXml, "\"(http://s\\d+\\.fileserving\\.com/file/" + fid + "/[^<>\"]+)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (this.checkLinks(new DownloadLink[] { link }) == false) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return link.getAvailableStatus();
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            br.setCustomCharset("utf-8");
            this.br.getPage("http://www.fileserving.com/Public/linkchecker");
            final String hash = br.getRegex("name=\"__hash__\" value=\"([a-z0-9]+)\"").getMatch(0);
            if (hash == null) {
                logger.warning("Fileserving.com availablecheck is broken!");
                return false;
            }
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            final StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("__hash__=" + hash + "&links=");
                links.clear();
                while (true) {
                    /*
                     * we test 100 links at once - its tested with 500 links,
                     * probably we could test even more at the same time...
                     */
                    if (index == urls.length || links.size() > 100) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                int c = 0;
                for (final DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report
                     * anything else
                     */
                    if (c > 0) {
                        sb.append("%0D%0A");
                    }
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                this.br.postPage("http://www.fileserving.com/Public/linkchecker", sb.toString());
                for (final DownloadLink dl : links) {
                    final String fileid = new Regex(dl.getDownloadURL(), this.FILEIDREGEX).getMatch(0);
                    if (fileid == null) {
                        logger.warning("Fileserving.com availablecheck is broken!");
                        return false;
                    }
                    Regex fileInfo = br.getRegex("(fileserving\\.com/files/" + fileid + "[\t\n\r ]+</td>[\t\n\r ]+<td>([^<>\"]+)</td>[\t\n\r ]+<td>([^<>\"]+)</td>)");
                    String filename = fileInfo.getMatch(1);
                    String filesize = fileInfo.getMatch(2);
                    if (br.containsHTML("class=\"icon_file_check_notvalid\"></span>[\t\n\r ]+http://(www\\.)?fileserving\\.com/files/" + fileid)) {
                        dl.setAvailable(false);
                        continue;
                    } else if (filename == null || filesize == null) {
                        logger.warning("Fileserving.com availablecheck is broken!");
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    dl.setName(filename);
                    if (filesize != null) {
                        if (filesize.contains(",") && filesize.contains(".")) {
                            /* workaround for 1.000,00 MB bug */
                            filesize = filesize.replaceFirst("\\.", "");
                        }
                        dl.setDownloadSize(SizeFormatter.getSize(filesize));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}