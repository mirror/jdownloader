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

package jd.plugins;

import java.nio.charset.CharacterCodingException;

import jd.controlling.reconnect.ipcheck.IP;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.Downloadable;
import jd.plugins.download.raf.OldRAFDownload;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.settings.GeneralSettings;

/**
 * heavily modified download interface by raztoki
 *
 * @author raztoki
 *
 */
public class BrowserDownloadInterface {

    public static final int ERROR_REDIRECTED = -1;

    protected DownloadInterface getDownloadInterface(Downloadable downloadable, Request request, boolean resumeEnabled, int chunksCount) throws Exception {
        final OldRAFDownload dl = new OldRAFDownload(downloadable, request);
        final int chunks = downloadable.getChunks();
        if (chunksCount == 0) {
            dl.setChunkNum(chunks <= 0 ? JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile() : chunks);
        } else {
            dl.setChunkNum(chunksCount < 0 ? Math.min(chunksCount * -1, chunks <= 0 ? JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile() : chunks) : chunksCount);
        }
        dl.setResume(resumeEnabled);
        return dl;
    }

    /**
     *
     * @param br
     * @param downloadable
     * @param request
     * @param resume
     *            true|false, if chunks over 1 it must be true!
     * @param chunks
     *            0 = unlimited, chunks must start with negative sign otherwise it forces value to be used instead of up to value.
     * @return
     * @throws Exception
     */
    protected DownloadInterface openDownload(Browser br, Downloadable downloadable, Request request, boolean resume, int chunks) throws Exception {
        String originalUrl = br.getURL();
        DownloadInterface dl = getDownloadInterface(downloadable, request, resume, chunks);
        downloadable.setDownloadInterface(dl);
        try {
            dl.connect(br);
        } catch (PluginException handle) {
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            if (handle.getValue() == ERROR_REDIRECTED) {
                final int redirect_max = 10;
                int redirect_count = 0;
                String lastRedirectUrl = null;
                while (redirect_count++ < redirect_max) {
                    request = br.createRedirectFollowingRequest(request);
                    String redirectUrl = request.getUrl();
                    handleBlockedRedirect(redirectUrl);
                    if (lastRedirectUrl != null && redirectUrl.equals(lastRedirectUrl)) {
                        /*
                         * some providers don't like fast redirects, as they use this for preparing final file. lets add short wait based on
                         * retry count
                         */
                        Thread.sleep(redirect_count * 250l);
                    }
                    if (originalUrl != null) {
                        request.getHeaders().put("Referer", originalUrl);
                    }
                    dl = getDownloadInterface(downloadable, request, resume, chunks);
                    downloadable.setDownloadInterface(dl);
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException handle2) {
                        try {
                            dl.getConnection().disconnect();
                        } catch (Throwable ignore) {
                        }
                        if (handle2.getValue() == ERROR_REDIRECTED) {
                            lastRedirectUrl = redirectUrl;
                            continue;
                        } else {
                            throw handle2;
                        }
                    }
                }
                if (redirect_count++ >= redirect_max) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop");
                }
            } else {
                throw handle;
            }
        } catch (Exception forward) {
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            if (dl.getConnection() != null) {
                handleBlockedConnection(dl, br);
            }
            throw forward;
        }
        if (dl.getConnection() != null) {
            handleBlockedConnection(dl, br);
        }
        return dl;
    }

    /**
     *
     * @param br
     * @param downloadLink
     * @param link
     * @param resume
     *            true|false, if chunks over 1 it must be true!
     * @param chunks
     *            0 = unlimited, chunks must start with negative sign otherwise it forces value to be used instead of up to value.
     * @return
     * @throws Exception
     */
    public DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link, boolean resume, int chunks) throws Exception {
        return openDownload(br, BrowserAdapter.getDownloadable(downloadLink, br), br.createRequest(link), resume, chunks);
    }

    /**
     * Antivirus/Firewall/Gateway blocks. ONLY to be used within headers, not content.
     *
     * @author raztoki
     * @since JD2
     * @param dl
     * @param br
     * @throws PluginException
     */
    protected void handleBlockedConnection(final DownloadInterface dl, final Browser br) throws PluginException {
        if (dl != null && br != null) {
            if (dl.getConnection().getResponseCode() == 403) {
                if ("Blocked by Bitdefender".equalsIgnoreCase(dl.getConnection().getResponseMessage())) {
                    // Bitdefender handling
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by Bitdefender");
                } else if ("Blocked by ESET Security".equalsIgnoreCase(dl.getConnection().getResponseMessage())) {
                    // Eset
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by ESET");
                } else if (dl.getConnection().getHeaderField("Server") != null && "WebGuard".equalsIgnoreCase(dl.getConnection().getHeaderField("Server"))) {
                    // WebGuard jdlog://7294408642041
                    // ----------------Response------------------------
                    // HTTP/1.1 403 Forbidden
                    // Server: WebGuard
                    // Content-Type: text/html; charset=UTF-8
                    // Content-Length: 3218
                    // ------------------------------------------------
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by WebGuard");
                } else if (dl.getConnection().getHeaderField("Server") != null && dl.getConnection().getHeaderField("Server").matches("^Zscaler/.+")) {
                    // Zscaler, corporate firewall/antivirus ? http://www.zscaler.com/ jdlog://2660609980341
                    // ----------------Response------------------------
                    // HTTP/1.1 403 Forbidden
                    // Content-Type: text/html
                    // Server: Zscaler/5.0
                    // Cache-Control: no-cache
                    // Content-length: 10135
                    // ------------------------------------------------

                    // <title>Threat download blocked</title>
                    // ..
                    // <span><font color="black" size=6><p>For security reasons your request was blocked.<p>If you feel you've reached
                    // this
                    // page
                    // in error, contact Helpdesk at the email address below</td>
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by Zscaler");

                } else if (dl.getConnection().getHeaderField("Server") != null && dl.getConnection().getHeaderField("Server").matches("Protected by WireFilter.+")) {
                    // country filter service.. note that the date is wrong!
                    // happens on standard html pages.. not just data..
                    // ----------------Response------------------------
                    // HTTP/1.1 403 Forbidden
                    // Server: Protected by WireFilter 8000 (RYD-WF02-FB01)
                    // Connection: close
                    // Content-Type: text/html
                    // Expires: Sat, 01 Jan 2000 11:11:11 GMT
                    // x,vitruvian: IT
                    // Content-Length: 3679
                    // ------------------------------------------------
                    // @see BlcokedConnections\2227034739341.log
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by your governments Internet filter.");
                }
            }
        }
    }

    /**
     * generic parsing browser content, for browser header blocks look at handleBlockedConnection, for redirect based blocks check
     * handleBlockedRedirect
     *
     * @author raztoki
     * @since JD2
     * @param br
     * @throws CharacterCodingException
     * @throws PluginException
     */
    public void handleBlockedContent(final Browser br) throws PluginException, CharacterCodingException {
        if (br.containsHTML("<div class=\"prodhead\"><div class=\"logoimg\"><span class=\"logotxt\">ESET NOD32 Antivirus</span></div></div>") && br.containsHTML("- ESET NOD32 Antivirus</title>")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by ESET NOD32 Antivirus");
        }
        // limit to size so it can't be abused.
        if (br.getHttpConnection().getLongContentLength() < 1024) {
            // https://board.jdownloader.org/showthread.php?t=64825
            // Link; 9509752095341.log; 289478; jdlog://9509752095341
            // always as full url, only seen as IP address:8080 182.79.218.37
            final String iframe = br.getRegex("<iframe [^>]+src=(\"|')(http?://182\\.79\\.218\\.37:\\d+/webadmin/deny/index\\.php.*?)\\1").getMatch(1);
            if (iframe != null && Encoding.urlDecode(iframe, false).contains(br.getURL())) {
                // Netsweeper Cloud Manager (indian http filtering front end?)
                throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by NetSweeper Cloud Manager");
            }
        } else if (br.getHttpConnection().getLongContentLength() < 1250) {
            // fortinet based blocks, you can usually identify them as they have the same css and heading layouts
            // Link; 9567354739341.log; 285632; jdlog://9567354739341 /JDClosed/src/LogRefences/BlockedConnections/9567354739341.log
            if (br.containsHTML("https?://(?:www\\.)?fortinet\\.com/")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by Forinet Security Services");
            }
            // Link; 5977354739341.log; 288093; jdlog://5977354739341 /JDClosed/src/LogRefences/BlockedConnections/5977354739341.log
            if (br.containsHTML("<h1>The URL you requested has been blocked</h1><p>The page you have requested has been blocked, because the URL is banned.<br />")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by Forinet Security Services");
            }
        }
    }

    /**
     * browser can get redirected to portals/firewall/antivirus/malware software services, we can check here based on URL Pattern.
     *
     * @author raztoki
     * @since JD2
     * @param br
     * @throws PluginException
     */
    protected void handleBlockedRedirect(final String redirect) throws PluginException {
        if (redirect == null) {
            return;
        }
        if (redirect.matches("https?://block\\.malwarebytes\\.org")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by Malwarebytes");
        }
        // local IP based network filters/blocks?
        final String ip = new Regex(redirect, "^https?://(" + IP.IP_PATTERN + ")").getMatch(0);
        if (IP.isLocalIP(ip) && new Regex(redirect, "/cgi-bin/blockpage\\.cgi\\?ws-session=\\d+$").matches()) {
            // websense block. example log jdlog://6965119980341
            throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by Websense");
        }
        if (new Regex(redirect, "^https?://portal\\.almaviva\\.it/UserCheck/PortalMain\\?").matches()) {
            // Link; 0628362095341.log; 269593; jdlog://0628362095341
            // https://support.jdownloader.org/staff/index.php?/Tickets/Ticket/View/45042/inbox/3/6/-1
            throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by almaviva.it portal");
        }
    }

}
