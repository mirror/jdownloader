package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.controlling.DistributeData;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Cryptlinkws extends PluginForDecrypt {

    static private String host = "cryptlink.ws";

    private String version = "1.0.0.0";

    final static private Pattern patternSupported_File = Pattern.compile("http://[\\w\\.]*?cryptlink\\.ws/crypt\\.php\\?file=[0-9]+", Pattern.CASE_INSENSITIVE);
    final static private Pattern patternSupported_Folder = Pattern.compile("http://[\\w\\.]*?cryptlink\\.ws/\\?file=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    final static private Pattern patternSupported = Pattern.compile(patternSupported_Folder.pattern() + "|" + patternSupported_File.pattern(), Pattern.CASE_INSENSITIVE);

    public Cryptlinkws() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return host + "-" + version;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public PluginStep doStep(PluginStep step, String parameter) {
        String cryptedLink = (String) parameter;
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = null;
                RequestInfo reqinfo = null;
                if (cryptedLink.matches(patternSupported_File.pattern())) {
                    /* eine einzelne Datei */
                    url = new URL(cryptedLink);
                    reqinfo = HTTP.getRequest(url);
                    String link = new Regex(reqinfo.getHtmlCode(), "unescape\\(('|\")(.*?)('|\")\\)").getFirstMatch(2);
                    link = JDUtilities.htmlDecode(JDUtilities.htmlDecode(link));
                    url = new URL("http://www.cryptlink.ws/" + link);
                    reqinfo = HTTP.getRequest(url);
                    link = new Regex(reqinfo.getHtmlCode(), "unescape\\(('|\")(.*?)('|\")\\)").getFirstMatch(2);
                    link = JDUtilities.htmlDecode(JDUtilities.htmlDecode(link));
                    if (link.startsWith("cryptfiles/")) {
                        /* weiterleitung durch den server */
                        url = new URL("http://www.cryptlink.ws/" + link);
                        reqinfo = HTTP.getRequest(url);
                        decryptedLinks.addAll(new DistributeData(reqinfo.getHtmlCode()).findLinks(false));
                    } else {
                        /* direkte weiterleitung */
                        decryptedLinks.add(this.createDownloadlink(link));
                    }
                } else if (cryptedLink.matches(patternSupported_Folder.pattern())) {
                    /* ein Folder */
                    boolean do_continue = false;
                    for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                        String post_parameter = "";
                        url = new URL(cryptedLink);
                        reqinfo = HTTP.getRequest(url);
                        if (reqinfo.containsHTML(">Ordnerpasswort:<")) {
                            String password = JDUtilities.getGUI().showUserInputDialog("Ordnerpasswort?");
                            if (password == null) {
                                /* auf abbruch geklickt */
                                step.setParameter(decryptedLinks);
                                return null;
                            }
                            post_parameter += "folderpass=" + JDUtilities.urlEncode(password);
                        }
                        if (reqinfo.containsHTML("captcha.php")) {
                            File captchaFile = getLocalCaptchaFile(this);
                            String captchaCode;
                            HTTPConnection captcha_con = new HTTPConnection(new URL("http://www.cryptlink.ws/captcha.php").openConnection());
                            captcha_con.setRequestProperty("Referer", cryptedLink);
                            captcha_con.setRequestProperty("Cookie", reqinfo.getCookie());
                            if (!captcha_con.getContentType().contains("text") && !JDUtilities.download(captchaFile, captcha_con) || !captchaFile.exists()) {
                                /* Fehler beim Captcha */
                                logger.severe("Captcha Download fehlgeschlagen!");
                                step.setParameter(decryptedLinks);
                                return null;
                            }
                            /* CaptchaCode holen */
                            if ((captchaCode = Plugin.getCaptchaCode(captchaFile, this)) == null) {
                                step.setParameter(decryptedLinks);
                                return null;
                            }
                            if (post_parameter!="") post_parameter +="&";
                            post_parameter += "captchainput=" + JDUtilities.urlEncode(captchaCode);
                        }
                        if (post_parameter != "") reqinfo = HTTP.postRequest(new URL("http://www.cryptlink.ws/index.php?action=getfolder"), reqinfo.getCookie(), cryptedLink, null, post_parameter, false);
                        if (!reqinfo.containsHTML("Wrong Password! Klicken Sie") && !reqinfo.containsHTML("Wrong Captchacode! Klicken Sie")) {
                            do_continue = true;
                            break;
                        }
                    }
                    if (do_continue == true) {
                        String[] links = new Regex(reqinfo.getHtmlCode(), Pattern.compile("href=\"crypt\\.php\\?file=(\\d+)\"", Pattern.CASE_INSENSITIVE)).getMatches(1);
                        progress.setRange(links.length);
                        for (int i = 0; i < links.length; i++) {
                            decryptedLinks.add(this.createDownloadlink("http://www.cryptlink.ws/crypt.php?file=" + links[i]));
                            progress.increase(1);
                        }
                    }
                }

                step.setParameter(decryptedLinks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}