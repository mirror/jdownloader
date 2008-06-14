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
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class BadOngo extends PluginForHost {
    private static final String  HOST          = "badongo.com";

    private static final String  VERSION       = "1.0.0.0";

    private static final Pattern PAT_SUPPORTED = Pattern.compile("http://.*?badongo\\.com/[a-zA-Z/]{0,5}(vid|file)/[\\d]{4,10}", Pattern.CASE_INSENSITIVE);

    private Form                 form;

    private File                 captchaFile;

    private String               cookie        = null;

    private long                 rsrnd         = 0;

    private String               code          = null;

    //
    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    public BadOngo() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) throws Exception {
        switch (step.getStep()) {
            case PluginStep.STEP_WAIT_TIME:
                if (!downloadLink.isAvailabilityChecked()) getFileInformation(downloadLink);
                if (!downloadLink.isAvailable()) {
                    step.setStatus(PluginStep.STATUS_ERROR);
                    logger.severe("konnte den Download nicht finden");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                    return null;
                }
                requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL().replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}file", "http://www.badongo.com/de/file").replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}vid", "http://www.badongo.com/de/vid")));
                String[] linksArray = requestInfo.getRegexp("Teil #[\\d]+:.nbsp.<a href=\"(.*?)\">").getMatches(1);
                if (linksArray != null && linksArray.length > 0) {
                    for (int i = 1; i < linksArray.length; i++) {
                        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
                        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
                        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
                        downloadLink.saveObjects.add(linksArray[i]);
                    }
                    downloadLink.setUrlDownload(linksArray[0]);
                }
                else if (downloadLink.saveObjects.size() > 0) {
                    downloadLink.setUrlDownload((String) downloadLink.saveObjects.getFirst());
                    downloadLink.saveObjects.removeFirst();
                }
                if (cookie == null) {
                    cookie = requestInfo.getCookie();
                    rsrnd = System.currentTimeMillis();
                    requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL().replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}file", "http://www.badongo.com/en/file").replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}vid", "http://www.badongo.com/en/vid") + "?rs=refreshImage&rst=&rsrnd=" + rsrnd + "&rsargs[]=0"), cookie, requestInfo.getConnection().getURL().toString(), true);
                    form = requestInfo.getForm();
                    form.action = form.action.replaceAll("\\\\\"", "");
                    captchaFile = getLocalCaptchaFile(this, ".jpg");
                    String captchaAdress = "http://www.badongo.com" + requestInfo.getRegexp("<img src=\\\\\"(.*?)\\\\\" />").getFirstMatch();
                    logger.info("CaptchaAdress:" + captchaAdress);
                    boolean fileDownloaded = JDUtilities.download(captchaFile, HTTP.getRequestWithoutHtmlCode(new URL(captchaAdress), requestInfo.getCookie(), null, true).getConnection());
                    if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {
                        logger.severe("Captcha not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.saveObjects.addFirst(downloadLink.getDownloadURL());
                        return step;
                    }
                    try {
                        code = Plugin.getCaptchaCode(captchaFile, this);
                    }
                    catch (Exception e) {

                    }
                    if (code == null || code == "") {
                        logger.severe("Captcha Wrong");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                        downloadLink.saveObjects.addFirst(downloadLink.getDownloadURL());
                        step.setStatus(PluginStep.STATUS_ERROR);
                        JDUtilities.appendInfoToFilename(this, captchaFile, "_NULL", false);
                        cookie = null;
                        return step;
                    }
                    form.put("user_code", code);
                    cookie += "; " + requestInfo.getCookie();
                    requestInfo = form.getRequestInfo(false);
                    cookie += "; " + requestInfo.getCookie();
                    if (!requestInfo.toString().contains("Premium<br>")) {
                        logger.severe("Captcha Wrong");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                        downloadLink.saveObjects.addFirst(downloadLink.getDownloadURL());
                        step.setStatus(PluginStep.STATUS_ERROR);
                        JDUtilities.appendInfoToFilename(this, captchaFile, "_" + code, false);
                        cookie = null;
                        return step;
                    }
                    return step;
                }
                else {
                    requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL().replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}file", "http://www.badongo.com/de/cfile").replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}vid", "http://www.badongo.com/de/cfile")), cookie, requestInfo.getConnection().getURL().toString(), false);
                    cookie += "; " + requestInfo.getCookie();
                }
                return step;
            case PluginStep.STEP_PENDING:
                step.setParameter((long) 15000);
                return step;
            case PluginStep.STEP_DOWNLOAD:
