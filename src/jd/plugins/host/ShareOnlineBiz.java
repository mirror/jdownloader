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
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class ShareOnlineBiz extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "share-online.biz";

    private static final String PLUGIN_NAME = HOST;

    private static final String PLUGIN_VERSION = "2.0.0.0";

    private static final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?share\\-online\\.biz/download.php\\?id\\=[a-zA-Z0-9]{9}", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;
    private String url;
    private File captchaFile;

    public ShareOnlineBiz() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

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
        url = downloadLink.getDownloadURL();
        for (int i = 1; i < 3; i++) {
            try {
                Thread.sleep(1000);/* Sicherheitspause, sonst gibts 403 Response */
                requestInfo = HTTP.getRequest(new URL(url));
                if (requestInfo != null && requestInfo.getLocation() == null) {
                    String filename = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), "<span class=\"locatedActive\">Download °</span>", 0);
                    String[] sizev = SimpleMatches.getSimpleMatches(requestInfo.getHtmlCode(), "</font> (° °) angefordert");
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
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        downloadLink.setAvailable(false);
        return false;
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        try {
            switch (step.getStep()) {
            case PluginStep.STEP_PAGE:
                url = downloadLink.getDownloadURL();
                /* Nochmals das File überprüfen */
                if (!getFileInformation(downloadLink)) {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                return step;
            case PluginStep.STEP_GET_CAPTCHA_FILE:
                captchaFile = this.getLocalCaptchaFile(this);
                HTTPConnection captcha_con = new HTTPConnection(new URL("http://www.share-online.biz/captcha.php").openConnection());
                captcha_con.setRequestProperty("Referer", url);
                captcha_con.setRequestProperty("Cookie", requestInfo.getCookie());
                if (!JDUtilities.download(captchaFile, captcha_con) || !captchaFile.exists()) {
                    /* Fehler beim Captcha */
                    logger.severe("Captcha Download fehlgeschlagen!");
                    step.setStatus(PluginStep.STATUS_ERROR);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                    return step;
                }
                step.setParameter(captchaFile);
                return step;
            case PluginStep.STEP_PENDING:
                String captchaCode = (String) steps.get(1).getParameter();
                requestInfo = HTTP.postRequest((new URL(url)), requestInfo.getCookie(), url, null, "captchacode=" + captchaCode, true);
                if (requestInfo.getHtmlCode().contains("<span>Die Nummer ist leider nicht richtig oder ausgelaufen!</span>")) {
                    step.setStatus(PluginStep.STATUS_ERROR);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                    return step;
                }
                // hier müsste jetzt der download link zusammengebaut werden
                step.setParameter(15 * 1000L);
                return step;
            case PluginStep.STEP_DOWNLOAD:
                // hab ich nur zu testzwecken hier so
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                return step;
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return step;

    }

    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getAGBLink() {
        return "http://share-online.biz/rules.php";
    }

}
