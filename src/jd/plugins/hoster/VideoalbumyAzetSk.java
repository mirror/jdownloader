//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;

/**
 * @author typek_pb
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videoalbumy.azet.sk" }, urls = { "http://[\\w\\.]*?videoalbumy\\.azet\\.sk/[-a-z0=9]+/[0-9a-zA-Z]+/" }, flags = { 0 })
public class VideoalbumyAzetSk extends PluginForHost {
    private String dlink = null;

    public VideoalbumyAzetSk(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://pomoc.azet.sk/vseobecne-podmienky/";
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        br.getPage(link.getDownloadURL());
        String filename = br.getRegex("<title>VideoAlbumy - (.*?)</title>").getMatch(0);
        if (null == filename || filename.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        StringBuilder linkSB = new StringBuilder("http://213.215.107.64/v/");
        String dlinkPart = new Regex(link.getDownloadURL(), "http://videoalbumy.azet.sk/[-a-z0=9]+/(.*?)/").getMatch(0);
        if (null == dlinkPart || dlinkPart.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        linkSB.append(dlinkPart);
        linkSB.append(".flv");

        dlink = linkSB.toString();
        if (dlink == null || dlink.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        filename = iso88591ToWin1250(filename);
        filename = filename.trim();
        link.setFinalFileName(filename + ".flv");
        br.setFollowRedirects(true);
        try {
            if (!br.openGetConnection(dlink).getContentType().contains("html")) {
                link.setDownloadSize(br.getHttpConnection().getLongContentLength());
                br.getHttpConnection().disconnect();
                return AvailableStatus.TRUE;
            }
        } finally {
            if (br.getHttpConnection() != null) br.getHttpConnection().disconnect();
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    /**
     * Converts txt from ISO-8859-1 to WINDOWS-1250 encoding.
     * 
     * @param txt
     * @return
     */
    private String iso88591ToWin1250(String txt) {
        CharsetDecoder decoder = Charset.forName("WINDOWS-1250").newDecoder();
        CharsetEncoder encoder = Charset.forName("ISO-8859-1").newEncoder();
        try {
            ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(txt));
            CharBuffer cbuf = decoder.decode(bbuf);
            return cbuf.toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }
}
