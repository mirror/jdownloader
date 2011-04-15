//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagefap.com" }, urls = { "http://[\\w\\.]*?imagefap.com/image.php\\?id=.*(&pgid=.*&gid=.*&page=.*)?" }, flags = { 0 })
public class ImageFap extends PluginForHost {

    public ImageFap(PluginWrapper wrapper) {
        super(wrapper);
        // this.setStartIntervall(500l);
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(getHost(), 200);
    }

    private String DecryptLink(String code) {
        try {
            String s1 = Encoding.htmlDecode(code.substring(0, code.length() - 1));

            String t = "";
            for (int i = 0; i < s1.length(); i++) {
                // logger.info("decrypt4 " + i);
                // logger.info("decrypt5 " + ((int) (s1.charAt(i+1) - '0')));
                // logger.info("decrypt6 " +
                // (Integer.parseInt(code.substring(code.length()-1,code.length()
                // ))));
                int charcode = s1.charAt(i) - Integer.parseInt(code.substring(code.length() - 1, code.length()));
                // logger.info("decrypt7 " + charcode);
                t = t + new Character((char) charcode).toString();
                // t+=new Character((char)
                // (s1.charAt(i)-code.charAt(code.length()-1)));

            }
            // logger.info(t);
            // var s1=unescape(s.substr(0,s.length-1)); var t='';
            // for(i=0;i<s1.length;i++)t+=String.fromCharCode(s1.charCodeAt(i)-s.
            // substr(s.length-1,1));
            // return unescape(t);
            // logger.info("return of DecryptLink(): " +
            // JDUtilities.htmlDecode(t));
            return Encoding.htmlDecode(t);
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
        }
        return null;
    }

    public String getAGBLink() {
        return "http://imagefap.com/faq.php";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            br.getPage(downloadLink.getDownloadURL());
            if (br.getRedirectLocation() != null) {
                if (!br.getRedirectLocation().contains("/photo/")) br.getPage(br.getRedirectLocation());
                logger.info("Setting new downloadUrl: " + br.getRedirectLocation());
                downloadLink.setUrlDownload(br.getRedirectLocation());
                br.getPage(downloadLink.getDownloadURL());
            }
            if (br.containsHTML("(>The image you are trying to access does not exist|<title> \\(Picture 1\\) uploaded by  on ImageFap\\.com</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String picture_name = br.getRegex("<title>(.*?) in gallery").getMatch(0);
            if (picture_name == null) {
                picture_name = "";
            } else {
                picture_name = " - " + picture_name;
            }
            String galleryName = getGalleryName(downloadLink);
            String authorsName = downloadLink.getStringProperty("authorsname");
            if (authorsName == null) {
                authorsName = br.getRegex("<b><font size=\"4\" color=\"#CC0000\">(.*?)\\'s gallery</font></b>").getMatch(0);
                if (authorsName == null) {
                    authorsName = br.getRegex("<td class=\"mnu0\"><a href=\"/profile\\.php\\?user=(.*?)\"").getMatch(0);
                    if (authorsName == null) {
                        authorsName = br.getRegex("jQuery\\.BlockWidget\\(\\d+,\"(.*?)\",\"left\"\\);").getMatch(0);
                    }
                }
            }
            String orderid = downloadLink.getStringProperty("orderid");
            if (authorsName == null || galleryName == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            galleryName = galleryName.trim();
            authorsName = authorsName.trim();
            if (orderid != null) {
                downloadLink.setFinalFileName(authorsName + " - " + galleryName + " - " + orderid + picture_name);
            } else {
                downloadLink.setFinalFileName(authorsName + " - " + galleryName + picture_name);
            }
            /* only set filepackage if not set yet */
            try {
                if (downloadLink.isDefaultFilePackage()) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(authorsName + " - " + galleryName);
                    downloadLink.setFilePackage(fp);
                }
            } catch (Throwable e) {
                /*
                 * does not work in stable 0.9580, can be removed with next
                 * major update
                 */
                try {
                    if (downloadLink.getFilePackage() == FilePackage.getDefaultFilePackage()) {
                        FilePackage fp = FilePackage.getInstance();
                        fp.setName(authorsName + " - " + galleryName);
                        downloadLink.setFilePackage(fp);
                    }
                } catch (Throwable e2) {
                }
            }
            return AvailableStatus.TRUE;
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String pfilename = downloadLink.getName();
        br.getPage(downloadLink.getDownloadURL());
        br.setDebug(true);
        String gallery_name = getGalleryName(downloadLink);
        String imagelink = br.getRegex("\"(http://fap\\.to/images/full/\\d+/\\d+/.*?)\"").getMatch(0);
        if (imagelink == null) {
            String returnID = new Regex(br, Pattern.compile("return lD\\(\\'(\\S+?)\\'\\);", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (returnID != null) imagelink = DecryptLink(returnID);
            if (imagelink == null) {
                imagelink = br.getRegex("onclick=\"OnPhotoClick\\(\\);\" src=\"(http://.*?)\"").getMatch(0);
                if (imagelink == null) imagelink = br.getRegex("href=\"#\" onclick=\"javascript:window\\.open\\(\\'(http://.*?)\\'\\)").getMatch(0);
            }
        }
        if (imagelink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Only set subdirectory if it wasn't set before or we'll get subfolders
        // in subfolders which is bad
        if (gallery_name != null && downloadLink.getSubdirectory() == null) downloadLink.addSubdirectory(gallery_name);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, imagelink);
        if (dl.getConnection().getResponseCode() == 404) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!pfilename.endsWith(new Regex(imagelink, "(\\.[A-Za-z0-9]+)$").getMatch(0))) pfilename += new Regex(imagelink, "(\\.[A-Za-z0-9]+)$").getMatch(0);
        downloadLink.setFinalFileName(pfilename);
        dl.startDownload();
    }

    private String getGalleryName(DownloadLink dl) {
        String galleryName = dl.getStringProperty("galleryname");
        if (galleryName == null) {
            galleryName = br.getRegex("<title>Porn pics of (.*?) \\(Page 1\\)</title>").getMatch(0);
            if (galleryName == null) {
                galleryName = br.getRegex("<font face=\"verdana\" color=\"white\" size=\"4\"><b>(.*?)</b></font>").getMatch(0);
                if (galleryName == null) galleryName = br.getRegex("<td bgcolor=\\'#FCFFE0\\'><a href=\"/gallery\\.php\\?gid=\\d+\">(.*?)</a></td>").getMatch(0);
            }
        }
        return galleryName;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
