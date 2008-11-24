//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.host;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class VimeoCom extends PluginForHost {
    static private final String AGB = "http://www.vimeo.com/terms";
    private String clipData;
    private String finalURL;

    public VimeoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return AGB;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();

        br.getPage(downloadLink.getDownloadURL() + "?hd=1");
        String swf = br.getRegex("<param name=\"movie\" value=\"(.*?)\" />").getMatch(0);
        String clipID = new Regex(swf, "clip_id=(\\d+?)\\&").getMatch(0);
        String context = new Regex(swf, "\\&context=(.*?)\\&").getMatch(0);

        this.clipData = br.getPage("/moogaloop/load/clip:" + clipID + "/local?param_force_embed=0&param_clip_id=" + clipID + "&param_show_portrait=0&param_multimoog=&param_server=vimeo.com&param_show_title=0&param_autoplay=0&param_show_byline=0&param_color=00ADEF&param_fullscreen=1&param_context=" + context + "&param_md5=0&param_context_id=&context=" + context);
        String title = getClipData("caption");
        downloadLink.setFinalFileName(title + ".flv");     
        
        String dlURL = "/moogaloop/play/clip:" + getClipData("clip_id") + "/" + getClipData("request_signature") + "/" + getClipData("request_signature_expires") + "/?q="+(getClipData("isHD").equals("1")?"hd":"sd");
        br.setFollowRedirects(false);
        br.getPage(dlURL);
        this.finalURL = br.getRedirectLocation();
        br.openGetConnection(finalURL);
        downloadLink.setDownloadSize(br.getRequest().getContentLength());
        if (title == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(title);
        downloadLink.setDupecheckAllowed(true);

        return true;
    }

    private String getClipData(String tag) {
        return new Regex(this.clipData, "<" + tag + ">(.*?)</" + tag + ">").getMatch(0);
    }

    @Override
    public String getVersion() {

        return getVersion("$Revision: 3397 $");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setDebug(true);
        getFileInformation(downloadLink);
        br.openDownload(downloadLink, finalURL, true, 0).startDownload();

    }

    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert nachprüfen */
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}