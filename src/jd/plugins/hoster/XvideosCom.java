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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

//xvideos.com by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xvideos.com" }, urls = { "http://(www\\.)?xvideos\\.com/video[0-9]+" }, flags = { 0 })
public class XvideosCom extends PluginForHost {

    public XvideosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://info.xvideos.com/legal/tos/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String NOCHUNKS = "NOCHUNKS";

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        String dllink = br.getRegex("flv_url=(.*?)\\&").getMatch(0);
        if (dllink == null) dllink = decode(br.getRegex("encoded=(.*?)\\&").getMatch(0));
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = Encoding.htmlDecode(dllink);
        int chunks = 0;
        if (link.getBooleanProperty(XvideosCom.NOCHUNKS, false)) {
            chunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, chunks);
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            /* unknown error, we disable multiple chunks */
            if (link.getBooleanProperty(XvideosCom.NOCHUNKS, false) == false) {
                link.setProperty(XvideosCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    private static String decode(String encoded) {
        if (encoded == null) return null;
        encoded = new String(jd.crypt.Base64.decode(encoded));
        String[] encodArr = encoded.split("-");
        String encodedUrl = "";
        for (String i : encodArr) {
            int charNum = Integer.parseInt(i);
            if (charNum != 0) {
                encodedUrl = encodedUrl + (char) charNum;
            }
        }
        return calculate(encodedUrl);
    }

    private static String calculate(String src) {
        if (src == null) return null;
        String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMabcdefghijklmnopqrstuvwxyzabcdefghijklm";
        String calculated = "";
        int i = 0;
        while (i < src.length()) {
            char character = src.charAt(i);
            int pos = CHARS.indexOf(character);
            if (pos > -1) {
                character = CHARS.charAt(13 + pos);
            }
            calculated = calculated + character;
            i++;
        }
        return calculated;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("Accept-Encoding", "gzip");
        br.getPage(parameter.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            logger.info("Setting new downloadlink: " + br.getRedirectLocation());
            parameter.setUrlDownload(br.getRedirectLocation());
            br.getPage(parameter.getDownloadURL());
        }
        if (br.containsHTML("(This video has been deleted|Page not found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2>([^<>\"]*?)<span class").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?)\\- XVIDEOS\\.COM</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = filename.trim() + ".flv";
        parameter.setFinalFileName(Encoding.htmlDecode(filename));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}