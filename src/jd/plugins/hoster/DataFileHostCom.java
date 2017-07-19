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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datafilehost.com" }, urls = { "https?://((www\\.)?datafilehost\\.com/(download\\-[a-z0-9]+\\.html|d/[a-z0-9]+)|www\\d+\\.datafilehost\\.com/d/[a-z0-9]+)" })
public class DataFileHostCom extends antiDDoSForHost {
    // note: at this time download not possible? everything goes via 'download manager' which is just used to install adware/malware.
    private char[] FILENAMEREPLACES = new char[] { ' ', '_', '[', ']' };

    @Override
    public char[] getFilenameReplaceMap() {
        return FILENAMEREPLACES;
    }

    @Override
    public boolean isHosterManipulatesFilenames() {
        return true;
    }

    @Override
    public String filterPackageID(String packageIdentifier) {
        return packageIdentifier.replaceAll("([^a-zA-Z0-9]+)", "");
    }

    public DataFileHostCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.datafilehost.com/index.php?page=tos";
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br = new Browser();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"alert alert\\-danger\"") || this.br.containsHTML("The file that you are looking for is either|an invalid file name|has been removed due to|Please check the file name again and|>The file you requested \\(id [a-z0-9]+\\) does not exist.|>Invalid file ID.|The file .*? is invalid.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("no longer exists on|>It was either removed by the owner of the file")) {
            /* 2017-02-06 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex(">\\s*File\\s*:\\s*(.*?)\\s*<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("fileName=\"([^<>\"]+)\"").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("class=\"col-sm-3\">[\t\n\r ]*?<strong>File Name:</strong>[\t\n\r ]*?</div>[\t\n\r ]*?<div class=\"col-sm-9\">([^<>\"]+)</div>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex(">File Name\\s*?:\\s*?<br><strong>([^<>\"]+)<").getMatch(0);
        }
        String filesize = br.getRegex(">\\s*Size\\s*:\\s*(.*?)\\s*<").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("class=\"col-sm-3\">[\t\n\r ]*?<strong>File Size:</strong>[\t\n\r ]*?</div>[\t\n\r ]*?<div class=\"col-sm-9\">([^<>\"]+)</div>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex("<h4> <strong>(\\d+[^<>\"\\']+)</strong>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private LinkedHashSet<String> dupe = new LinkedHashSet<String>();

    private void simulateBrowser() throws InterruptedException {
        dupe.clear();
        final AtomicInteger requestQ = new AtomicInteger(0);
        final AtomicInteger requestS = new AtomicInteger(0);
        final ArrayList<String> links = new ArrayList<String>();
        final String out = br.toString();
        String[] l1 = new Regex(out, "\\s+(?:src|href)=(\"|')((?!'.*?\\').*?)\\1").getColumn(1);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        l1 = new Regex(out, "\\s+(?:src|href)=(?!\"|')([^\\s]+)").getColumn(0);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        l1 = new Regex(out, "url\\((\"|')(.*?datafilehost\\.com/.*?\\.(?:css|js|png|jpe?g|gif))\\1").getColumn(1);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        for (final String link : links) {
            // lets only add links related to this hoster.
            final String correctedLink = Request.getLocation(link, this.br.getRequest());
            if (this.getHost().equals(Browser.getHost(correctedLink)) && !correctedLink.matches(".+" + Pattern.quote(this.getHost()) + "/?$") && !new Regex(correctedLink, "\\.html|\\.php|/demo/public|%20|&(?:#[0-9]{3,4}|[A-Za-z]{2,8});").matches() && !correctedLink.matches(getSupportedLinks().pattern()) && !correctedLink.equals(this.br.getURL())) {
                if (this.dupe.add(correctedLink)) {
                    final Thread simulate = new Thread("SimulateBrowser") {
                        @Override
                        public void run() {
                            final Browser rb = DataFileHostCom.this.br.cloneBrowser();
                            rb.getHeaders().put("Cache-Control", null);
                            // open get connection for images, need to confirm
                            if (correctedLink.matches(".+\\.(?:png.*|jpe?g|ico|gif).*")) {
                                rb.getHeaders().put("Accept", "image/webp,*/*;q=0.8");
                            } else if (correctedLink.matches(".+\\.js.*")) {
                                rb.getHeaders().put("Accept", "*/*");
                            } else if (correctedLink.matches(".+\\.css.*")) {
                                rb.getHeaders().put("Accept", "text/css,*/*;q=0.1");
                            }
                            try {
                                requestQ.getAndIncrement();
                                dummyLoad(correctedLink);
                            } catch (final Exception e) {
                            } finally {
                                requestS.getAndIncrement();
                            }
                            return;
                        }
                    };
                    simulate.start();
                    Thread.sleep(100);
                }
            }
        }
        while (requestQ.get() != requestS.get()) {
            Thread.sleep(1000);
        }
    }

    private void dummyLoad(final String url) throws Exception {
        final URLConnectionAdapter con = openAntiDDoSRequestConnection(br.cloneBrowser(), br.createGetRequest(url));
        try {
            if (con.isOK()) {
                final byte[] buf = new byte[32];
                final InputStream is = con.getInputStream();
                while (is.read(buf) != -1) {
                }
            }
        } finally {
            con.disconnect();
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        simulateBrowser();
        sleep(2000, downloadLink);
        final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9]+)(?:\\.html)?$").getMatch(0);
        final String dllink = br.getURL("/get.php?file=" + fid).toString().replace("https://", "http://");
        br.getHeaders().put("Upgrade-Insecure-Requests", "1");
        br.setRequest(null);
        final DataFileHostConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.DataFileHostCom.DataFileHostConfigInterface.class);
        final int maxchunks;
        if (cfg.isFreeUnlimitedChunksEnabled()) {
            maxchunks = 0;
        } else {
            maxchunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxchunks);
        if (downloadLink.getDownloadSize() > 0 && dl.getConnection().getLongContentLength() == 0) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: Server sends empty file", 5 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML("Accessing directly the download link doesn\\'t work")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return DataFileHostConfigInterface.class;
    }

    public static interface DataFileHostConfigInterface extends PluginConfigInterface {
        public static class TRANSLATION {
            public String getFreeUnlimitedChunksEnabled_label() {
                return "Enable unlimited chunks for free mode [can cause issues]?";
            }
        }

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        @DefaultBooleanValue(false)
        @Order(8)
        boolean isFreeUnlimitedChunksEnabled();

        void setFreeUnlimitedChunksEnabled(boolean b);
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