//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.host;

import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class ImageFap extends PluginForHost {

    static private final String CODER = "JD-Team";

    public ImageFap() {
        super();
    }

    private String DecryptLink(String code) {
        try {
            String s1 = Encoding.htmlDecode(code.substring(0, code.length() - 1));

            String t = "";
            for (int i = 0; i < s1.length(); i++) {
                // logger.info("decrypt4 " + i);
                // logger.info("decrypt5 " + ((int) (s1.charAt(i+1) - '0')));
                // logger.info("decrypt6 " +
                //(Integer.parseInt(code.substring(code.length()-1,code.length()
                // ))));
                int charcode = s1.charAt(i) - Integer.parseInt(code.substring(code.length() - 1, code.length()));
                // logger.info("decrypt7 " + charcode);
                t = t + new Character((char) charcode).toString();
                // t+=new Character((char)
                // (s1.charAt(i)-code.charAt(code.length()-1)));

            }
            // logger.info(t);
            // var s1=unescape(s.substr(0,s.length-1)); var t='';
            //for(i=0;i<s1.length;i++)t+=String.fromCharCode(s1.charCodeAt(i)-s.
            // substr(s.length-1,1));
            // return unescape(t);
            // logger.info("return of DecryptLink(): " +
            // JDUtilities.htmlDecode(t));
            return Encoding.htmlDecode(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getAGBLink() {
        return "http://imagefap.com/faq.php";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            Browser br = new Browser();
            br.getPage(downloadLink.getDownloadURL());
            String picture_name = new Regex(br, Pattern.compile("<td bgcolor='#FCFFE0' width=\"100\">Filename</td>.*?<td bgcolor='#FCFFE0'>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            String gallery_name = new Regex(br, Pattern.compile("size=4>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(0);
            String uploader_name = new Regex(br, Pattern.compile("<a href=\"/profile\\.php\\?user=(.*?)\" style=\"text-decoration: none;\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (gallery_name != null) {
                gallery_name = gallery_name.trim();
            }
            if (picture_name != null) {
                FilePackage fp = new FilePackage();
                fp.setName(uploader_name);
                downloadLink.setName(gallery_name + " + " + picture_name);
                downloadLink.setFilePackage(fp);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        Browser br = new Browser();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String picture_name = new Regex(br, Pattern.compile("<td bgcolor='#FCFFE0' width=\"100\">Filename</td>.*?<td bgcolor='#FCFFE0'>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (picture_name == null) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        String gallery_name = new Regex(br, Pattern.compile("size=4>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (gallery_name != null) {
            gallery_name = gallery_name.trim();
        }

        /* DownloadLink holen */
        String imagelink = DecryptLink(new Regex(br, Pattern.compile("return lD\\('(\\S+?)'\\);", Pattern.CASE_INSENSITIVE)).getMatch(0));
        HTTPConnection con = br.openGetConnection(imagelink);
        String filename = Plugin.extractFileNameFromURL(imagelink);
        downloadLink.setStaticFileName(filename);
        downloadLink.addSubdirectory(gallery_name);
        dl = new RAFDownload(this, downloadLink, con);
        dl.setResume(false);
        dl.setChunkNum(1);
        dl.startDownload();
        return;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 15;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
