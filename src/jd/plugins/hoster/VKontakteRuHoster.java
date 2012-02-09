//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.Map;

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
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

//Links are coming from a decrypter
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vkontakte.ru" }, urls = { "http://vkontaktedecrypted\\.ru/picturelink/(\\-)?\\d+_\\d+" }, flags = { 0 })
public class VKontakteRuHoster extends PluginForHost {

    private static final String DOMAIN    = "vk.com";

    private String              FINALLINK = null;

    public VKontakteRuHoster(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://vk.com/help.php?page=terms";
    }

    private boolean linkOk(DownloadLink downloadLink) throws IOException {
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(FINALLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
            } else {
                return false;
            }
            return true;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /**
         * Chunks disabled because (till now) this plugin only exists to
         * download pictures
         */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, FINALLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        FINALLINK = null;
        this.setBrowserExclusive();

        br.setFollowRedirects(false);
        /**
         * Decrypter will always have working cookies so we can just get em from
         * // there ;)
         */
        final PluginForDecrypt vkontakteDecrypter = JDUtilities.getPluginForDecrypt("vkontakte.ru");
        final Object ret = vkontakteDecrypter.getPluginConfig().getProperty("cookies", null);
        String albumID = link.getStringProperty("albumid");
        String photoID = new Regex(link.getDownloadURL(), "vkontaktedecrypted\\.ru/picturelink/((\\-)?\\d+_\\d+)").getMatch(0);
        if (ret == null || albumID == null || photoID == null) {
            // This should never happen
            logger.warning("A property couldn't be found!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!albumID.startsWith("album")) albumID = "album" + albumID;
        final HashMap<String, String> cookies = (HashMap<String, String>) ret;
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            this.br.setCookie(DOMAIN, entry.getKey(), entry.getValue());
        }
        br.getPage("http://vk.com/photo" + photoID);
        /* seems we have to refesh the login process */
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        br.postPage("http://vk.com/al_photos.php", "act=show&al=1&module=photos&list=" + albumID + "&photo=" + photoID);
        final String correctedBR = br.toString().replace("\\", "");
        /**
         * Try to get best quality and test links till a working link is found
         * as it can happen that the found link is offline but others are online
         */
        String[] qs = { "w_", "z_", "y_", "x_", "m_" };
        for (String q : qs) {
            /* large image */
            if (FINALLINK == null || (FINALLINK != null && !linkOk(link))) {
                String base = new Regex(correctedBR, "\"id\":\"" + photoID + "\",\"base\":\"(http://.*?)\"").getMatch(0);
                if (base != null) FINALLINK = new Regex(correctedBR, "\"id\":\"" + photoID + "\",\"base\":\"" + base + "\".*?\"" + q + "src\":\"(" + base + ".*?)\"").getMatch(0);
            } else {
                break;
            }
        }
        if (FINALLINK == null) {
            logger.warning("Finallink is null for photoID: " + photoID);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return AvailableStatus.TRUE;

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}