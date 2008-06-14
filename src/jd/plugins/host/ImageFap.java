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

import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ImageFap extends PluginForHost {
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?imagefap.com/image.php\\?id=.*(&pgid=.*&gid=.*&page=.*)?", Pattern.CASE_INSENSITIVE);

    static private final String HOST = "imagefap.com";
    static private final String PLUGIN_NAME = HOST;
    static private final String PLUGIN_VERSION = "0.3";
    static private final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    static private final String CODER = "JD-Team";
    static private final Pattern IMAGELINK = Pattern.compile("return lD\\('(\\S+?)'\\);", Pattern.CASE_INSENSITIVE);
    // <a href= "gallery .php ?pgid=314aad4251046f "> <font
    // color="white">Private - Amateur Interracial Orgy 2</font></a>
    static private final String GALLERY = "<a href=\"gallery.php°gid=°\"><font color=\"white\">°</font></a>";
    static private final Pattern FILENAME = Pattern.compile("\\<td bgcolor='#FCFFE0' width=\"100\"\\>Filename\\<\\/td\\>[\\s\\S]*?\\<td bgcolor='#FCFFE0'\\>(.*?\\.jpg)\\<\\/td\\>", Pattern.CASE_INSENSITIVE);

    public ImageFap() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public String getCoder() {
        return CODER;
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
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
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
                // (Integer.parseInt(code.substring(code.length()-1,code.length()))));
                int charcode = ((int) (s1.charAt(i))) - (Integer.parseInt(code.substring(code.length() - 1, code.length())));
                // logger.info("decrypt7 " + charcode);
                t = t + new Character((char) charcode).toString();
                // t+=new Character((char)
                // (s1.charAt(i)-code.charAt(code.length()-1)));

            }
            // logger.info(t);
            // var s1=unescape(s.substr(0,s.length-1)); var t='';
            // for(i=0;i<s1.length;i++)t+=String.fromCharCode(s1.charCodeAt(i)-s.substr(s.length-1,1));
            // return unescape(t);
            logger.info("return of DecryptLink(): " + JDUtilities.htmlDecode(t));
            return JDUtilities.htmlDecode(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        RequestInfo requestInfo;
        try {
            if (step.getStep() == PluginStep.STEP_DOWNLOAD) {

                requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
                if (requestInfo.getHtmlCode() == null||requestInfo.getHtmlCode().trim().length()==0) {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    step.setParameter(JDLocale.L("plugins.host.imagefap.imageerror", "Bildfehler"));
                    return step;
                }
                String Imagelink = DecryptLink(SimpleMatches.getFirstMatch(requestInfo.getHtmlCode(), IMAGELINK, 1));
                String gallery = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), GALLERY, 2).trim();
                String Imagename = SimpleMatches.getFirstMatch(requestInfo.getHtmlCode(), FILENAME, 1).trim();

                if (Imagename == null || gallery == null) {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    step.setParameter(JDLocale.L("plugins.host.imagefap.imageerror", "Bildfehler"));
                    return step;

                }
                File file = new File(downloadLink.getFilePackage().getDownloadDirectory(), JDUtilities.validatePath(gallery));

                requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(Imagelink), requestInfo.getCookie(), null, null, false);
                if (requestInfo.getLocation() != null) {
                    requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(requestInfo.getLocation()), requestInfo.getCookie(), null, false);
                }

                downloadLink.setName(JDUtilities.validatePath(gallery + " - " + Imagename));
            
                downloadLink.setDownloadPath(file.getAbsolutePath());

                dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());
                dl.startDownload();
             

               // step.setStatus(PluginStep.STATUS_DONE);
               // downloadLink.setStatus(DownloadLink.STATUS_DONE);
                return step;

            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
        // this.url = null;
    }

    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        RequestInfo requestInfo;
        try {
            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
            String name = SimpleMatches.getFirstMatch(requestInfo.getHtmlCode(), FILENAME, 1);
            String gallery = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), GALLERY, 2);
            downloadLink.setName(gallery + "/" + gallery + " - " + name);
            
            if(name==null||gallery==null)return false;
            
            /*
             * 
             * Vector<String> link = matches.get(id);
             * downloadLink.setName(link.get(1)); if (link != null) { try { int
             * length = (int) (Double.parseDouble(link.get(2)) * 1024 * 1024);
             * downloadLink.setDownloadMax(length); } catch (Exception e) { } }
             */
            return true;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return true;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 50;
    }

    @Override
    public void resetPluginGlobals() {

    }

    @Override
    public String getAGBLink() {
        return "http://imagefap.com/faq.php";
    }
}
