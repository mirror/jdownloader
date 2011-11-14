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
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vkontakte.ru" }, urls = { "http://vkontaktedecrypted\\.ru/picturelink/\\d+_\\d+" }, flags = { 0 })
public class VKontakteRuHoster extends PluginForHost {

    public VKontakteRuHoster(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://vkontakte.ru/help.php?page=terms";
    }

    private static final String DOMAIN = "vkontakte.ru";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        /**
         * Decrypter will always have working cookies so we can just get em from
         * // there ;)
         */
        final PluginForDecrypt vkontakteDecrypter = JDUtilities.getPluginForDecrypt("vkontakte.ru");
        final Object ret = vkontakteDecrypter.getPluginConfig().getProperty("cookies", null);
        String albumID = downloadLink.getStringProperty("albumid");
        String photoID = new Regex(downloadLink.getDownloadURL(), "vkontaktedecrypted\\.ru/picturelink/(\\d+_\\d+)").getMatch(0);
        if (ret == null || albumID == null || photoID == null) {
            // This should never happen
            logger.warning("A property couldn't be found!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final HashMap<String, String> cookies = (HashMap<String, String>) ret;
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            this.br.setCookie(DOMAIN, entry.getKey(), entry.getValue());
        }
        br.postPage("http://vk.com/al_photos.php", "act=show&al=1&list=" + albumID + "&photo=" + photoID);
        String correctedBR = br.toString().replace("\\", "");
        /** Try to get best quality */
        String finallink = new Regex(correctedBR, "\"id\":\"" + photoID + "\",\"w_src\":\"(http://.*?)\"").getMatch(0);
        if (finallink == null) {
            finallink = new Regex(correctedBR, "\"id\":\"" + photoID + "\",\"x_src\":\"http://[^\"\\']+\",\"y_src\":\"http://[^\"\\']+\",\"z_src\":\"http://[^\"\\']+\",\"w_src\":\"(http://.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = new Regex(correctedBR, "\"id\":\"" + photoID + "\",\"x_src\":\"http://[^\"\\']+\",\"y_src\":\"http://[^\"\\']+\",\"z_src\":\"(http://.*?)\"").getMatch(0);
                if (finallink == null) {
                    finallink = new Regex(correctedBR, "\"id\":\"" + photoID + "\",\"x_src\":\"http://[^\"\\']+\",\"y_src\":\"(http://.*?)\"").getMatch(0);
                    if (finallink == null) {
                        finallink = new Regex(correctedBR, "\"id\":\"" + photoID + "\",\"x_src\":\"(http://.*?)\"").getMatch(0);
                    }
                }
            }
        }
        if (finallink == null) {
            logger.warning("Finallink is null for photoID: " + photoID);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /**
         * Chunks disabled because (till now) this plugin only exists to
         * download pictures
         */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}