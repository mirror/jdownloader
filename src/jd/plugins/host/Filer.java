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
import java.net.URL;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Filer extends PluginForHost {

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?filer.net/(file[\\d]+|get)/.*", Pattern.CASE_INSENSITIVE);
    // static private final Pattern GETID =
    // Pattern.compile("http://[\\w\\.]*?filer.net/file([\\d]+)/.*?",
    // Pattern.CASE_INSENSITIVE);
    static private final String HOST = "filer.net";
    static private final String PLUGIN_NAME = HOST;
    static private final String PLUGIN_VERSION = "0.5";
    static private final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    static private final String CODER = "JD-Team";
    // static private final String FILE_NOT_FOUND = "Konnte Download nicht
    // finden";
    // static private final String FILE_NOT_FOUND2 = "Oops! We couldn't find
    // this page for you.";
    // static private final String FREE_USER_LIMIT = "Momentan sind die Limits
    // f&uuml;r Free-Downloads erreicht";
    // static private final String CAPTCHAADRESS =
    // "http://www.filer.net/captcha.png";
    // <form method="post" action="/get/e14eaf55d686a2b.html"><p class="center">
    // static private final Pattern DOWNLOAD = Pattern.compile("<form
    // method=\"post\" action=\"(\\/dl\\/.*?)\">", Pattern.CASE_INSENSITIVE);
    // static private final Pattern WAITTIME = Pattern.compile("Bitte warten Sie
    // ([\\d]+)", Pattern.CASE_INSENSITIVE);
    // static private final Pattern INFO = Pattern.compile("(?s)<td><a
    // href=\"(\\/get\\/.*?.html)\">(.*?)</a></td>.*?<td>([0-9\\.]+) .*?</td>",
    // Pattern.CASE_INSENSITIVE);
    // static private final String WRONG_CAPTCHACODE = "<img
    // src=\"/captcha.png\"";
    private static final Pattern PATTERN_MATCHER_ERROR = Pattern.compile("errors", Pattern.CASE_INSENSITIVE);

    // private String cookie;
    // private String dlink = null;
    // private String url;
    // private int waitTime = 500;

    public Filer() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));

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

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        int maxCaptchaTries = 5;
        String code;
        String page = null;
        try {
            Browser br = new Browser();
            br.getPage(downloadLink.getDownloadURL());
            int tries = 0;
            while (tries < maxCaptchaTries) {
                File captchaFile = getLocalCaptchaFile(this, ".png");
                JDUtilities.download(captchaFile, br.openGetConnection("http://www.filer.net/captcha.png"));
                code = Plugin.getCaptchaCode(captchaFile, this);
                page = br.postPage(downloadLink.getDownloadURL(), "captcha=" + code);
                tries++;
                if (!page.contains("captcha.png")) break;
            }
            if (page.contains("captcha.png")) {
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
            }

            if (Regex.matches(page, PATTERN_MATCHER_ERROR)) {
                step.setStatus(PluginStep.STATUS_ERROR);
                step.setParameter("parameter");
                String error = new Regex(page, "folgende Fehler und versuchen sie es erneut.*?<ul>.*?<li>(.*?)<\\/li>").getFirstMatch();
                logger.severe("Error: " + error);
                step.setParameter(JDLocale.L("plugin.host.filernet.error." + JDUtilities.getMD5(error), error));
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                return step;

            }

            br.setFollowRedirects(false);
            page = br.submitForm(br.getForms()[1]);
            //        
            // if (requestInfo.containsHTML(FREE_USER_LIMIT)) {
            // logger.severe("Free User Limit reached");
            // downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            // step.setParameter("Free User Limit");
            // step.setStatus(PluginStep.STATUS_ERROR);
            // return step;
            // }
            // if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
            // logger.severe("Die Datei existiert nicht");
            // downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            // return step;
            // }
            //
            // String strWaitTime =
            // SimpleMatches.getFirstMatch(requestInfo.getHtmlCode(), WAITTIME,
            // 1);
            // if (strWaitTime != null) {
            // logger.severe("wait " + strWaitTime + " minutes");
            // waitTime = Integer.parseInt(strWaitTime) * 60 * 1000;
            // downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
            // step.setStatus(PluginStep.STATUS_ERROR);
            // logger.info(" WARTEZEIT SETZEN IN " + step + " : " + waitTime);
            // step.setParameter((long) waitTime);
            // return step;
            // }

            sleep(61000, downloadLink);

            HTTPConnection con = br.openGetConnection(br.getRedirectLocation());
            int length = con.getContentLength();
            downloadLink.setDownloadMax(length);
            logger.info("Filename: " + getFileNameFormHeader(con));
            if (getFileNameFormHeader(con) == null || getFileNameFormHeader(con).indexOf("?") >= 0) {
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                step.setParameter(20000l);
                return step;
            }
            downloadLink.setName(getFileNameFormHeader(con));

            dl = new RAFDownload(this, downloadLink, con);
            dl.setChunkNum(1);
            dl.startDownload();

            return step;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {

    }

    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        String page;
        File captchaFile;
        String code;
        int bytes;
        int maxCaptchaTries = 5;
        int tries = 0;
        while (maxCaptchaTries > tries) {
            try {

                Browser br = new Browser();
                br.getPage(downloadLink.getDownloadURL());
                captchaFile = getLocalCaptchaFile(this, ".png");
                JDUtilities.download(captchaFile, br.openGetConnection("http://www.filer.net/captcha.png"));
                code = Plugin.getCaptchaCode(captchaFile, this);
                page = br.postPage(downloadLink.getDownloadURL(), "captcha=" + code);
                if (Regex.matches(page, PATTERN_MATCHER_ERROR)) return false;
                bytes = (int) SimpleMatches.getBytes(new Regex(page, "<tr class=\"even\">.*?<th>DateigrÃ¶ÃŸe</th>.*?<td>(.*?)</td>").getFirstMatch());
                downloadLink.setDownloadMax(bytes);
                br.setFollowRedirects(false);
                br.submitForm(br.getForms()[1]);
                downloadLink.setName(getFileNameFormURL(new URL(br.getRedirectLocation())));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            tries++;
        }
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getAGBLink() {
        // TODO Auto-generated method stub
        return "http://www.filer.net/faq";
    }
}