//                if (aborted) {
//                    logger.warning("Plugin abgebrochen");
//                    downloadLink.setStatus(DownloadLink.STATUS_TODO);
//                    step.setStatus(PluginStep.STATUS_TODO);
//                    return step;
//                }
                form = null;
                for (int i = 0; i < 10; i++) {
                    try {
                        requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL().replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}(vid|file)", "http://www.badongo.com/de/cfile") + "?rs=getFileLink&rst=&rsrnd=" + rsrnd + "&rsargs[]=0"), cookie, requestInfo.getConnection().getURL().toString(), true);
                        form = requestInfo.getForm();
                    }
                    catch (Exception e) {
                        // TODO: handle exception
                    }

                    if (form != null)
                        break;
                    else {
                        try {
                            Thread.sleep(500);
                        }
                        catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                if (form == null) {
                    logger.warning("Download gerade nicht verfügbar");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                    downloadLink.saveObjects.addFirst(downloadLink.getDownloadURL());
                    step.setStatus(PluginStep.STATUS_ERROR);
                }
                form.action = form.action.replaceAll("\\\\\"", "");
                form.put("Datei runterladen", "");
                form.setRequestPopertie("Cookie", cookie);
                logger.info(form.toString());
                HTTPConnection urlConnection = form.getConnection();
                int c = 1;
                if (downloadLink.saveObjects.size() > 0) downloadLink.setName(downloadLink.getName().replaceFirst("\\.[\\d]+$", "") + "." + c++);
                File fileOutput = new File(downloadLink.getFileOutput());

                while (fileOutput.exists()) {
                    downloadLink.setName(downloadLink.getName().replaceFirst("\\.[\\d]+$", "") + "." + c++);
                    fileOutput = new File(downloadLink.getFileOutput());
                }
                downloadLink.setDownloadMax(urlConnection.getContentLength());
            
               dl = new RAFDownload(this, downloadLink, urlConnection);

                if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {

                    logger.severe("captcha wrong");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                    downloadLink.saveObjects.addFirst(downloadLink.getDownloadURL());
                    JDUtilities.appendInfoToFilename(this, captchaFile, "_" + code, false);
                    step.setStatus(PluginStep.STATUS_ERROR);
                }
                return step;
        }
        step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        return step;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL().replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}file", "http://www.badongo.com/de/file").replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}vid", "http://www.badongo.com/de/vid")));
            String[] fileInfo = new Regex(requestInfo.getHtmlCode(), "<td valign=\"top\"><b>Datei.</b></td>.*?<td>.nbsp.(.*?)</td>.*?<td valign=\"top\"><b>Dateigr.sse.</b></td>.*?<td>.nbsp.([0-9\\.]*)(.*?)</td>").getMatches()[0];
            // Wurden DownloadInfos gefunden? --> Datei ist vorhanden/online
            downloadLink.setName(fileInfo[0]);
            try {
                double length = Double.parseDouble(fileInfo[1].trim());
                int bytes;
                String type = fileInfo[2].toLowerCase();
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
            // Datei ist noch verfuegbar
            return true;
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        // Datei scheinbar nicht mehr verfuegbar, Fehler?
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    // TODO Automatisch erstellter Methoden-Stub
    }

    @Override
    public void resetPluginGlobals() {
    // TODO Automatisch erstellter Methoden-Stub
    }

    @Override
    public String getAGBLink() {

        return "http://www.badongo.com/toc";
    }
}
