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

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.MediadeliveryNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porn3dx.com" }, urls = { "https?://(?:www\\.)?porn3dx\\.com/post/(\\d+)(/([a-z0-9\\-_]+))?" })
public class Porn3dxComCrawler extends PluginForDecrypt {
    public Porn3dxComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String postID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String titleSlug = br.getRegex("/post/" + postID + "/([a-z0-9\\-_]+)").getMatch(0);
        String authorSlug = br.getRegex("href=\"https?://[^/]+/tag/([^\"]+)\" class=\"tag-artist\"[^>]*>").getMatch(0);
        if (authorSlug == null) {
            /* 2022-02-08 */
            authorSlug = br.getRegex("class=\"avatar-link\" href=\"https?://[^/]+/([^/\"]+)\"").getMatch(0);
        }
        final String[] newEmbedURLs = br.getRegex("(https?://iframe\\.mediadelivery\\.net/embed/[^\"]+)\"").getColumn(0);
        if (newEmbedURLs.length > 0) {
            /* 2022-02-08 */
            final FilePackage fp = FilePackage.getInstance();
            if (authorSlug != null && titleSlug != null) {
                fp.setName(authorSlug + " - " + titleSlug);
            } else if (titleSlug != null) {
                fp.setName(titleSlug);
            } else {
                /* Fallback */
                fp.setName(postID);
            }
            int index = 0;
            for (final String newEmbedURL : newEmbedURLs) {
                final DownloadLink dl = this.createDownloadlink(newEmbedURL);
                dl.setProperty(MediadeliveryNet.PROPERTY_PORN3DX_POST_ID, postID);
                if (authorSlug != null) {
                    dl.setProperty(MediadeliveryNet.PROPERTY_AUTHOR, authorSlug);
                }
                if (titleSlug != null) {
                    dl.setProperty(MediadeliveryNet.PROPERTY_TITLE, titleSlug);
                }
                dl.setProperty(MediadeliveryNet.PROPERTY_POSITION, (index + 1));
                MediadeliveryNet.setFilename(dl);
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                index++;
            }
            return decryptedLinks;
        }
        final String embedURL = br.getRegex("(https?://[^/]+/videos/embed/[a-f0-9\\-]+)").getMatch(0);
        if (embedURL != null) {
            /* Self-embedded video hosted most likely on tube.porn3dx.com (peertube instance). */
            decryptedLinks.add(createDownloadlink(embedURL));
        } else {
            final String imageURL = br.getRegex("(https?://media\\.[^/]+/post/\\d+/large\\.[a-z]+)").getMatch(0);
            if (imageURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink link = this.createDownloadlink(imageURL);
            if (authorSlug != null && titleSlug != null) {
                link.setFinalFileName(authorSlug + "_" + postID + "_" + titleSlug + Plugin.getFileNameExtensionFromString(imageURL));
            } else if (titleSlug != null) {
                link.setFinalFileName(postID + "_" + titleSlug + Plugin.getFileNameExtensionFromString(imageURL));
            } else {
                link.setFinalFileName(postID + Plugin.getFileNameExtensionFromString(imageURL));
            }
            link.setAvailable(true);
            decryptedLinks.add(link);
        }
        return decryptedLinks;
    }
}
