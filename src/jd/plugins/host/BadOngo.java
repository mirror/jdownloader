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
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class BadOngo extends PluginForHost {
    private static final String HOST = "badongo.com";

    private static final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?badongo\\.com/[a-zA-Z/]{0,5}(vid|file)/[\\d]{4,10}", Pattern.CASE_INSENSITIVE);

    private File captchaFile;

    private String code = null;

    private String cookie = null;

    private Form form;

    private long rsrnd = 0;

    //

    public BadOngo() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        // steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

    @Override
    public String getAGBLink() {

        return "http://www.badongo.com/toc";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
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
                } else if (type.equalsIgnoreCase("mb")) {
                    bytes = (int) (length * 1024 * 1024);
                } else {
                    bytes = (int) length;
                }
                downloadLink.setDownloadMax(bytes);
            } catch (Exception e) {
            }
            // Datei ist noch verfuegbar
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Datei scheinbar nicht mehr verfuegbar, Fehler?
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
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

        // switch (step.getStep()) {
        // //case PluginStep.STEP_WAIT_TIME:
        if (!downloadLink.isAvailabilityChecked()) {
            getFileInformation(downloadLink);
        }
        if (!downloadLink.isAvailable()) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("konnte den Download nicht finden");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL().replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}file", "http://www.badongo.com/de/file").replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}vid", "http://www.badongo.com/de/vid")));
        String[] linksArray = requestInfo.getRegexp("Teil #[\\d]+:.nbsp.<a href=\"(.*?)\">").getMatches(1);
        if (linksArray != null && linksArray.length > 0) {
            for (int i = 1; i < linksArray.length; i++) {
                // steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
                // steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
                // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
                downloadLink.saveObjects.add(linksArray[i]);
            }
            downloadLink.setUrlDownload(linksArray[0]);
        } else if (downloadLink.saveObjects.size() > 0) {
            downloadLink.setUrlDownload((String) downloadLink.saveObjects.getFirst());
            downloadLink.saveObjects.removeFirst();
        }
        if (cookie == null) {
            cookie = requestInfo.getCookie();
            rsrnd = System.currentTimeMillis();
            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL().replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}file", "http://www.badongo.com/en/file").replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}vid", "http://www.badongo.com/en/vid") + "?rs=refreshImage&rst=&rsrnd=" + rsrnd + "&rsargs[]=0"), cookie, requestInfo.getConnection().getURL().toString(), true);
            form = requestInfo.getForm();
            form.action = form.action.replaceAll("\\\\\"", "");
            captchaFile = Plugin.getLocalCaptchaFile(this, ".jpg");
            String captchaAdress = "http://www.badongo.com" + requestInfo.getRegexp("<img src=\\\\\"(.*?)\\\\\" />").getFirstMatch();
            logger.info("CaptchaAdress:" + captchaAdress);
            boolean fileDownloaded = JDUtilities.download(captchaFile, HTTP.getRequestWithoutHtmlCode(new URL(captchaAdress), requestInfo.getCookie(), null, true).getConnection());
            if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {
                logger.severe("Captcha not found");
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);// step.setParameter("Captcha
                // ImageIO
                // Error");
                // step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.saveObjects.addFirst(downloadLink.getDownloadURL());
                return;
            }
            try {
                code = Plugin.getCaptchaCode(captchaFile, this);
            } catch (Exception e) {

            }
            if (code == null || code == "") {
                logger.severe("Captcha Wrong");
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                downloadLink.saveObjects.addFirst(downloadLink.getDownloadURL());
                // step.setStatus(PluginStep.STATUS_ERROR);
                JDUtilities.appendInfoToFilename(this, captchaFile, "_NULL", false);
                cookie = null;
                return;
            }
            form.put("user_code", code);
            cookie += "; " + requestInfo.getCookie();
            requestInfo = form.getRequestInfo(false);
            cookie += "; " + requestInfo.getCookie();
            if (!requestInfo.toString().contains("Premium<br>")) {
                logger.severe("Captcha Wrong");
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                downloadLink.saveObjects.addFirst(downloadLink.getDownloadURL());
                // step.setStatus(PluginStep.STATUS_ERROR);
                JDUtilities.appendInfoToFilename(this, captchaFile, "_" + code, false);
                cookie = null;
                return;
            }
            return;
        } else {
            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL().replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}file", "http://www.badongo.com/de/cfile").replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}vid", "http://www.badongo.com/de/cfile")), cookie, requestInfo.getConnection().getURL().toString(), false);
            cookie += "; " + requestInfo.getCookie();
        }
        // return;
        // case PluginStep.STEP_PENDING:
        // step.setParameter((long) 15000);
        // return;
        // case PluginStep.STEP_DOWNLOAD:
        // if (aborted) {
        // logger.warning("Plugin abgebrochen");
        // linkStatus.addStatus(LinkStatus.TODO);
        // //step.setStatus(PluginStep.STATUS_TODO);
        // return;
        // }
        form = null;
        for (int i = 0; i < 10; i++) {
            try {
                requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL().replaceFirst("http://.*?badongo\\.com/[a-zA-Z/]{0,5}(vid|file)", "http://www.badongo.com/de/cfile") + "?rs=getFileLink&rst=&rsrnd=" + rsrnd + "&rsargs[]=0"), cookie, requestInfo.getConnection().getURL().toString(), true);
                form = requestInfo.getForm();
            } catch (Exception e) {
                // TODO: handle exception
            }

            if (form != null) {
                break;
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
            }
        }
        if (form == null) {
            logger.warning("Download gerade nicht verfügbar");
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            downloadLink.saveObjects.addFirst(downloadLink.getDownloadURL());
            // step.setStatus(PluginStep.STATUS_ERROR);
        }
        form.action = form.action.replaceAll("\\\\\"", "");
        form.put("Datei runterladen", "");
        form.setRequestPoperty("Cookie", cookie);
        logger.info(form.toString());
        HTTPConnection urlConnection = form.getConnection();
        int c = 1;
        if (downloadLink.saveObjects.size() > 0) {
            downloadLink.setName(downloadLink.getName().replaceFirst("\\.[\\d]+$", "") + "." + c++);
        }
        File fileOutput = new File(downloadLink.getFileOutput());

        while (fileOutput.exists()) {
            downloadLink.setName(downloadLink.getName().replaceFirst("\\.[\\d]+$", "") + "." + c++);
            fileOutput = new File(downloadLink.getFileOutput());
        }

        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.startDownload();
        // dl.startDownload(); \r\n if (!dl.startDownload() && step.getStatus()
        // != PluginStep.STATUS_ERROR && step.getStatus() !=
        // PluginStep.STATUS_TODO) {
        //
        // logger.severe("captcha wrong");
        // linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA_WRONG);
        // downloadLink.saveObjects.addFirst(downloadLink.getDownloadURL());
        // JDUtilities.appendInfoToFilename(this, captchaFile, "_" + code,
        // false);
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // }
        return;

        // step.setStatus(PluginStep.STATUS_ERROR);
        // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        // return;

    }

    @Override
    public void reset() {
        // TODO Automatisch erstellter Methoden-Stub
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Automatisch erstellter Methoden-Stub
    }
}
