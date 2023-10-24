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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "orangedox.com" }, urls = { "https?://(?:www\\.)?dl\\.orangedox\\.com/([A-Za-z0-9]+)(/(\\d+))?" })
public class OrangedoxCom extends PluginForDecrypt {
    public OrangedoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        /*
         * Special handling for Google Drive content because they will try to redirect us to direct-URLs but we prefer using the Google
         * Drive plugin to handle such URLs.
         */
        final PluginForHost googleDrivePlugin = this.getNewPluginForHostInstance(jd.plugins.hoster.GoogleDrive.getPluginDomains().get(0)[0]);
        final Regex urlregex = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String contentID = urlregex.getMatch(0);
        final String singleFileInsideFolderID = urlregex.getMatch(2);
        final boolean isFolder = singleFileInsideFolderID == null;
        String nexturl = param.getCryptedUrl() + "?dl=1";
        URLConnectionAdapter con = null;
        int redirects = 0;
        do {
            con = br.openGetConnection(nexturl);
            if (br.getRedirectLocation() == null) {
                break;
            } else if (googleDrivePlugin.canHandle(br.getRedirectLocation())) {
                try {
                    con.disconnect();
                } catch (final Throwable ignore) {
                }
                ret.add(this.createDownloadlink(br.getRedirectLocation()));
                return ret;
            } else {
                logger.info("Redirect to the unknown: " + br.getRedirectLocation());
                nexturl = br.getRedirectLocation();
                redirects++;
            }
        } while (redirects < 10);
        if (this.looksLikeDownloadableContent(con)) {
            try {
                con.disconnect();
            } catch (final Throwable ignore) {
            }
            /* Typically Google Drive URLs. */
            final DownloadLink direct = this.createDownloadlink("directhttp://" + con.getURL().toString());
            if (con.getCompleteContentLength() > 0) {
                direct.setVerifiedFileSize(con.getCompleteContentLength());
            }
            if (con.isContentDisposition()) {
                direct.setFinalFileName(Plugin.getFileNameFromHeader(con));
            }
            direct.setAvailable(true);
            ret.add(direct);
        } else {
            br.setFollowRedirects(true);
            /*
             * Access initially added URL. Important because if we try first with "?dl=1" appended and item is a folder, we might get
             * redirected to a 404 error page.
             */
            br.getPage(param.getCryptedUrl());
            if (br.containsHTML("type=\"password\" name=\"pwd\"")) {
                logger.info("Password protected URLs are not yet supported");
                throw new DecrypterException(DecrypterException.PASSWORD);
            } else if (!isFolder) {
                /* Assume that content is offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                /* Folder -> Look for file items */
                final String[] fileurls = br.getRegex("(/" + contentID + "/\\d+)").getColumn(0);
                if (fileurls == null || fileurls.length == 0) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* These results will go back into this crawler plugin. */
                for (final String fileurl : fileurls) {
                    ret.add(this.createDownloadlink(br.getURL(fileurl).toExternalForm()));
                }
            }
        }
        return ret;
    }
}
