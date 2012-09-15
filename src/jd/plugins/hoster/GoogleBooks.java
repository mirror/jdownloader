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

import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.captcha.easy.load.LoadImage;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "books.google.com" }, urls = { "http://googlebooksdecrypter.[a-z]+/books\\?id=.*&pg=.*" }, flags = { 0 })
public class GoogleBooks extends PluginForHost {

    private static AtomicInteger counter = new AtomicInteger(0);

    private static Object        LOCK    = new Object();

    public GoogleBooks(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("googlebooksdecrypter", "books.google"));
    }

    @Override
    public String getAGBLink() {
        return "http://books.google.de/accounts/TOS";
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setDebug(true);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(link.getDownloadURL());
        // logger.info(br.toString());
        if (br.containsHTML("src=\"/googlebooks/restricted_logo.gif\"")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
        if (br.containsHTML("Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // mpage moved + capcha - secure for automatic downloads
        if (br.containsHTML("http://sorry.google.com/sorry/\\?continue=.*")) {
            String url = br.getRedirectLocation() != null ? br.getRedirectLocation() : br.getRegex("<A HREF=\"(http://sorry.google.com/sorry/\\?continue=http://books.google.com/books.*?)\">").getMatch(0);
            if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            // TODO: can make redirect and capcha but this only for secure to
            // continue connect not for download page
        }
        String dllink = br.getRegex(";preloadImg.src = \\'(.*?)\\';window").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getContentType().contains("html")) {
            br.followConnection();
            synchronized (LOCK) {
                if (counter.get() > 10) {
                    /* too many failed lets wait and retry later */
                    counter.set(0);
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
                } else {
                    /* lets temp unavail this download, maybe it works later */
                    counter.incrementAndGet();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
                }
            }

        }
        // Get and set the correct ending of the file!
        String correctEnding = LoadImage.getFileType(dllink, dl.getConnection().getContentType());
        String wrongEnding = null;
        if (link.getName().lastIndexOf('.') > 0) wrongEnding = link.getName().substring(link.getName().lastIndexOf('.'));
        if (correctEnding != null && wrongEnding != null) link.setFinalFileName(link.getName().replace(wrongEnding, correctEnding));
        if (correctEnding != null && wrongEnding == null) link.setFinalFileName(link.getName() + correctEnding);
        dl.startDownload();

    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
        counter.set(0);
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

}