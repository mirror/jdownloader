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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagevenue.com" }, urls = { "https?://(www\\.)?(img\\d+\\.)?imagevenue\\.com/(galshow\\.php\\?gal=gallery_.+|GA[A-Za-z0-9]+)" })
public class ImageVenueComGallery extends PluginForDecrypt {
    public ImageVenueComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String parameter = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final String format = new Regex(parameter, "(format=[A-Za-z0-9]+)").getMatch(0);
        if (format != null) {
            parameter = parameter.replace(format, "format=show");
        }
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String redirect = br.getRedirectLocation();
        if (redirect != null) {
            /* 2020-07-13: New */
            if (redirect.contains("/no_image.jpg")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.setFollowRedirects(true);
            br.getPage(redirect);
        }
        if (br.getHttpConnection().getResponseCode() == 403) {
            /* Invalid link */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] links = br.getRegex("<a href=\"(https?://img\\d+\\.imagevenue\\.com/img\\.php\\?image=[^<>\"]*?)\" target=\"_blank\"><img src=\"").getColumn(0);
        if (links != null) {
            for (String singleLink : links) {
                ret.add(createDownloadlink(singleLink));
            }
        }
        final String images[][] = br.getRegex("<a href\\s*=\\s*\"(https:?//(?:www\\.)?" + getHost() + "/[A-Za-z0-9]+)\"\\s*>\\s*<img[^>]*>\\s*<span>\\s*(.*?)\\s*</span>").getMatches();
        if (images != null) {
            for (String image[] : images) {
                final DownloadLink link = createDownloadlink(image[0]);
                link.setAvailable(true);
                link.setName(image[1]);
                ret.add(link);
            }
        }
        if (ret.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String title = br.getRegex("<title>\\s*(?:ImageVenue.com\\s*-)?\\s*(.*?)\\s*</title>").getMatch(0);
        if (title == null) {
            title = new Regex(parameter, "gal=gallery_(.+)").getMatch(0);
        }
        if (title != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(ret);
        }
        return ret;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}