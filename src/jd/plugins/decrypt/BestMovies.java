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

package jd.plugins.decrypt;

import java.io.File;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class BestMovies extends PluginForDecrypt {

    static private final String HOST = "best-movies.us";

    private String VERSION = "1.0.0";

    private String CODER = "jD-Team";
    static private final Pattern patternSupported = getSupportPattern("http://crypt.best-movies.us/go.php\\?id\\=\\d{1,}");
    static private final Pattern patternCaptcha_Needed = Pattern.compile("<img src=\"captcha.php\"");
    static private final Pattern patternCaptcha_Wrong = Pattern.compile("Der Sicherheitscode ist falsch");
    static private final Pattern patternIframe = Pattern.compile("<iframe src=\"(.+?)\"", Pattern.DOTALL);

    public BestMovies() {

        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();

    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String cryptedLink = (String) parameter;
        Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            try {
                URL url = new URL(cryptedLink);
                RequestInfo reqInfo = null;
                Matcher matcher;
                boolean bestmovies_continue = false;                
                reqInfo = HTTP.getRequest(url);
                for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {

                    matcher = patternCaptcha_Wrong.matcher(reqInfo.getHtmlCode());
                    if (matcher.find()) {
                        /* Falscher Captcha, Seite neu laden */
                        reqInfo = HTTP.getRequest(url);
                    }
                    /* Alle Requests müssen mit Cookie und Referer stattfinden */
                    String cookie = reqInfo.getCookie();
                    cookie = cookie.substring(0, cookie.indexOf(";") + 1);

                    matcher = patternCaptcha_Needed.matcher(reqInfo.getHtmlCode());
                    if (matcher.find()) {
                        /* Captcha vorhanden */                        
                        File captchaFile = this.getLocalCaptchaFile(this);
                        URL captcha_url = new URL("http://crypt.best-movies.us/captcha.php");
                        HTTPConnection captcha_con = new HTTPConnection(captcha_url.openConnection());
                        captcha_con.setRequestProperty("Referer", cryptedLink);
                        captcha_con.setRequestProperty("Cookie", cookie);
                        if (!JDUtilities.download(captchaFile, captcha_con) || !captchaFile.exists()) {
                            /* Fehler beim Captcha */
                            logger.severe("Captcha Download fehlgeschlagen!");
                            step.setParameter(null);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        String captchaCode = Plugin.getCaptchaCode(captchaFile, this);
                        if (captchaCode == null) {
                            /* abbruch geklickt */
                            step.setParameter(decryptedLinks);
                            return null;
                        }
                        reqInfo = HTTP.postRequest(new URL(cryptedLink), cookie, cryptedLink, null, "sicherheitscode=" + captchaCode + "&submit=Submit+Query", false);

                    } else {
                        /* Kein Captcha */
                        bestmovies_continue = true;
                        break;
                    }
                }
                if (bestmovies_continue == true) {
                    matcher = patternIframe.matcher(reqInfo.getHtmlCode());
                    if (matcher.find()) {
                        /* EinzelLink gefunden */
                        String link = matcher.group(1);                       
                        decryptedLinks.add(this.createDownloadlink((link)));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }
        step.setParameter(decryptedLinks);
        return null;
    }

    public boolean doBotCheck(File file) {
        return false;
    }

}
