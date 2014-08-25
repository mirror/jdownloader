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
import java.util.concurrent.atomic.AtomicInteger;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hdstream.to" }, urls = { "http://(www\\.)?hdstream\\.to/#\\!f=[A-Za-z0-9]+" }, flags = { 0 })
public class HdStreamTo extends PluginForHost {

    public HdStreamTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://hdstream.to/#!p=tos";
    }

    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger totalMaxSimultanFreeDownload = new AtomicInteger(20);
    /* don't touch the following! */
    private static AtomicInteger maxFree                      = new AtomicInteger(1);

    /** Using API: http://hdstream.to/#!p=api */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            prepBrowser(br);
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("check=");
                for (final DownloadLink dl : links) {
                    sb.append(dl.getDownloadURL());
                    sb.append("%0A");
                }
                br.postPage("http://hdstream.to/json/check.php", sb.toString());
                for (final DownloadLink dllink : links) {
                    final String fid = this.getFID(dllink);
                    final String thisjson = br.getRegex("(\"" + fid + "\":\\{.*?\\})").getMatch(0);
                    if (thisjson == null || !"on".equals(this.getJson(thisjson, "state"))) {
                        dllink.setAvailable(false);
                    } else {
                        final String hash = this.getJson(thisjson, "hash");
                        final String name = this.getJson(thisjson, "name");
                        final String size = this.getJson(thisjson, "size");
                        dllink.setAvailable(true);
                        dllink.setFinalFileName(encodeUnicode(Encoding.htmlDecode(name)));
                        dllink.setDownloadSize(SizeFormatter.getSize(size));
                        if (hash != null) {
                            dllink.setMD5Hash(hash);
                        }
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
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        checkDownloadable();
        final String server = getJson("server");
        final String free_downloadable = getJson("downloadable");
        final String free_downloadable_max_filesize = new Regex(free_downloadable, "mb(\\d+)").getMatch(0);
        if (free_downloadable.equals("premium") || (free_downloadable_max_filesize != null && downloadLink.getDownloadSize() >= SizeFormatter.getSize(free_downloadable_max_filesize + " mb"))) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        if (server == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String fid = getFID(downloadLink);
        try {
            br.getPage("http://hdstream.to/json/filelist.php?file=" + fid);
        } catch (final Throwable e) {
        }
        br.getPage("http://hdstream.to/send.php?visited=" + fid);
        final String waittime = getJson("wait");
        if (waittime == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.sleep(Integer.parseInt(waittime) * 1001l, downloadLink);
        final String dllink = "http://s" + server + ".hdstream.to/send.php?token=" + fid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 3 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            /* add a download slot */
            controlFree(+1);
            /* start the dl */
            dl.startDownload();
        } finally {
            /* remove download slot */
            controlFree(-1);
        }
    }

    /* Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
    }

    /* Check if the link can be downloaded - 0 = NOT downloadable, not even for premium users - either server problems or only streamable */
    private void checkDownloadable() throws PluginException {
        if (!this.getJson("download").equals("1")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This link is not downloadable");
        }
    }

    /**
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    protected String getJson(final String source, final String key) {
        String result = new Regex(source, "\"" + key + "\":(-?\\d+(\\.\\d+)?|true|false|null)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + key + "\":\"([^\"]+)\"").getMatch(0);
        }
        if (result != null) {
            result = result.replaceAll("\\\\/", "/");
        }
        return result;
    }

    /**
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    protected String getJson(final String key) {
        return getJson(br.toString(), key);
    }

    private void prepBrowser(final Browser br) {
        br.setCookie("http://hdstream.to/", "lang", "en");
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlFree
     *            (+1|-1)
     */
    public synchronized void controlFree(final int num) {
        logger.info("maxFree was = " + maxFree.get());
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
        logger.info("maxFree now = " + maxFree.get());
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}