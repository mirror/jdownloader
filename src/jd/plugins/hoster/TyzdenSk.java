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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

/**
 * @author typek_pb
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tyzden.sk" }, urls = { "http://[\\w\\.]*?tyzden\\.sk/(video-komentare|lampa|rozhovory-dna|rozhovory-tyzdna|reportaze-tyzdna|ankety-copy-1|ankety|rozhovory-dna-2|pripravujeme0|dokumenty-copy-1|komentare-dna)/[-0-9a-zA-Z]+.html" }, flags = { 0 })
public class TyzdenSk extends PluginForHost {
    private String dlink = null;

    public TyzdenSk(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        // nothing found on site
        return "";
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
        String filename = br.getRegex("<title>(.*?) [|] ").getMatch(0);
        if (null == filename || filename.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        StringBuilder linkSB = new StringBuilder("http://media.monogram.sk/media/file?ac=tyzden&fid");
        String dlinkPart = new Regex(Encoding.htmlDecode(br.toString()), "\"FlashVars\", \"basePath=(.*?)fid(.*?)&").getMatch(1);
        if (null == dlinkPart || dlinkPart.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        linkSB.append(dlinkPart);

        dlink = Encoding.htmlDecode(linkSB.toString());
        if (dlink == null || dlink.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

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

}
