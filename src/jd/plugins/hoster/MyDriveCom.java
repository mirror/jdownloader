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

import java.util.ArrayList;

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

import org.appwork.utils.formatter.SizeFormatter;

//INDO: belongs to fileserving.com
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mydrive.com" }, urls = { "http://(www\\.)?(myvdrive|fileserving)\\.com/files/[A-Za-z0-9_]+" }, flags = { 0 })
public class MyDriveCom extends PluginForHost {

    public MyDriveCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.myvdrive.com/Public/term";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("fileserving.com/", "myvdrive.com/"));
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser br = new Browser();
            prepBr();
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 49) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                br.getPage("http://www.myvdrive.com/Public/linkchecker");
                final String hash = br.getRegex("name=\"__hash__\" value=\"([a-z0-9]+)\"").getMatch(0);
                if (hash == null) {
                    logger.warning("Linkchecker for mydrive.com is broken!");
                    return false;
                }
                sb.append("__hash__=" + hash + "&links=");
                for (final DownloadLink dl : links) {
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    sb.append("%0A");
                }
                br.postPage("http://www.myvdrive.com/Public/linkchecker", sb.toString());
                for (final DownloadLink dllink : links) {
                    final String linkPart = new Regex(dllink.getDownloadURL(), "(myvdrive\\.com/files/[A-Za-z0-9_]+)").getMatch(0);
                    if (br.containsHTML(linkPart + "[\t\n\r ]+</td>[\t\n\r ]+<td>[\t\n\r ]+\\-\\- \\-\\-[\t\n\r ]+</td>")) {
                        dllink.setAvailable(false);
                    } else {
                        final Regex fileInfo = br.getRegex(linkPart + "[\t\n\r ]+</td>[\t\n\r ]+<td>([^<>\"]*?)</td>[\t\n\r ]+<td>([^<>\"]*?)</td>");
                        if (fileInfo.getMatches().length != 1) {
                            logger.warning("Linkchecker for mydrive.com is broken!");
                            return false;
                        }
                        dllink.setAvailable(true);
                        dllink.setFinalFileName(Encoding.htmlDecode(fileInfo.getMatch(0).trim()));
                        dllink.setDownloadSize(SizeFormatter.getSize(fileInfo.getMatch(1)));
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
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        /** Old linkcheck code can be found in rev 16195 */
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) { return AvailableStatus.UNCHECKED; }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9_]+)$").getMatch(0);
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Sorry, this file has been removed\\.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("http://www.myvdrive.com/Index/verifyRecaptcha?fid=" + fid + "&sscode=&v=&server=");
        String dllink = br.getRegex("Your download is ready\\! <a href=\\\\\"(http:[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void prepBr() {
        br.setCookie("http://mydrive.com/", "fileserving_think_language", "en-us");
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}