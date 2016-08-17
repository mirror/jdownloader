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

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "brazzers.com" }, urls = { "https?://(?:www\\.)?brazzers\\.com/(scenes/view/id/\\d+(?:/[a-z0-9\\-]+/?)?|embed/\\d+/?)" }, flags = { 0 })
public class BrazzersCom extends antiDDoSForHost {

    public BrazzersCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://brazzerssupport.com/terms-of-service/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;

    private boolean              not_yet_released  = false;

    private final String         type_embed        = "https?://(?:www\\.)?brazzers\\.com/embed/\\d+/?";
    private final String         type_normal       = "https?://(?:www\\.)?brazzers\\.com/scenes/view/id/\\d+(?:/[a-z0-9\\-]+/?)?";

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().matches(type_embed)) {
            final String fid = new Regex(link.getDownloadURL(), "/embed/(\\d+)").getMatch(0);
            link.setUrlDownload("http://www.brazzers.com/scenes/view/id/" + fid + "/");
        }
    }

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);

    /**
     * So far this plugin has no account support which means the plugin itself cannot download anything but the download via MOCH will work
     * fine.
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        /* Offline will usually return 404 */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fid = new Regex(link.getDownloadURL(), "/id/(\\d+)/?").getMatch(0);
        final String url_name = new Regex(link.getDownloadURL(), "/id/\\d+/([^/]+)").getMatch(0);
        String filename = br.getRegex("<h1 itemprop=\"name\">([^<>\"]+)<span").getMatch(0);

        /* This way we have a better dupe-detection! */
        link.setLinkID(fid);

        /* Two fallbacks in case we do not get any filename via html code */
        if (inValidate(filename)) {
            filename = url_name;
        }
        if (inValidate(filename)) {
            /* Finally - fallback to fid because we found nothing better. */
            filename = fid;
        } else {
            /* Add fileid in front of the filename to make it look nicer - will usually be removed in the final filename. */
            filename = fid + "_" + filename;
        }
        final Account aa = AccountController.getInstance().getValidAccount(this);
        final boolean moch_download_possible = AccountController.getInstance().hasMultiHostAccounts(this.getHost());
        long filesize_max = -1;
        long filesize_temp = -1;
        final String[] filesizes = br.getRegex("\\[(\\d{1,5}(?:\\.\\d{1,2})? (?:GiB|MB))\\]").getColumn(0);
        for (final String filesize_temp_str : filesizes) {
            filesize_temp = SizeFormatter.getSize(filesize_temp_str);
            if (filesize_temp > filesize_max) {
                filesize_max = filesize_temp;
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);
        /* Do NOT set final filename yet!! */
        link.setName(filename + ".mp4");
        if (filesize_max > -1) {
            if (aa != null) {
                /* Original brazzers account available --> Set highest filesize found --> Best Quality possible */
                link.setDownloadSize(filesize_max);
            } else if (moch_download_possible) {
                /* Multihosters usually return a medium quality - about 1 / 4 the size of the best possible! */
                link.setDownloadSize((long) (filesize_max * 0.25));
            } else {
                link.setDownloadSize(filesize_max);
            }
            link.setProperty("not_yet_released", false);
            not_yet_released = false;
        } else {
            /* No filesize available --> Content is (probably) not (yet) released/downloadable */
            link.getLinkStatus().setStatusText("Content has not yet been released");
            link.setProperty("not_yet_released", true);
            not_yet_released = true;
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        handleGeneralErrors();
        /* Premiumonly */
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void handleGeneralErrors() throws PluginException {
        if (not_yet_released) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Content has not (yet) been released", 1 * 60 * 60 * 1000l);
        }
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) {
        /*
         * Usually an account is needed for this host but in case content has not yet been released the plugin should jump into download
         * mode to display this errormessage to the user!
         */
        return account != null || contentHasNotYetBeenReleased(downloadLink);
    }

    private boolean contentHasNotYetBeenReleased(final DownloadLink dl) {
        return dl.getBooleanProperty("not_yet_released", false);
    }

    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        final boolean is_this_plugin = downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        if (is_this_plugin) {
            /* The original brazzers plugin is always allowed to download. */
            return true;
        } else {
            /* Multihosts should not be tried if we know that content is not yet downloadable! */
            return !contentHasNotYetBeenReleased(downloadLink);
        }
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

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