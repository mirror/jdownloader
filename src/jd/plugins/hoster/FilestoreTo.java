//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;
import jd.plugins.components.UserAgents.BrowserName;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filestore.to" }, urls = { "http://(www\\.)?filestore\\.to/\\?d=[A-Z0-9]+" })
public class FilestoreTo extends PluginForHost {

    private String aBrowser = "";

    public FilestoreTo(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2000l);

    }

    @Override
    public String getAGBLink() {
        return "http://www.filestore.to/?p=terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2000;
    }

    private static AtomicReference<String> agent = new AtomicReference<String>(null);

    private Browser prepBrowser(Browser prepBr) {
        if (agent.get() == null) {
            agent.set(UserAgents.stringUserAgent(BrowserName.Chrome));
        }
        prepBr.getHeaders().put("User-Agent", agent.get());
        prepBr.setCustomCharset("utf-8");
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        prepBrowser(br);
        final String url = downloadLink.getDownloadURL();
        String downloadName = null;
        String downloadSize = null;
        for (int i = 1; i < 3; i++) {
            try {
                br.getPage(url);
            } catch (final Exception e) {
                continue;
            }
            if (br.containsHTML(">Download-Datei wurde nicht gefunden<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML(">Download-Datei wurde gesperrt<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML("Entweder wurde die Datei von unseren Servern entfernt oder der Download-Link war")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            haveFun();
            downloadName = new Regex(aBrowser, "\\s*(File:|Filename:?)\\s*(.*?)\\s*(Dateigr??e|(File)?size|Gr??e):?\\s*(\\d+(,\\d+)? (B|KB|MB|GB))").getMatch(1);
            if (downloadName == null) {
                downloadName = new Regex(aBrowser, "und starte dann den Download\\.\\.\\.\\.\\s*[A-Za-z]+:?\\s*([^<>\"/]*\\.(3gp|7zip|7z|abr|ac3|aiff|aifc|aif|ai|au|avi|bin|bat|bz2|cbr|cbz|ccf|chm|cso|cue|cvd|dta|deb|divx|djvu|dlc|dmg|doc|docx|dot|eps|epub|exe|ff|flv|flac|f4v|gsd|gif|gz|iwd|idx|iso|ipa|ipsw|java|jar|jpg|jpeg|load|m2ts|mws|mv|m4v|m4a|mkv|mp2|mp3|mp4|mobi|mov|movie|mpeg|mpe|mpg|mpq|msi|msu|msp|nfo|npk|oga|ogg|ogv|otrkey|par2|pkg|png|pdf|pptx|ppt|pps|ppz|pot|psd|qt|rmvb|rm|rar|ram|ra|rev|rnd|[r-z]\\d{2}|r\\d+|rpm|run|rsdf|reg|rtf|shnf|sh(?!tml)|ssa|smi|sub|srt|snd|sfv|swf|tar\\.gz|tar\\.bz2|tar\\.xz|tar|tgz|tiff|tif|ts|txt|viv|vivo|vob|webm|wav|wmv|wma|xla|xls|xpi|zeno|zip|z\\d+|_[_a-z]{2}))").getMatch(0);
            }
            downloadSize = new Regex(aBrowser, "(Dateigr??e|(File)?size|Gr??e):?\\s*(\\d+(,\\d+)? (B|KB|MB|GB))").getMatch(1);
            if (downloadSize == null) {
                downloadSize = new Regex(aBrowser, "(\\d+(,\\d+)? (B|KB|MB|GB))").getMatch(0);
            }
            if (downloadName != null) {
                downloadLink.setName(Encoding.htmlDecode(downloadName.trim()));
            }
            if (downloadSize != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(downloadSize.replaceAll(",", "\\.").trim()));
            }
            return AvailableStatus.TRUE;

        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // lets check they at times have issues
        if (br.containsHTML(Pattern.quote(">Der Download ist nicht bereit !</span><br />Die Datei wird noch auf die Server verteilt.<br />Bitte versuche es in ein paar Minuten erneut.<"))) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
        }
        removeme
        {
            // form 1
            final Form f = br.getFormByRegex(">Download</button>");
            if (f != null) {
                if ("".equals(f.getAction())) {
                    f.setAction(br.getURL());
                }
                br.submitForm(f);
            }
        }
        {
            // form 2
            final Form f = br.getFormByRegex(">Download starten</button>");
            if (f != null) {
                if ("".equals(f.getAction())) {
                    f.setAction(br.getURL());
                }
                br.submitForm(f);
            }
        }
        final String dllink = getDllink();
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        processWait();
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Derzeit haben wir Serverprobleme und arbeiten daran\\. Bitte nochmal versuchen\\.")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server issues", 15 * 60 * 1000l);
            }
            if (br.containsHTML("Derzeit haben wir leider keinen freien Downloadslots frei\\. Bitte nochmal versuchen\\.")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void processWait() throws PluginException {
        final String waittime = br.getRegex("data-wait=\"(\\d+)\"").getMatch(0);
        int wait = 10;
        if (waittime != null && Integer.parseInt(waittime) < 61) {
            wait = Integer.parseInt(waittime);
        }
        sleep(wait * 1001l, getDownloadLink());

    }

    private String getDllink() {
        String dllink = br.getRegex("<a href=(\"|')(.*?)\\1").getMatch(1);
        if (dllink == null) {
            dllink = br.getRegex("<iframe class=\"downframe\" src=\"(.*?)\"").getMatch(0);
        }
        return dllink;
    }

    private Browser prepAjax(Browser prepBr) {
        prepBr.getHeaders().put("Accept", "*/*");
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("X-Requested-With:", "XMLHttpRequest");
        return prepBr;
    }

    public void haveFun() throws Exception {
        aBrowser = br.toString();
        aBrowser = aBrowser.replaceAll("(<(p|div)[^>]+(display:none|top:-\\d+)[^>]+>.*?(<\\s*(/\\2\\s*|\\2\\s*/\\s*)>){2})", "");
        aBrowser = aBrowser.replaceAll("(<(table).*?class=\"hide\".*?<\\s*(/\\2\\s*|\\2\\s*/\\s*)>)", "");
        aBrowser = aBrowser.replaceAll("[\r\n\t]+", " ");
        aBrowser = aBrowser.replaceAll("&nbsp;", " ");
        aBrowser = aBrowser.replaceAll("(<[^>]+>)", " ");
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
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

}