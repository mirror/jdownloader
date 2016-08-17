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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/**
 * @author typek_pb
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tv.sme.sk" }, urls = { "http://(?:www\\.)?tv\\.sme\\.sk/v/[0-9]+/[\\-a-zA-Z0-9]+\\.html" }) 
public class TvSmeSk extends PluginForHost {

    private String dlink = null;

    public TvSmeSk(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.sme.sk/dok/faq/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String fid = new Regex(link.getDownloadURL(), "/v/([0-9]+)").getMatch(0);
        br.getPage(link.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML(">Neexistujúce video")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        String filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        if (filename == null) {
            /* Fallback to url-filename */
            filename = new Regex(link.getDownloadURL(), "([\\-a-zA-Z0-9]+)\\.html$").getMatch(0);
        }

        br.getPage("http://tv.sme.sk/storm-sme-crd/mmdata_get.asp?vp=2&id=" + fid + "&hd1=1");

        dlink = br.getRegex("<location>(.*?)</location>").getMatch(0);
        if (null == dlink || dlink.trim().length() == 0) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        filename = Encoding.htmlDecode(filename).trim();
        // filename = iso88591ToWin1250(filename);
        if (filename == null || filename.trim().length() == 0) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        link.setFinalFileName(filename + ".mp4");
        br.setFollowRedirects(true);
        try {
            if (!br.openHeadConnection(dlink).getContentType().contains("html")) {
                link.setDownloadSize(br.getHttpConnection().getLongContentLength());
                br.getHttpConnection().disconnect();
                return AvailableStatus.TRUE;
            }
        } finally {
            if (br.getHttpConnection() != null) {
                br.getHttpConnection().disconnect();
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dlink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    // /**
    // * Converts txt from ISO-8859-1 to WINDOWS-1250 encoding.
    // *
    // * @param txt
    // * @return
    // */
    // private String iso88591ToWin1250(String txt) {
    // CharsetDecoder decoder = Charset.forName("WINDOWS-1250").newDecoder();
    // CharsetEncoder encoder = Charset.forName("ISO-8859-1").newEncoder();
    // try {
    // ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(txt));
    // CharBuffer cbuf = decoder.decode(bbuf);
    // return cbuf.toString();
    // } catch (CharacterCodingException e) {
    // return null;
    // }
    // }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}