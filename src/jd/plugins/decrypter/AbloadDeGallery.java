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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "abload.de" }, urls = { "https?://(?:www\\.)?abload\\.de/(gallery\\.php\\?key=[A-Za-z0-9]+|browseGallery\\.php\\?gal=[A-Za-z0-9]+\\&img=.+|image.php\\?img=[\\w\\.]+)" })
public class AbloadDeGallery extends PluginForDecrypt {
    public AbloadDeGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    private static final String DIRECTLINKREGEX  = "um die Originalgröße anzuzeigen\\.</div><img src=\"(/img/[^\"]+)\"";
    private static final String DIRECTLINKREGEX2 = "(/img/[^\"]+)\"";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String addedurl = param.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getPage(addedurl);
        if (br.containsHTML("Ein Bild mit diesem Dateinamen existiert nicht\\.") || br.containsHTML(">Dieses Bild wurde gelöscht")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("Galerie nicht gefunden\\.") || br.containsHTML("Gallery not found\\.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!addedurl.contains("browseGallery.php?gal=") && !addedurl.contains("image.php")) {
            final String galID = new Regex(addedurl, "([A-Za-z0-9]+)$").getMatch(0);
            String passCode = null;
            if (getPasswordProtectedForm(br) != null) {
                int counter = 0;
                Form pwform = getPasswordProtectedForm(br);
                do {
                    passCode = getUserInput("Password?", param);
                    pwform.put("pwd", Encoding.urlEncode(passCode));
                    br.submitForm(pwform);
                    pwform = getPasswordProtectedForm(br);
                    if (pwform == null) {
                        logger.info("User entered valid password: " + passCode);
                        break;
                    } else {
                        logger.info("User entered invalid password: " + passCode);
                        counter++;
                        continue;
                    }
                } while (!this.isAbort() && counter <= 2);
                if (pwform != null) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
            }
            // Needed for galleries with ajax-picture-reload function
            String[] urls = br.getRegex("\"filename\":\"([^<>\"]+)\"").getColumn(0);
            // For "normal" galleries
            if (urls == null || urls.length == 0) {
                urls = br.getRegex("\"/browseGallery\\.php\\?gal=[A-Za-z0-9]+\\&amp;img=([^<>\"/]*?)\"").getColumn(0);
            }
            if (urls == null || urls.length == 0 || galID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String fpName = br.getRegex("(?i)<title>\\s*Galerie\\s*:([^<>\"]*?)\\- abload\\.de\\s*</title>").getMatch(0);
            if (fpName == null) {
                /* Fallback */
                fpName = galID;
            }
            fpName = Encoding.htmlDecode(fpName).trim();
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            for (final String url : urls) {
                // singlePictureLink = "https://www.abload.de/browseGallery.php?gal=" + galID + "&img=" +
                // Encoding.htmlDecode(singlePictureLink);
                // br.getPage(singlePictureLink);
                // String finallink = br.getRegex(DIRECTLINKREGEX).getMatch(0);
                // if (finallink == null) {
                // finallink = br.getRegex(DIRECTLINKREGEX2).getMatch(0);
                // }
                // if (finallink == null) {
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                // }
                final String finallink = "https://" + this.getHost() + "/img/" + url;
                final DownloadLink dl = this.createDownloadlink(finallink);
                dl.setFinalFileName(url);
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                if (passCode != null) {
                    dl.setPasswordProtected(true);
                    dl.setDownloadPassword(passCode);
                }
                ret.add(dl);
            }
        } else {
            String finallink = br.getRegex(DIRECTLINKREGEX).getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex(DIRECTLINKREGEX2).getMatch(0);
            }
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ret.add(createDownloadlink("directhttp://" + br.getURL(finallink)));
        }
        return ret;
    }

    private Form getPasswordProtectedForm(final Browser br) {
        return br.getFormbyKey("pwd");
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}