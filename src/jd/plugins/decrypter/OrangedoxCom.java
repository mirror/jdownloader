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
package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "orangedox.com" }, urls = { "https?://(?:www\\.)?dl\\.orangedox\\.com/([A-Za-z0-9]+)" })
public class OrangedoxCom extends PluginForDecrypt {
    public OrangedoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        // br.getPage(parameter);
        // if (br.getHttpConnection().getResponseCode() == 404) {
        // decryptedLinks.add(this.createOfflinelink(parameter));
        // return decryptedLinks;
        // }
        // br.setFollowRedirects(true);
        // br.getPage(br.getURL() + "?dl=1");
        final URLConnectionAdapter con = br.openGetConnection(param.getCryptedUrl());
        if (this.looksLikeDownloadableContent(con)) {
            try {
                con.disconnect();
            } catch (final Throwable ignore) {
            }
            final DownloadLink direct = this.createDownloadlink("directhttp://" + con.getURL().toString());
            if (con.getCompleteContentLength() > 0) {
                direct.setVerifiedFileSize(con.getCompleteContentLength());
            }
            if (con.isContentDisposition()) {
                direct.setFinalFileName(Plugin.getFileNameFromHeader(con));
            }
            direct.setAvailable(true);
            decryptedLinks.add(direct);
        } else {
            br.followConnection();
            if (br.containsHTML("type=\"password\" name=\"pwd\"")) {
                logger.info("Password protected URLs are not yet supported");
                throw new DecrypterException(DecrypterException.PASSWORD);
            } else {
                logger.info("Failed to find any content");
            }
        }
        return decryptedLinks;
    }
}
