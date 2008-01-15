package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;
import jd.plugins.DownloadLink;
import jd.plugins.Form;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class DatenKlo extends PluginForHost {
    private static final String HOST = "datenklo.net";
    private static final String VERSION = "1.0.0.0";
    private static final Pattern PAT_SUPPORTED = Pattern.compile(
            "http://.*?datenklo\\.net/dl\\-[a-zA-Z0-9]{5}",
            Pattern.CASE_INSENSITIVE);
    private Form form;
    private File captchaFile;
    //
    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck
    @Override
    public String getCoder() {
        return "G4E";
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
    public DatenKlo() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }
    public PluginStep doStep(PluginStep step, DownloadLink downloadLink)
            throws Exception {
        switch (step.getStep()) {
        case PluginStep.STEP_GET_CAPTCHA_FILE:

            String dlurl = downloadLink.getUrlDownloadDecrypted();
            String password = new Regexp(dlurl, "\\&down_passwort\\=(.*)")
                    .getFirstMatch();
            if (password != null)
                dlurl = dlurl.replaceFirst("\\&down_passwort.*", "");
            requestInfo = getRequest(new URL(dlurl));
            Form[] forms = Form.getForms(requestInfo);
            if (forms == null || forms.length == 0 || forms[0] == null) {
                step.setStatus(PluginStep.STATUS_ERROR);
                logger.severe("konnte den Download nicht finden");
                downloadLink
                        .setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                return null;
            }
            form = forms[0];
            form.withHtmlCode = false;
            if (requestInfo.getHtmlCode().contains(
                    "type=\"text\" name=\"down_passwort\"")) {
                if (password != null)
                    form.put("down_passwort", password);
                else form.put("down_passwort", JDUtilities.getController()
                        .getUiInterface().showUserInputDialog(
                                "Please enter the password!"));
            }
            captchaFile = getLocalCaptchaFile(this, ".gif");
            String captchaAdress = "http://"
                    + HOST
                    + new Regexp(requestInfo.getHtmlCode(),
                            "(/lib/captcha/CaptchaImage.php.*?)\"")
                            .getFirstMatch();
            logger.info("CaptchaAdress:" + captchaAdress);
            boolean fileDownloaded = JDUtilities.download(captchaFile,
                    getRequestWithoutHtmlCode(new URL(captchaAdress),
                            requestInfo.getCookie(), null, true)
                            .getConnection());
            if (!fileDownloaded || !captchaFile.exists()
                    || captchaFile.length() == 0) {
                logger.severe("Captcha not found");
                downloadLink
                        .setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            form.put("down_captcha", Plugin.getCaptchaCode(captchaFile, this));
            step = nextStep(step);
        case PluginStep.STEP_DOWNLOAD:
            if (aborted) {
                logger.warning("Plugin abgebrochen");
                downloadLink.setStatus(DownloadLink.STATUS_TODO);
                step.setStatus(PluginStep.STATUS_TODO);
                return step;
            }
            URLConnection urlConnection = form.getConnection();
            int size = urlConnection.getContentLength();
            if (urlConnection.getContentType().matches(".*html.*")) {
                if (size == 170) {
                    logger.warning("Password falsch");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                } else if (size == 160) {
                    logger.severe("captcha wrong");
                    JDUtilities.appendInfoToFilename(captchaFile, "_" + form.vars.get("down_captcha"), false);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
            }
            downloadLink.setName(getFileNameFormHeader(urlConnection));
            downloadLink.setDownloadMax(urlConnection.getContentLength());
            if (!hasEnoughHDSpace(downloadLink)) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            if (download(downloadLink, urlConnection)) {
                step.setStatus(PluginStep.STATUS_DONE);
                downloadLink.setStatus(DownloadLink.STATUS_DONE);
                return null;
            } else if (aborted) {
                logger.warning("Plugin abgebrochen");
                downloadLink.setStatus(DownloadLink.STATUS_TODO);
                step.setStatus(PluginStep.STATUS_TODO);
            } else {
                logger.severe("unknown error");
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                step.setStatus(PluginStep.STATUS_ERROR);
            }
        }
        return null;
    }
    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            RequestInfo requestInfo = getRequest(new URL(downloadLink
                    .getUrlDownloadDecrypted()));
            String fileName = new Regexp(requestInfo.getHtmlCode(),
                    "<td>Datei: </td>.*?<td>(.*?)</td>").getFirstMatch();
            String[][] fileSize = new Regexp(requestInfo.getHtmlCode(),
                    "<td>Dateigr.*?: </td>.*?<td>(.*?) (.*?)</td>")
                    .getMatches();
            String password = "";
            if (requestInfo.getHtmlCode().contains(
                    "type=\"text\" name=\"down_passwort\""))
                password = "&down_passwort="
                        + JDUtilities.getController().getUiInterface()
                                .showUserInputDialog(
                                        "Please enter the password!");
            // Wurden DownloadInfos gefunden? --> Datei ist vorhanden/online
            if (fileName != null && fileSize != null) {
                downloadLink.setName(fileName);
                downloadLink.setEncryptedUrlDownload(downloadLink
                        .getUrlDownloadDecrypted()
                        + password);
                try {
                    double length = Double.parseDouble(fileSize[0][0].trim());
                    int bytes;
                    String type = fileSize[0][1].toLowerCase();
                    if (type.equalsIgnoreCase("kb")) {
                        bytes = (int) (length * 1024);
                    } else if (type.equalsIgnoreCase("mb")) {
                        bytes = (int) (length * 1024 * 1024);
                    } else {
                        bytes = (int) length;
                    }
                    downloadLink.setDownloadMax(bytes);
                }
                catch (Exception e) {}
                // Datei ist noch verfuegbar
                return true;
            }
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
        return Integer.MAX_VALUE;
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
        
        return "http://www.datenklo.net/agb";
    }
}
