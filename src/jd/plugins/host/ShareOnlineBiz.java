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

import jd.parser.Form;
import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ShareOnlineBiz extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "share-online.biz";

    private static final String PLUGIN_NAME = HOST;

    private static final String PLUGIN_VERSION = "2.0.0.0";

    private static final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    // http://share-online.biz/?d=28TUT6W93
    // http://share-online.biz/dl/1/48DQA0U39
    // http://dl-o-1.share-online.biz/1051994
    // http://www.share-online.biz/download.php?id=D90258485
    //http://.*?share-online\\.biz/(download.php\\?id\\=[a-zA-Z0-9]{9}|\\?d\\=[a-zA-Z0-9]{9}).*
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://.*?share-online\\.biz/(download.php\\?id\\=[a-zA-Z0-9]{9}|\\?d\\=[a-zA-Z0-9]{9}).*", Pattern.CASE_INSENSITIVE);

    private static final String PATTERN_FILENAME = "<span class=\"locatedActive\">Download °</span>";

    private static final String PATTERN_FILESIZE = "</font> (° °) angefordert";

    private static final String PATTERN_DLINK = "loadfilelink.decode(\"°\"); document.";

    private static String ERROR_DOWNLOAD_NOT_FOUND = "Invalid download link";

    private static Pattern firstIFrame = Pattern.compile("<iframe src=\"(http://.*?share-online\\.biz/dl_page.php\\?file=.*?)\" style=\"border", Pattern.CASE_INSENSITIVE);

    private static Pattern dlButtonFrame = Pattern.compile("<iframe name=.download..*?src=.(dl_button.php\\?file=.*?). marginwidth\\=", Pattern.CASE_INSENSITIVE);

    private static Pattern countDown = Pattern.compile("<script language=\"Javascript\">[\n\r]+.*?\\=([0-9]+)", Pattern.CASE_INSENSITIVE);

    private static Pattern filename = Pattern.compile("<center>.*?(File Name.*?|Dateiname.*?)</center>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static Pattern dloc = Pattern.compile("src=\'(dl.php?.*?)\'", Pattern.CASE_INSENSITIVE);

    // src='dl.php?a=48DQA0U39&b=344f4df81f101a8393e73fe5c61a6fe2'
    private long ctime = 0;

    private String dlink = "";

    // <center><b>File
    // Name:</b><br>kompas.www.godlike-project.com.part6.rar<br><i>(M&ouml;glicherweise
    // gek&uuml;rzt)</i><br><b>File Size:</b> 100 MB<br><b>
    public ShareOnlineBiz() {
        super();

        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    /*
     * Funktionen
     */
    // muss aufgrund eines Bugs in DistributeData true zurÃ¼ckgeben, auch wenn
    // die Zwischenablage nicht vom Plugin verarbeitet wird
    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
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

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
            String filename = SimpleMatches.getSimpleMatch(requestInfo, PATTERN_FILENAME, 0);
            String[] sizev = SimpleMatches.getSimpleMatches(requestInfo, PATTERN_FILESIZE);
            double size = Double.parseDouble(sizev[0].trim());
            String type = sizev[1].trim().toLowerCase();
            int filesize = 0;
            if (type.equals("mb")) {
                filesize = (int) (1024 * 1024 * size);
            } else if (type.equals("kb")) {
                filesize = (int) (1024 * size);
            } else {
                filesize = (int) (size);
            }
            downloadLink.setDownloadMax(filesize);
            downloadLink.setName(filename);

            return true;
        }

        catch (Exception e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }
        return false;
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        try {

            switch (step.getStep()) {
            case PluginStep.STEP_PAGE:
                requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
                http://www.share-online.biz/download.php?id=D90258d485
                logger.info(requestInfo + "");
                if(requestInfo.getLocation()!=null){
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    step.setParameter(requestInfo.getLocation().substring(requestInfo.getLocation().lastIndexOf("=")+1));
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                dlink = JDUtilities.Base64Decode(SimpleMatches.getSimpleMatch(requestInfo, PATTERN_DLINK, 0));
             
                return step;

            case PluginStep.STEP_PENDING:
                step.setParameter(ctime * 1000);
                return step;

            case PluginStep.STEP_DOWNLOAD:

                requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(dlink), requestInfo.getCookie(), null, false);
                if(requestInfo.getLocation()!=null){
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    step.setParameter(requestInfo.getLocation().substring(requestInfo.getLocation().lastIndexOf("=")+1));
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                HTTPConnection urlConnection = requestInfo.getConnection();
                int length = urlConnection.getContentLength();
                downloadLink.setDownloadMax(length);
                String filename = getFileNameFormHeader(urlConnection);

                downloadLink.setName(filename);

                dl = new RAFDownload(this, downloadLink, urlConnection);

                dl.startDownload();

                return step;

            }
            return step;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getAGBLink() {

        return "http://share-online.biz/?page=tos";
    }

}
