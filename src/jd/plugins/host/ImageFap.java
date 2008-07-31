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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class ImageFap extends PluginForHost {
    // static private final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "0.3";
    // static private final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    static private final String CODER = "JD-Team";

    static private final String HOST = "imagefap.com";
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?imagefap.com/image.php\\?id=.*(&pgid=.*&gid=.*&page=.*)?", Pattern.CASE_INSENSITIVE);
    private String gallery_name;
    private String picture_name;
    private RequestInfo requestInfo;
    private String uploader_name;

    public ImageFap() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
    }

    private String DecryptLink(String code) { // similar to lD() @
        // imagefap.com
        try {
            String s1 = JDUtilities.htmlDecode(code.substring(0, code.length() - 1));

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
            return JDUtilities.htmlDecode(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
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
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
            picture_name = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<td bgcolor='#FCFFE0' width=\"100\">Filename</td>.*?<td bgcolor='#FCFFE0'>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getFirstMatch();
            gallery_name = new Regex(requestInfo.getHtmlCode(), Pattern.compile("size=4>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            if (gallery_name != null) {
                gallery_name = gallery_name.trim();
            }
            uploader_name = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"/profile\\.php\\?user=(.*?)\" style=\"text-decoration: none;\"", Pattern.CASE_INSENSITIVE)).getFirstMatch();

            if (picture_name != null) {
                FilePackage fp = new FilePackage();
                fp.setName(uploader_name);
                downloadLink.setName(gallery_name + File.separator + picture_name);
                downloadLink.setFilePackage(fp);
                return true;
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        downloadLink.setAvailable(false);
        return false;
    }

    private boolean getFileInformation2(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
            picture_name = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<td bgcolor='#FCFFE0' width=\"100\">Filename</td>.*?<td bgcolor='#FCFFE0'>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getFirstMatch();
            gallery_name = new Regex(requestInfo.getHtmlCode(), Pattern.compile("size=4>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            if (gallery_name != null) {
                gallery_name = gallery_name.trim();
            }
            uploader_name = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"/profile\\.php\\?user=(.*?)\" style=\"text-decoration: none;\"", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            if (picture_name != null) { return true; }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        downloadLink.setAvailable(false);
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 50;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation2(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        /* DownloadLink holen */
        String Imagelink = DecryptLink(new Regex(requestInfo.getHtmlCode(), Pattern.compile("return lD\\('(\\S+?)'\\);", Pattern.CASE_INSENSITIVE)).getFirstMatch());
        requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(Imagelink), requestInfo.getCookie(), null, null, false);
        if (requestInfo.getLocation() != null) {
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(requestInfo.getLocation()), requestInfo.getCookie(), null, false);
        }
        /* Downloaden */
        String filename = getFileNameFormHeader(requestInfo.getConnection()).replaceAll("getimg\\.php\\?img=", "");
        downloadLink.setName(gallery_name + File.separator + filename);
        dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());
        dl.setResume(false);
        dl.setChunkNum(1);
        dl.startDownload();
        // if (!dl.startDownload() && step.getStatus() !=
        // PluginStep.STATUS_ERROR && step.getStatus() !=
        // PluginStep.STATUS_TODO) {
        // linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // return;
        // }
        return;

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }
}
