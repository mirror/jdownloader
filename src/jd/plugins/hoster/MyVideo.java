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

import java.util.HashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

// Altes Decrypterplugin bis Revision 14394 
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "myvideo.de" }, urls = { "fromDecrypter://(www\\.)?myvideo\\.(de|at)/watch/\\d+(/\\w+)?" }, flags = { 32 })
public class MyVideo extends PluginForHost {

    private String CLIPURL  = null;
    private String CLIPPATH = null;
    private String SWFURL   = null;
    private String KEY      = "Yzg0MDdhMDhiM2M3MWVhNDE4ZWM5ZGM2NjJmMmE1NmU0MGNiZDZkNWExMTRhYTUwZmIxZTEwNzllMTdmMmI4Mw==";

    public MyVideo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("myvideo.at/", "myvideo.de/").replaceFirst("fromDecrypter", "http"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCustomCharset("utf8");
        br.getPage(downloadLink.getDownloadURL());
        String redirect = br.getRedirectLocation();
        if (redirect != null && "http://www.myvideo.de/".equals(redirect.trim())) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (redirect != null) br.getPage(redirect);
        br.setFollowRedirects(true);

        if (br.containsHTML("Dieser Film ist für Zuschauer unter \\d+ Jahren nicht geeignet")) {
            String ageCheck = br.getRegex("class=\'btnMiddle\'><a href=\'(/iframe.*?)\'").getMatch(0);
            if (ageCheck != null) {
                br.getPage("http://www.myvideo.de" + Encoding.htmlDecode(ageCheck));
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }

        String filename = br.getRegex("name=\\'subject_title\\' value=\\'([^\\'<]+)").getMatch(0);
        if (filename == null) filename = br.getRegex("name=\\'title\\' content=\\'(.*?)(Video)? \\-? (Film|Musik|TV Serie|MyVideo)").getMatch(0);
        if (filename == null) {
            filename = br.getURL();
            if (filename != null) filename = filename.substring(filename.lastIndexOf("/") + 1);
        }
        /* get encUrl */
        HashMap<String, String> p = new HashMap<String, String>();
        String values = br.getRegex("flashvars=\\{(.*?)\\}").getMatch(0);
        for (String[] tmp : new Regex(values == null ? "NPE" : values, "(.*?):\'(.*?)\',").getMatches()) {
            if (tmp.length != 2) {
                continue;
            }
            p.put(tmp[0], tmp[1]);
        }
        if (p.isEmpty() || !p.containsKey("_encxml") || !p.containsKey("ID")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String next = Encoding.htmlDecode(p.get("_encxml")) + "?";
        p.remove("_encxml");
        for (Entry<String, String> valuePair : p.entrySet()) {
            if (!next.endsWith("?")) {
                next = next + "&";
            }
            next = next + valuePair.getKey() + "=" + valuePair.getValue();
        }
        SWFURL = br.getRegex("(SWFObject|embedSWF)\\(\'(.*?)\',").getMatch(1);
        SWFURL = SWFURL == null ? "http://is4.myvideo.de/de/player/mingR11q/ming.swf" : SWFURL;
        br.getPage(next + "&domain=www.myvideo.de");
        String input = br.getRegex("_encxml=(\\w+)").getMatch(0);
        if (input == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        input = input.replaceAll("%0D%0A", "").trim();
        String result;
        JDUtilities.getPluginForDecrypt("linkcrypt.ws");
        try {
            result = decrypt(input, p.get("ID"));
        } catch (Throwable e) {
            e.printStackTrace();
            return AvailableStatus.UNCHECKABLE;
        }
        CLIPURL = new Regex(result, "connectionurl=\'(.*?)\'").getMatch(0);
        CLIPPATH = new Regex(result, "source=\'(.*?)\'").getMatch(0);
        if (CLIPURL.equals("") || CLIPPATH == null) {
            CLIPURL = new Regex(result, "path=\'(.*?)\'").getMatch(0);
        }
        if (CLIPURL == null || CLIPPATH.equals("")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String ext = new Regex(CLIPPATH, "(\\.\\w{3})$").getMatch(0);
        if (!CLIPPATH.matches("(\\w+):(\\w+)/(\\w+)/(\\d+)") && ext != null && !CLIPURL.startsWith("http")) {
            CLIPPATH = CLIPPATH.replace(ext, "");
            if (ext.startsWith(".")) {
                CLIPPATH = ext.replace(".", "") + ":" + CLIPPATH;
            } else {
                CLIPPATH = ext + ":" + CLIPPATH;
            }
        }
        ext = ext == null ? ".mp4" : ext;
        if (filename == null) filename = "unknown_myvideo_title__ID(" + p.get("ID") + ")_" + System.currentTimeMillis();
        filename = filename.replaceAll("\t", "").trim() + ext;
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename));
        /* filesize */
        if (CLIPURL.startsWith("http://")) {
            Browser br2 = br.cloneBrowser();
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(CLIPURL + CLIPPATH);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private String decrypt(String cipher, String id) {
        String key = org.appwork.utils.Hash.getMD5(Encoding.Base64Decode(KEY) + org.appwork.utils.Hash.getMD5(id));
        byte[] ciphertext = JDHexUtils.getByteArray(cipher);
        jd.plugins.decrypter.LnkCrptWs.KeyCaptchaShowDialogTwo arkfour = new jd.plugins.decrypter.LnkCrptWs.KeyCaptchaShowDialogTwo();
        /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
        byte[] plain = arkfour.D(key.getBytes(), ciphertext);
        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
        return Encoding.htmlDecode(new String(plain));
    }

    private void download(DownloadLink downloadLink) throws Exception {
        if (CLIPURL.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, CLIPURL);
            setupRTMPConnection(dl);
            ((RTMPDownload) dl).startDownload();
        } else if (CLIPURL.startsWith("http")) {
            br.getHeaders().put("Referer", SWFURL);
            br.getHeaders().put("x-flash-version", "10,3,183,7");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, CLIPURL + CLIPPATH, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.myvideo.de/AGB";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setupRTMPConnection(DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        String app = CLIPURL.replaceAll("\\w+://[\\w\\.]+/", "");
        if (!CLIPURL.contains("token")) {
            rtmp.setProtocol(0);
        }
        rtmp.setPlayPath(CLIPPATH);
        rtmp.setApp(app);
        rtmp.setUrl(CLIPURL);
        rtmp.setSwfVfy(SWFURL);
        rtmp.setResume(true);
    }

}