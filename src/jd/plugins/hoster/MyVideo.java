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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;

// Altes Decrypterplugin bis Revision 14394 
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "myvideo.de" }, urls = { "http://(www\\.)?myvideo\\.de/watch/\\d+/\\w+" }, flags = { PluginWrapper.DEBUG_ONLY })
public class MyVideo extends PluginForHost {

    private String       CLIPURL  = null;
    private String       CLIPPATH = null;
    private String       SWFURL   = null;
    private final String KEY      = "Yzg0MDdhMDhiM2M3MWVhNDE4ZWM5ZGM2NjJmMmE1NmU0MGNiZDZkNWExMTRhYTUwZmIxZTEwNzllMTdmMmI4Mw==";

    public MyVideo(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String decrypt(final String cipher, final String id) {
        final String key = org.appwork.utils.Hash.getMD5(Encoding.Base64Decode(KEY) + org.appwork.utils.Hash.getMD5(id));
        final byte[] ciphertext = JDHexUtils.getByteArray(cipher);
        final jd.crypt.RC4 rc4 = new jd.crypt.RC4();
        final byte[] plain = rc4.decrypt(key.getBytes(), ciphertext);
        return Encoding.htmlDecode(new String(plain));
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
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (CLIPURL.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, CLIPURL);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            final String app = CLIPURL.replaceAll("\\w+://[\\w\\.]+/", "");
            if (!CLIPURL.contains("token")) {
                rtmp.setProtocol(0);
            }

            rtmp.setPlayPath(CLIPPATH);
            rtmp.setApp(app);
            rtmp.setUrl(CLIPURL);
            rtmp.setSwfVfy(SWFURL);
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setResume(true);

            ((RTMPDownload) dl).startDownload();

        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());

        if (br.containsHTML("Dieser Film ist für Zuschauer unter \\d+ Jahren nicht geeignet")) {
            final String ageCheck = br.getRegex("class=\'btnMiddle\'><a href=\'(/iframe.*?)\'").getMatch(0);
            if (ageCheck != null) {
                br.getPage("http://www.myvideo.de" + Encoding.htmlDecode(ageCheck));
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }

        String filename = Encoding.htmlDecode(br.getRegex("name=\'title\' content=\'(.*?)-? (Film|Musik|TV Serie)").getMatch(0));
        // get encUrl
        final HashMap<String, String> p = new HashMap<String, String>();
        final String[][] encUrl = br.getRegex("p\\.addVariable\\('(.*?)',\\s?'(.*?)'\\)").getMatches();
        for (final String[] tmp : encUrl) {
            if (tmp.length != 2) {
                continue;
            }
            p.put(tmp[0], tmp[1]);
        }
        if (p.isEmpty() || !p.containsKey("_encxml") || !p.containsKey("ID")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String bla = Encoding.htmlDecode(p.get("_encxml")) + "?";
        p.remove("_encxml");
        for (final Entry<String, String> next : p.entrySet()) {
            if (!bla.endsWith("?")) {
                bla = bla + "&";
            }
            bla = bla + next.getKey() + "=" + next.getValue();
        }
        SWFURL = br.getRegex("SWFObject\\(\'(.*?)\',").getMatch(0);
        SWFURL = SWFURL == null ? "http://is2.myvideo.de/de/player/mingR10i/ming.swf" : SWFURL;
        br.getPage(bla);
        final String input = br.getRegex("_encxml=(\\w+)").getMatch(0);
        if (input == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        String result;
        try {
            result = decrypt(input, p.get("ID"));
        } catch (final Throwable e) {
            return AvailableStatus.UNCHECKABLE;
        }
        CLIPURL = new Regex(result, "connectionurl=\'(.*?)\'").getMatch(0);
        CLIPPATH = new Regex(result, "source=\'(.*?)\'").getMatch(0);
        if (CLIPURL == null || CLIPPATH == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String ext = new Regex(CLIPPATH, "(\\.\\w{3})$").getMatch(0);
        if (!CLIPPATH.matches("(\\w+):(\\w+)/(\\w+)/(\\d+)") && ext != null) {
            CLIPPATH = CLIPPATH.replace(ext, "");
            if (ext.startsWith(".")) {
                CLIPPATH = ext.replace(".", "") + ":" + CLIPPATH;
            } else {
                CLIPPATH = ext + ":" + CLIPPATH;
            }
        }
        ext = ext == null ? ".mp4" : ext;
        if (filename == null) {
            filename = new Regex(result, "description=\'(.*?):").getMatch(0);
        }
        filename = filename.trim() + ext;
        downloadLink.setFinalFileName(filename);
        return AvailableStatus.TRUE;
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
