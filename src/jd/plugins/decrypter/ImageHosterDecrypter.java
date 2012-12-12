//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pic4free.org", "cocoimage.com", "imagetwist.com", "postimage.org", "imgur.com", "imgchili.com", "pimpandhost.com", "turboimagehost.com", "imagehyper.com", "imagebam.com", "photobucket.com", "freeimagehosting.net", "pixhost.org", "sharenxs.com", "9gag.com" }, urls = { "http://(www\\.)?pic4free\\.org/\\?v=[^<>\"/]+", "http://(www\\.)?img\\d+\\.cocoimage\\.com/img\\.php\\?id=\\d+", "http://(www\\.)?imagetwist\\.com/[a-z0-9]{12}", "http://(www\\.)?postimage\\.org/image/[a-z0-9]+", "http://((www|i)\\.)?imgur\\.com(/gallery|/a|/download)?/(?!register|contact|removalrequest|stats|https)[A-Za-z0-9]{5,}", "http://(www\\.)?imgchili\\.com/show/\\d+/[a-z0-9_\\.]+", "http://(www\\.)?pimpandhost\\.com/image/(show/id/\\d+|\\d+\\-(original|medium|small)\\.html)", "http://(www\\.)?turboimagehost\\.com/p/\\d+/.*?\\.html",
        "http://(www\\.)?(img\\d+|serve)\\.imagehyper\\.com/img\\.php\\?id=\\d+\\&c=[a-z0-9]+", "http://[\\w\\.]*?imagebam\\.com/(image|gallery)/[a-z0-9]+", "http://(www\\.)?(media\\.photobucket.com/image/.+\\..{3,4}\\?o=[0-9]+|gs\\d+\\.photobucket\\.com/groups/[A-Za-z0-9]+/[A-Za-z0-9]+/\\?action=view\\&current=[^<>\"/]+)", "http://[\\w\\.]*?freeimagehosting\\.net/image\\.php\\?.*?\\..{3,4}", "http://(www\\.)?pixhost\\.org/show/\\d+/.+", "http://(www\\.)?sharenxs\\.com/view/\\?id=[a-z0-9-]+", "https?://(www\\.)?9gag\\.com/gag/\\d+" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class ImageHosterDecrypter extends PluginForDecrypt {

    public ImageHosterDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        String finallink = null;
        String finalfilename = null;
        if (parameter.contains("imagebam.com")) {
            br.getPage(parameter);
            /* Error handling */
            if (br.containsHTML("The gallery you are looking for")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML("Image not found|>Image violated our terms of service|>The requested image could not be located")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (parameter.contains("/gallery/")) {
                br.getPage(parameter);
                String name = new Regex(parameter, "/gallery/(.+)").getMatch(0);
                if (name == null) {
                    name = "ImageBamGallery";
                } else {
                    name = "ImageBamGallery_" + name;
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(name);
                final String pages[] = br.getRegex("class=\"pagination_(current|link)\">(\\d+)<").getColumn(1);
                if (pages != null && pages.length > 0) {
                    for (final String page : pages) {
                        br.getPage(parameter + "/" + page);
                        if (br.containsHTML("The gallery you are looking for")) {
                            continue;
                        }
                        final String links[] = br.getRegex("\\'(http://[\\w\\.]*?imagebam\\.com/image/[a-z0-9]+)\\'").getColumn(0);
                        for (final String link : links) {
                            final DownloadLink dl = handleImageBam(br, Encoding.htmlDecode(link), true);
                            if (dl != null) decryptedLinks.add(dl);
                        }
                    }
                } else {
                    final String links[] = br.getRegex("\\'(http://[\\w\\.]*?imagebam\\.com/image/[a-z0-9]+)\\'").getColumn(0);
                    for (final String link : links) {
                        final DownloadLink dl = handleImageBam(br, Encoding.htmlDecode(link), true);
                        if (dl != null) decryptedLinks.add(dl);
                    }
                }
                if (decryptedLinks.size() > 0) {
                    fp.addLinks(decryptedLinks);
                    return decryptedLinks;
                } else {
                    return null;
                }
            }
            DownloadLink dl = handleImageBam(br, null, false);
            if (dl != null) {
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
        } else if (parameter.contains("photobucket.com")) {
            br.getPage(parameter);
            finallink = br.getRegex("mediaUrl(\\')?: ?(\\'|\")(http.*?)(\\'|\")").getMatch(2);
        } else if (parameter.contains("freeimagehosting.net")) {
            br.getPage(parameter);
            /* Error handling */
            if (!br.containsHTML("uploads/")) return decryptedLinks;
            finallink = parameter.replace("image.php?", "uploads/");
        } else if (parameter.contains("pixhost.org")) {
            br.getPage(parameter);
            /* Error handling */
            if (!br.containsHTML("images/")) return decryptedLinks;
            finallink = br.getRegex("show_image\" src=\"(http.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(http://img[0-9]+\\.pixhost\\.org/images/[0-9]+/.*?)\"").getMatch(0);
            }
        } else if (parameter.contains("sharenxs.com/")) {
            br.getPage(parameter);
            finallink = br.getRegex("<a  href=\"#\" onclick=\\'imgsize\\(\\)\\' ><img[\t\n\r ]+src=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://cache\\.sharenxs\\.com/images/[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("imagehyper.com/img")) {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            finallink = br.getRegex("<img class=\"mainimg\" id=\"mainimg\" src=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(http://img\\d+\\.imagehyper\\.com/img/.*?)\"").getMatch(0);
            }
            if (finallink != null) {
                String ext = finallink.substring(finallink.lastIndexOf("."));
                if (ext == null || ext.length() > 5) ext = ".jpg";
                finalfilename = new Regex(parameter, "([a-z0-9]+)$").getMatch(0) + ext;
            }
        } else if (parameter.contains("turboimagehost.com/")) {
            br.getPage(parameter);
            if (br.containsHTML("(don`t exist on our server|\\- Invalid link<)")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            finallink = br.getRegex("<a href=\"http://www\\.turboimagehost\\.com\"><img src=\"(http://.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(http://s\\d+d\\d+\\.turboimagehost\\.com/sp/[a-z0-9]+/.*?)\"").getMatch(0);
            }
        } else if (parameter.contains("pimpandhost.com/")) {
            String picID = new Regex(parameter, "pimpandhost\\.com/image/show/id/(\\d+)").getMatch(0);
            if (picID == null) {
                picID = new Regex(parameter, "pimpandhost\\.com/image/(\\d+)\\-").getMatch(0);
            }
            br.getPage("http://pimpandhost.com/image/" + picID + "-original.html");
            finallink = br.getRegex("pointer;\" alt=\"\" id=\"image\" src=\"(http://.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(http://ist\\d+\\-\\d+\\.filesor\\.com/pimpandhost\\.com/.*?)\"").getMatch(0);
            }
        } else if (parameter.contains("imgchili.com/")) {
            br.getPage(parameter);
            if (br.containsHTML("does not exist\\. <")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            finallink = br.getRegex("id=\"show_image\" onload=\"scale\\(this\\);\" onclick=\"scale\\(this\\);\"[\t\n\r ]+src=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(http://i\\d+\\.imgchili\\.com/\\d+/[a-z0-9_]+\\.[a-z]{1,5})\"").getMatch(0);
            }
            finalfilename = new Regex(parameter, "imgchili\\.com/show/\\d+/(.+)").getMatch(0);
        } else if (parameter.contains("9gag.com/")) {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            if (br.getURL().contains("?post_removed=1")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            finallink = br.getRegex("ref=\"nofollow\" target=\"_blank\">[\t\n\r ]+<img src=\"(http://[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("imgur.com/a/")) {
            br.getPage(parameter.replace("/all$", ""));
            String fpName = br.getRegex("<title>(.*?) \\- Imgur").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("data-title=\"([^\"]+)").getMatch(0);
                if (fpName == null) {
                    fpName = new Regex(parameter, "/a/([A-Za-z0-9]{5,})").getMatch(0);
                }
            }
            String[] links = br.getRegex("title=\"[^\"]+\" alt=\"[^\"]+\" data\\-src=\"(https?://[^\"]+)\" data-index=\"\\d+").getColumn(0);
            if (links == null || links.length == 0) {
                links = br.getRegex("class=\"unloaded\".*?data\\-src=\"(https?://[^\"]+)\" alt=\"[^\"]*\"").getColumn(0);
            }
            if (links == null || links.length == 0) {
                logger.warning("Possible Error, please make sure link is valid within browser.");
                logger.warning("If plugin is out of date please report issue to JDownloader Developement : " + parameter);
            }
            String imgUID = null;
            if (links != null && links.length != 0) {
                for (String link : links) {
                    imgUID = new Regex(link, "https?://[^/]+/([a-zA-Z0-9]+)").getMatch(0);
                    if (imgUID == null) {
                        logger.warning("Can't find imgUID");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    DownloadLink dl = createDownloadlink("http://imgurdecrypted/download/" + imgUID);
                    dl.setProperty("imgUID", imgUID);
                    decryptedLinks.add(dl);
                }
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        } else if (parameter.matches("http://(i\\.imgur\\.com/[A-Za-z0-9]{5,}|(www\\.)?imgur\\.com/(download|gallery)/[A-Za-z0-9]{5,})")) {
            String imgUID = new Regex(parameter, "https?://i\\.imgur\\.com/([A-Za-z0-9]{5,})").getMatch(0);
            if (imgUID == null) {
                imgUID = new Regex(parameter, "https?://(www\\.)?imgur\\.com/(download|gallery)?/([A-Za-z0-9]{5,})").getMatch(2);
                if (imgUID == null) {
                    logger.warning("Can't find imgUID");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            DownloadLink dl = createDownloadlink("http://imgurdecrypted/download/" + imgUID);
            dl.setProperty("imgUID", imgUID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (parameter.contains("picsapart.com/")) {
            finallink = parameter.replace("/photo/", "/download/");
            finalfilename = new Regex(parameter, "picsapart\\.com/photo/(\\d+)").getMatch(0) + ".jpg";
        } else if (parameter.contains("postimage.org/")) {
            br.getPage(parameter);
            finallink = br.getRegex("<img src=\\'(http://[^<>\"]*?)\\'").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\\'(http://s\\d+\\.postimage\\.org/[a-z0-9]+/[^<> \"/]*?)\\'").getMatch(0);
        } else if (parameter.contains("imagetwist.com/")) {
            br.getPage(parameter);
            if (!br.containsHTML(">Continue to your  image<") && !br.containsHTML(">Show image to friends") && !br.containsHTML("class=\"btndiv\">copy</div>")) {
                logger.info("Unsupported linktype: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML(">Image Not Found<|Die von Ihnen angeforderte Datei konnte nicht gefunden werden")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            finallink = br.getRegex("\"(http://img\\d+\\.imagetwist\\.com/i/\\d+/.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<p><img src=\"(http://.*?)\"").getMatch(0);
            }
        } else if (parameter.contains("cocoimage.com/")) {
            br.getPage(parameter);
            finallink = br.getRegex("\"(http://img\\d+\\.cocoimage\\.com/showimg\\.php\\?id=\\d+[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("pic4free.org/")) {
            br.getPage(parameter);
            finallink = br.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+/download\\.php\\?id=\\d+)\"").getMatch(0);
            if (finallink == null) finallink = parameter.replace("/?v=", "/images/");
        }
        if (finallink == null) {
            logger.warning("Imagehoster-Decrypter broken for link: " + parameter);
            return null;
        }
        finallink = Encoding.htmlDecode("directhttp://" + finallink);

        final DownloadLink dl = createDownloadlink(finallink);
        dl.setUrlDownload(finallink);

        if (finalfilename != null) {
            dl.setFinalFileName(Encoding.htmlDecode(finalfilename));
        }
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

    private DownloadLink handleImageBam(Browser br, String url, boolean refresh) throws IOException {
        Browser brc = br;
        if (refresh == true) {
            brc = br.cloneBrowser();
            brc.getPage(url);
        }
        String finallink = brc.getRegex("(\\'|\")(http://\\d+\\.imagebam\\.com/download/.*?)(\\'|\")").getMatch(1);
        if (finallink == null) {
            finallink = brc.getRegex("onclick=\"scale\\(this\\);\" src=\"(http://.*?)\"").getMatch(0);
        }
        if (finallink == null) return null;
        finallink = Encoding.htmlDecode(finallink);
        DownloadLink dl = createDownloadlink("directhttp://" + finallink);
        final String finalfilename = new Regex(finallink, "/([^<>\"/]*?\\.[a-z]*?)$").getMatch(0);
        if (finalfilename != null) dl.setFinalFileName(Encoding.htmlDecode(finalfilename));
        return dl;
    }

}
