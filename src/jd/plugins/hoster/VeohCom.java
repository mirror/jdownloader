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

import java.io.IOException;

import jd.PluginWrapper;
import jd.crypt.Base64;
import jd.crypt.JDCrypt;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "veoh.com" }, urls = { "http://(www\\.)?veoh.com/browse/videos/category/.*?/watch/[A-Za-z0-9]+" }, flags = { 0 })
public class VeohCom extends PluginForHost {

    // Important: If this key changes we also have to change it ;)
    private static final String APIKEY = "NTY5Nzc4MUUtMUM2MC02NjNCLUZGRDgtOUI0OUQyQjU2RDM2";
    private static final String IV     = "ZmY1N2NlYzMwYWVlYTg5YTBmNTBkYjQxNjRhMWRhNzI=";
    private static final String SKEY   = "ODY5NGRmY2RkODY0Y2FhYWM4OTAyZDdlYmQwNGVkYWU=";

    public VeohCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.veoh.com/corporate/termsofuse";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // decrase Browserrequests
        final Browser br2 = br.cloneBrowser();
        // Webplayerrequest
        br2.getPage("http://www.veoh.com/static/swf/webplayer/VWPBeacon.swf?port=50246&version=1.2.2.1112");

        final String fbsetting = br.getRegex("FB\\.init\\(\"(.*?)\"").getMatch(0);
        final String videoID = new Regex(downloadLink.getDownloadURL(), "/watch/(.+)").getMatch(0);
        br.getPage("http://www.veoh.com/rest/v2/execute.xml?apiKey=" + Encoding.Base64Decode(APIKEY) + "&method=veoh.video.findByPermalink&permalink=" + videoID + "&");
        if (br.containsHTML("(<rsp stat=\"fail\"|\"The video does not exist\"|name=\"YouTube\\.com\" type=\"\")")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        // Fileextension
        if (!downloadLink.getName().matches(".+\\.[\\w]{1,3}$")) {
            final String extension = br.getRegex("extension=\"(.*?)\"").getMatch(0);
            downloadLink.setName(downloadLink.getName() + extension);
        }
        // Finallinkparameter
        final String fHashPath = br.getRegex("fullHashPath=\"(.*?)\"").getMatch(0);
        String fHashToken = br.getRegex("fullHashPathToken=\"(.*?)\"").getMatch(0);
        // Decrypt fHashToken
        try {
            fHashToken = JDCrypt.decrypt(JDHexUtils.getByteArray(JDHexUtils.getHexString(Base64.decode(fHashToken))), JDHexUtils.getByteArray(Encoding.Base64Decode(SKEY)), JDHexUtils.getByteArray(Encoding.Base64Decode(IV)));
        } catch (final Throwable e) {
        }
        if (fHashPath == null || fHashToken == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        // I am a flashplayer
        br.setCookie("http://www.veoh.com", "fbsetting_" + fbsetting, "%7B%22connectState%22%3A2%2C%22oneLineStorySetting%22%3A3%2C%22shortStorySetting%22%3A3%2C%22inFacebook%22%3Afalse%7D");
        br.setCookie("http://www.veoh.com", "base_domain_" + fbsetting, "veoh.com");
        br.getHeaders().put("Referer", "http://www.veoh.com/static/swf/qlipso/production/MediaPlayer.swf?version=2.0.0.011311.5");
        br.getHeaders().put("x-flash-version", "10,1,53,64");

        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, fHashPath + fHashToken, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() != 403) {
                br.followConnection();
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Dieses Video ist nicht mehr verf&uuml;gbar")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String filename = br.getRegex("title\":\"(.*?)\"").getMatch(0);
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
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
