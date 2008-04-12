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

import jd.plugins.DownloadLink;
import jd.plugins.Form;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ShareOnlineBiz extends PluginForHost {
    private static final String  CODER                    = "JD-Team";

    private static final String  HOST                     = "share-online.biz";

    private static final String  PLUGIN_NAME              = HOST;

    private static final String  PLUGIN_VERSION           = "2.0.0.0";

    private static final String  PLUGIN_ID                = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    // http://share-online.biz/?d=28TUT6W93
    // http://share-online.biz/dl/1/48DQA0U39
    // http://dl-o-1.share-online.biz/1051994
    static private final Pattern PAT_SUPPORTED            = Pattern.compile("http://.*?share-online\\.biz/(dl/[\\d]/[a-zA-Z0-9]{9}|[\\d]{5,9}|\\?d\\=[a-zA-Z0-9]{9}).*", Pattern.CASE_INSENSITIVE);

    private static String        ERROR_DOWNLOAD_NOT_FOUND = "Invalid download link";

    private static Pattern       firstIFrame              = Pattern.compile("<iframe src=\"(http://.*?share-online\\.biz/dl_page.php\\?file=.*?)\" style=\"border", Pattern.CASE_INSENSITIVE);

    private static Pattern       dlButtonFrame            = Pattern.compile("<iframe name=.download..*?src=.(dl_button.php\\?file=.*?). marginwidth\\=", Pattern.CASE_INSENSITIVE);

    private static Pattern       countDown                = Pattern.compile("<script language=\"Javascript\">[\n\r]+.*?\\=([0-9]+)", Pattern.CASE_INSENSITIVE);

    private static Pattern       filename                 = Pattern.compile("<center>.*?(File Name.*?|Dateiname.*?)</center>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static Pattern       dloc                     = Pattern.compile("src=\'(dl.php?.*?)\'", Pattern.CASE_INSENSITIVE);

    // src='dl.php?a=48DQA0U39&b=344f4df81f101a8393e73fe5c61a6fe2'
    private long                 ctime                    = 0;

    private String               dlink                    = "";

    private URL                  url;

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
    public void reset() {}

    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            requestInfo = getRequest(new URL(downloadLink.getDownloadURL()));

            if (downloadLink.getDownloadURL().matches("http://.*?share-online\\.biz/dl/[\\d]/[a-zA-Z0-9]{9}")) requestInfo = getRequest(new URL(getFirstMatch(requestInfo.getHtmlCode(), firstIFrame, 1)));

            if (requestInfo.containsHTML(ERROR_DOWNLOAD_NOT_FOUND) || requestInfo.getHtmlCode().equals("")) {
                logger.severe("download not found");
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                return false;
            }
            if (requestInfo.getHtmlCode().contains("<input type=text class=textinput name=downloadpw")) {
                String password = "&downloadpw=" + JDUtilities.getController().getUiInterface().showUserInputDialog("Please enter the password!");
                downloadLink.setUrlDownload(downloadLink.getDownloadURL() + password);
            }
            String[] names = getFirstMatch(requestInfo.getHtmlCode(), filename, 1).split("<.*?>");

            if (names.length > 2) {
                int c = 1;
                for (int i = 1; i < names.length; i++) {
                    if (!names[i].matches("[\\s]*")) {
                        downloadLink.setName(names[i]);
                        c = i + 1;
                        break;
                    }
                }
                for (int i = c; i < names.length; i++) {
                    String string = names[i];
                    if (string.matches(".*(File Size|Dateigr).*")) {
                        c = i + 1;
                        break;
                    }
                }
                for (int i = c; i < names.length; i++) {
                    if (!names[i].matches("[\\s]*")) {
                        String[] fileSize = names[i].substring(1, names[i].length()).split(" ");
                        if (fileSize.length > 1 && fileSize[0].matches("\\d+")) {
                            try {
                                double length = Double.parseDouble(fileSize[0].trim());
                                int bytes;
                                String type = fileSize[1].toLowerCase();
                                if (type.equalsIgnoreCase("kb")) {
                                    bytes = (int) (length * 1024);
                                }
                                else if (type.equalsIgnoreCase("mb")) {
                                    bytes = (int) (length * 1024 * 1024);
                                }
                                else {
                                    bytes = (int) length;
                                }
                                downloadLink.setDownloadMax(bytes);
                            }
                            catch (Exception e) {
                            }
                        }
                        break;
                    }

                }
            }

            return true;
        }
        catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        try {

            switch (step.getStep()) {
                case PluginStep.STEP_PAGE:
                    String link = downloadLink.getDownloadURL();
                    String password = new Regexp(link, ".downloadpw\\=(.*)").getFirstMatch();
                    if (password != null) link = link.replaceFirst(".downloadpw.*", "");
                    logger.info(password);
                    logger.info(link);
                    url = new URL(link);
                    requestInfo = getRequest(url);
                    if (link.matches("http://.*?share-online\\.biz/dl/[\\d]/[a-zA-Z0-9]{9}.*")) {
                        url = new URL(getFirstMatch(requestInfo.getHtmlCode(), firstIFrame, 1));
                        requestInfo = getRequest(url);
                        if (requestInfo.containsHTML(ERROR_DOWNLOAD_NOT_FOUND)) {
                            logger.severe("download not found");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        url = new URL("http://" + url.getHost() + "/" + getFirstMatch(requestInfo.getHtmlCode(), dlButtonFrame, 1));

                        requestInfo = getRequest(url);
                    }
                    else if (requestInfo.containsHTML(ERROR_DOWNLOAD_NOT_FOUND)) {

                        logger.severe("download not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;

                    }
                    downloadLink.setStatusText(JDLocale.L("plugins.hoster.shareonlinebiz.captcha", "Captcha Erkennung"));
                    if (link.matches("http://.*?share-online\\.biz/\\?d\\=[a-zA-Z0-9]{9}.*")) {
                        while (requestInfo.getHtmlCode().contains("<input type=text class=textinput name=captchacode value=")) {
                            Form[] forms = Form.getForms(requestInfo);
                            if (forms == null || forms.length == 0 || forms[0] == null) {
                                step.setStatus(PluginStep.STATUS_ERROR);
                                logger.severe("konnte den Download nicht finden");
                                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                                return null;
                            }
                            Form form = forms[0];
                            File captchaFile = getLocalCaptchaFile(this, ".png");
                            String captchaAdress = "http://share-online.biz/captcha.php";
                            boolean fileDownloaded = JDUtilities.download(captchaFile, getRequestWithoutHtmlCode(new URL(captchaAdress), requestInfo.getCookie(), link, true).getConnection());
                            if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {
                                logger.severe("Captcha not found");
                                downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                                step.setStatus(PluginStep.STATUS_ERROR);
                                return step;
                            }
                            String captchaCode=Plugin.getCaptchaCode(captchaFile, this);
                            if(captchaCode==null){
                                logger.severe("Captcha not detected");
                                downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                                step.setStatus(PluginStep.STATUS_ERROR);
                                return step;                                 
                                
                            }
                            form.put("captchacode", captchaCode);
                            if (requestInfo.getHtmlCode().contains("downloadpw")) {
                                if (password != null)
                                    form.put("downloadpw", password);
                                else {
                                    password = JDUtilities.getController().getUiInterface().showUserInputDialog("Please enter the password!");
                                    form.put("downloadpw", password);
                                }

                            }
                            requestInfo = form.getRequestInfo();
                            logger.info(form.toString());
                        }
                        ctime = Integer.parseInt(new Regexp(requestInfo.getHtmlCode(), "var timeout=\'([\\d]+)\';").getFirstMatch());

                    }
                    else {
                        ctime = Integer.parseInt(getFirstMatch(requestInfo.getHtmlCode(), countDown, 1));
                    }

                    /*
                     * dlink = getFirstMatch(requestInfo.getHtmlCode(),
                     * downloadl, 1); if(!dlink.matches("http://.*")) dlink =
                     * "http://" + url.getHost() + "/"+dlink;
                     */
                    dlink = getBetween(requestInfo.getHtmlCode(), "document.location=\"", "\"");
                    // logger.info(dlink);

                    return step;

                case PluginStep.STEP_PENDING:
                    step.setParameter(ctime * 1000);
                    return step;

                case PluginStep.STEP_DOWNLOAD:
                    url = new URL(dlink);

                    if (!downloadLink.getDownloadURL().matches("http://.*?share-online\\.biz/\\?d\\=[a-zA-Z0-9]{9}.*")) {
                        requestInfo = getRequest(url);
                        dlink = "http://" + url.getHost() + "/" + getFirstMatch(requestInfo.getHtmlCode(), dloc, 1);
                    }
                    requestInfo = getRequestWithoutHtmlCode(new URL(dlink), null, null, false);
                    HTTPConnection urlConnection = requestInfo.getConnection();
                    int length = urlConnection.getContentLength();
                    downloadLink.setDownloadMax(length);
                    String filename = getFileNameFormHeader(urlConnection);
                    if (filename.startsWith("[share-online.biz]")) filename = filename.substring(18);
                    downloadLink.setName(filename);
                    if (!hasEnoughHDSpace(downloadLink)) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                   dl = new RAFDownload(this, downloadLink, urlConnection);

                    dl.startDownload();

                    return step;

            }
            return step;
        }
        catch (IOException e) {
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
