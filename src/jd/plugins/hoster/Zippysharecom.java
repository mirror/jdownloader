//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zippyshare.com" }, urls = { "http://www\\d{0,}\\.zippyshare\\.com/(d/\\d+/\\d+/.|v/\\d+/[^<>\"/]*?\\.html?|.*?key=\\d+|downloadMusic\\?key=\\d+|swf/player_local\\.swf\\?file=\\d+)" }, flags = { 0 })
public class Zippysharecom extends PluginForHost {

    public Zippysharecom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String addedLink = link.getDownloadURL();
        if (addedLink.matches("http://www\\d{0,}\\.zippyshare\\.com/downloadMusic\\?key=\\d+")) {
            link.setUrlDownload(addedLink.replace("downloadMusic?key=", "view.jsp?key="));
        } else if (addedLink.matches("http://www\\d{0,}\\.zippyshare\\.com/swf/player_local\\.swf\\?file=\\d+")) {
            final Regex linkInfo = new Regex(addedLink, "http://(www\\d{0,})\\.zippyshare\\.com/swf/player_local\\.swf\\?file=(\\d+)");
            final String server = linkInfo.getMatch(0);
            final String fid = linkInfo.getMatch(1);
            link.setUrlDownload("http://" + server + ".zippyshare.com/v/" + fid + "/file.html");
        } else if (addedLink.matches("http://www\\d{0,}\\.zippyshare\\.com/d/\\d+/\\d+/.*?")) {
            final Regex linkInfo = new Regex(addedLink, "http://(www\\d{0,})\\.zippyshare\\.com/d/(\\d+)");
            final String server = linkInfo.getMatch(0);
            final String fid = linkInfo.getMatch(1);
            link.setUrlDownload("http://" + server + ".zippyshare.com/v/" + fid + "/file.html");
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        prepareBrowser(downloadLink);
        if (br.containsHTML("(File has expired and does not exist anymore on this server|<title>Zippyshare.com \\- File does not exist</title>|File does not exist on this server)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        String filename = br.getRegex("<title>Zippyshare\\.com \\- (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex(Pattern.compile("Name:(\\s+)?</font>(\\s+)?<font style=.*?>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(2);
        }
        if (filename != null && filename.contains("fileName?key=")) {
            filename = null;
        }
        if (filename == null) {
            filename = br.getRegex("<input value=\"\\[url=[^\\]]+\\](.*?)\\[/url\\]").getMatch(0);
        }
        if (filename == null) {
            final String var = br.getRegex("var fulllink.*?'\\+(.*?)\\+'").getMatch(0);
            filename = br.getRegex("'\\+" + var + "\\+'/(.*?)';").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("document\\.getElementById\\(\\'dlbutton\\'\\)\\.href(.*?\";)").getMatch(0);
            if (filename != null) {
                filename = new Regex(filename, "/([^<>\"]*?)\";").getMatch(0);
            }
        }

        if (filename == null || filename.contains("/fileName?key=")) {
            String url = br.getRegex("document\\.location = '(/d/[^<>\"]+';)").getMatch(0);
            if (url != null) {
                filename = new Regex(url, "d/\\d+/\\d+/([^<>\"]+)';").getMatch(0);
            } else {
                url = br.getRegex("dlbutton'(?:.*?)\"/d/\\d+/.*?/(.*?)\"").getMatch(0);
                if (url != null) {
                    filename = url;
                }
            }
        }

        if (filename == null) {
            filename = br.getRegex("\\+\"/(.*?)\";").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        if (!br.containsHTML(">Share movie:")) {
            final String filesize = br.getRegex(Pattern.compile("Size:(\\s+)?</font>(\\s+)?<font style=.*?>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(2);
            if (filesize != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public String getAGBLink() {
        return "http://www.zippyshare.com/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        if (!JD2required.get() && !isJD2()) {
            showJD2requirement(this.getHost());
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "You need to use JDownloader 2!");
    }

    private static AtomicReference<String> agent = new AtomicReference<String>(null);

    private void prepareBrowser(final DownloadLink downloadLink) throws IOException {
        if (agent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        br.getHeaders().put("User-Agent", agent.get());
        br.setCookie("http://www.zippyshare.com", "ziplocale", "en");
        br.getPage(downloadLink.getDownloadURL().replaceAll("locale=..", "locale=en"));
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
    }

    private static AtomicBoolean JD2required = new AtomicBoolean(false);

    public static void showJD2requirement(final String domain) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        boolean xSystem = CrossSystem.isOpenBrowserSupported();
                        title = "JDownloader 2 Dependancy";
                        message = "In order to use this plugin you will need to use JDownloader 2.\r\n";
                        if (xSystem) {
                            message += "JDownloader 2 install instructions and download link: Click -OK- (open in browser)\r\n ";
                        } else {
                            message += "JDownloader 2 install instructions and download link:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                        }
                        int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.CLOSED_OPTION, JOptionPane.CLOSED_OPTION);
                        if (xSystem && JOptionPane.OK_OPTION == result) {
                            CrossSystem.openURL(new URL("http://board.jdownloader.org/showthread.php?t=37365"));
                        }
                        JD2required.set(true);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private boolean isJD2() {
        if (System.getProperty("jd.revision.jdownloaderrevision") != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {

    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }

}