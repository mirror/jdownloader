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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imx.to" }, urls = { "https?://(?:www\\.)?imx\\.to/g/([a-z0-9]+)" })
public class ImxToGallery extends PluginForDecrypt {
    public ImxToGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)^http://", "https://");
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        final String galleryID = new Regex(contenturl, this.getSupportedLinks()).getMatch(0);
        final String galleryFilesizeStr = br.getRegex("(?i)Size\\s*<span [^>]*>([^<]+)</span>").getMatch(0);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(galleryID) && galleryFilesizeStr == null) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        long galleryFilesize = -1;
        if (galleryFilesizeStr != null) {
            galleryFilesize = SizeFormatter.getSize(galleryFilesizeStr);
        }
        String fpName = br.getRegex("<title>IMX\\.to / ([^<>\"]+)</title>").getMatch(0);
        if (fpName == null) {
            /* Fallback */
            fpName = galleryID;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        final String[] imageIDs = br.getRegex("imx\\.to/i/([a-z0-9]+)").getColumn(0);
        if (imageIDs == null || imageIDs.length == 0) {
            if (galleryFilesize == 0) {
                /* Empty gallery */
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, fpName);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        for (final String imageID : imageIDs) {
            final DownloadLink dl = createDownloadlink("https://" + getHost() + "/i/" + imageID);
            dl._setFilePackage(fp);
            dl.setName(imageID + ".jpg");
            if (imageIDs.length == 1 && galleryFilesize != -1) {
                /* Single image in gallery -> Set filesize of gallery as size of single item. */
                dl.setDownloadSize(galleryFilesize);
            }
            dl.setAvailable(true);
            ret.add(dl);
        }
        return ret;
    }
}
