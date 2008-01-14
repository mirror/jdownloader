package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class SerienFreaksTv extends PluginForDecrypt {
    private static final String  CODER              = "Bo0nZ/Coa fixed";

    private static final String  HOST               = "serienfreaks.tv";

    private static final String  PLUGIN_NAME        = HOST;

    private static final String  PLUGIN_VERSION     = "1.0.0.0";

    private static final String  PLUGIN_ID          = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    //http://serienfreaks.tv/?id=5554
    private static final Pattern PAT_SUPPORTED      = getSupportPattern("http://[*]serienfreaks\\.tv/\\?id\\=[\\d]+");

    /*
     * Suchmasken (z.B. Fehler)
     */
    private static final String  ERROR_CAPTCHA      = "Der Sichheitscode ist";

    private static final String  ERROR_CAPTCHA_TIME = "Der Sichheitscode ist abgelaufen!";

    private static final Pattern CATEGORY_URL       = getSupportPattern("http://[*]serienfreaks.tv/[*]\\?cat=[+]");

    private static final Pattern FILE_URL           = getSupportPattern("http://[*]serienfreaks.tv/[*]\\?id=[+]");

    private static final String  DL_LINK            = "<FORM ACTION=\"°\" METHOD=\"POST\" STYLE=\"display: inline;\" TARGET=\"_blank\">";

    private static final String  FILE_LINK          = "<A HREF=\"?id=°\">";

   

    /*
     * Konstruktor
     */
    public SerienFreaksTv() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        default_password.add("serienfreaks.tv");
        default_password.add("serienfreaks.dl.am");

        this.initConfig();
    }

    /*
     * Funktionen
     */
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
        return PLUGIN_ID;
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
        return PLUGIN_VERSION;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            // bevorzugte Hoster
            String strFavorites = this.getProperties().getStringProperty("FAVORITES", "rapidshare.com;xirror.to");
            String[] favorites = strFavorites.split(";");

            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            RequestInfo reqinfo; // Zum Aufrufen von WebSeiten

            try {
                String strURL = parameter;
                URL url = new URL(parameter);

                // ist der Link eine Kategorie?
                if (countOccurences(strURL, CATEGORY_URL) > 0) {
                    reqinfo = getRequest(url);
                    Vector<Vector<String>> fileLinks = getAllSimpleMatches(reqinfo.getHtmlCode(), FILE_LINK);

                    progress.setRange(fileLinks.size());

                    // fuege alle gefundenen Dateien der Kategorie als
                    // Decrypt-Auftrag hinzu
                    for (int i = 0; i < fileLinks.size(); i++) {
                       
                   
                       decryptedLinks.add(this.createDownloadlink(url.getProtocol() + "://" + url.getHost() + url.getPath() + "?id=" + fileLinks.get(i).get(0)));
                        progress.increase(1);
                    }

                }
                else if (countOccurences(strURL, FILE_URL) > 0) {
                    // ist der Link ein Download?
                    // bis Captcha erkannt
                    File captchaFile = null;
                    logger.info("FILE");
                    String plainCaptcha = null;
                    while (true) {

                        Vector<Vector<String>> results = null;
                        reqinfo = getRequest(url);

                        // Schleife für Anzahl an Favoriten +1 (Default-Hoster)
                        // durchlaufen:
                        // Suche nach Favorit und zuletzt nach Default-Hoster
                        for (int i = 0; i < favorites.length + 1; i++) {
                            String favPattern;

                            // Suche nach Favorit/Default-Download
                            // nutze "non-greedy matching", also keine gierige
                            // Suche (.*?)
                            // nutze multiline-mode
                            // nutze dotall-mode (. steht für jedes Zeichen,
                            // auch newline)

                            // Zum Schluss nach Default-Download suchen anstatt
                            // nach Favorit
                            //<form action="/?id=5903&amp;location=mirror" enctype="multipart/form-data" method="post" name="download_form" style="display: inline;" target="id_5903_download" onsubmit="OpenPopup('about:blank', this.target, 720, 560); return(true);">
                            
                            if (favorites.length == i) {
                                favPattern = "<FORM ACTION=\"([^\"]+)\" [^>]+ NAME=\"download_form\" [^>]+>.*?<H1>Download \\(.*?, .*?Url.*?\\)</H1>.*?<IMG SRC=\"(/gfx/secure/[^\"]+)\" [^>]+>.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">";
                            }
                            else {
                                // Favorit
                                String favorit = favorites[i].trim();

                                // Wenn Favorit leer, dann abbrechen
                                if (favorit.length() == 0) continue;

                                favPattern = "<FORM ACTION=\"([^\"]+)\" [^>]+ NAME=\"download_form\" [^>]+>.*?<H1>[^<>]+ \\(.*?" + favorit + ".*?, .*?Url.*?\\)</H1>.*?<IMG SRC=\"(/gfx/secure/[^\"]+)\" [^>]+>.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">";
                                ;
                            }

                            results = getAllSimpleMatches(reqinfo.getHtmlCode(), Pattern.compile(favPattern, Pattern.MULTILINE | Pattern.DOTALL));

                            // Favorit/Default-Download gefunden?
                            if (results != null && results.isEmpty() == false) {
                                break;
                            }
                        } // end for

                        // Kein Download gefunden?
                        if (results == null || results.isEmpty()) {
                            logger.severe("kein Download gefunden");
                            break;
                        }

                        String formURL = results.get(0).get(0);
                        String captchaURL = results.get(0).get(1);
                        String formHiddenName1 = results.get(0).get(2);
                        String formHiddenValue1 = results.get(0).get(3);
                        String formHiddenName2 = results.get(0).get(4);
                        String formHiddenValue2 = results.get(0).get(5);
                        String formHiddenName3 = results.get(0).get(6);
                        String formHiddenValue3 = results.get(0).get(7);
                        String formHiddenName4 = results.get(0).get(8);
                        String formHiddenValue4 = results.get(0).get(9);
                        formURL = "http://" + HOST + formURL;

                        // CAPTCHA
                        captchaURL = "http://" + HOST + captchaURL;
                        captchaFile = getLocalCaptchaFile(this, ".gif");

                        // Captcha downloaden
                        boolean dlSuccess = JDUtilities.download(captchaFile, captchaURL);

                        // Captcha-Download nicht erfolgreich?
                        if (!dlSuccess || !captchaFile.exists() || captchaFile.length() == 0) {
                            logger.severe("Captcha-Download nicht erfolgreich. Versuche erneut.");

                            try {
                                Thread.sleep(1000);
                            }
                            catch (InterruptedException e) {
                            }

                            continue; // retry
                        }

                        // Captcha-Erkennung
                        plainCaptcha = Plugin.getCaptchaCode(captchaFile, this);
                        reqinfo = postRequest(new URL(formURL), "code=" + plainCaptcha + "&" + formHiddenName1 + "=" + formHiddenValue1 + "&" + formHiddenName2 + "=" + formHiddenValue2 + "&" + formHiddenName3 + "=" + formHiddenValue3+ "&" + formHiddenName4 + "=" + formHiddenValue4);

                        // Falscher Captcha-Code?
                        if (reqinfo.getHtmlCode().contains(ERROR_CAPTCHA)) {
                            if (captchaFile != null && plainCaptcha != null) {
                                JDUtilities.appendInfoToFilename(captchaFile, plainCaptcha, false);
                            }
                            logger.severe("falscher Captcha-Code");
                            continue; // retry

                        }
                        else if (reqinfo.getHtmlCode().contains(ERROR_CAPTCHA_TIME)) {
                            logger.severe("Captcha-Code abgelaufen");
                            continue; // retry
                        }

                        // Captcha erkannt
                        if (captchaFile != null && plainCaptcha != null) {
                            JDUtilities.appendInfoToFilename(captchaFile, plainCaptcha, true);
                        }
                        break;
                    }

                    // suche nach Links
                  
                    Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), DL_LINK);
                    progress.setRange(links.size());

                    for (int i = 0; i < links.size(); i++) {
                     
                        
                        decryptedLinks.add(this.createDownloadlink(links.get(i).get(0)));
                        progress.increase(1);
                    }

                }// else: kein unterstuetzter Link

                // Decrypt abschliessen

                step.setParameter(decryptedLinks);

            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void initConfig() {
      //  ConfigEntry cfg;
      
        ConfigEntry cfgLabel1 = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hier kannst du deine bevorzugten Hoster angeben (durch Semikolon getrennt).");
        ConfigEntry cfgLabel2 = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Sofern vorhanden wird dann von diesem Hoster geladen, im anderen Fall wird der");
        ConfigEntry cfgLabel3 = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Standard-Hoster verwendet.");
        ConfigEntry cfgTextField = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), "FAVORITES", "Hoster: ");
        cfgTextField.setDefaultValue("Rapidshare.com;Xirror.com");

        config.addEntry(cfgLabel1);
        config.addEntry(cfgLabel2);
        config.addEntry(cfgLabel3);
        config.addEntry(cfgTextField);
    }

}
