package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;
import jd.utils.JDUtilities;

public class oXygenWarez extends PluginForDecrypt {
    static private final String host = "oxygen-warez.com";
    private String version = "1.0.0.2";
    private static final String DEFAULT_PASSWORD = "www.oxygen-warez.com";
    private Pattern patternSupported = Pattern.compile("http://.*?oxygen-warez.com/(category|\\?id=).*", Pattern.CASE_INSENSITIVE);
    private Pattern PASSWORT = Pattern.compile("<P><B>Passwort:</B> <A HREF=\"\" onClick=\"CopyToClipboard\\(this\\); return\\(false\\);\">(.+?)</A></P>");
    private static final String DL_LINK = "<FORM ACTION=\"°\" METHOD=\"POST\" STYLE=\"display: inline;\" TARGET=\"_blank\">";
    private static final String ERROR_CAPTCHA = "Der Sichheitscode wurde falsch eingeben!";
    private static final String ERROR_CAPTCHA_TIME = "Der Sichheitscode ist abgelaufen!";
    private String pw = "";
    String strFavorites = this.getProperties().getStringProperty("FAVORITES", "rapidshare.com;uploaded.to;xirror.to");
    String[] favorites = strFavorites.split(";");
    public oXygenWarez() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigElements();
    }
    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */
    @Override
    public String getCoder() {
        return "DwD";
    }
    @Override
    public String getHost() {
        return host;
    }
    @Override
    public String getPluginID() {
        return "oXygenWarez-1.0.0.";
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

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        switch (step.getStep()) {
            case PluginStep.STEP_DECRYPT :
                Vector<String[]> decryptedLinks = new Vector<String[]>();

                RequestInfo reqinfo;
                try {
                    while (true) {
                        Vector<Vector<String>> results = null;
                        reqinfo = getRequest(new URL(parameter));
                        // Schleife für Anzahl an Favoriten +1 (Default-Hoster)
                        // durchlaufen:
                        // Suche nach Favorit und zuletzt nach Default-Hoster
                        String htmlCode = reqinfo.getHtmlCode().replaceAll("(?s)<!--.*?-->", "");
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
                            
                            if (favorites.length == i) {
                                favPattern = "<FORM ACTION=\"([^\"]+)\" [^>]+ NAME=\"download_form\" [^>]+>.*?<H1>Download \\(.*?, .*?Url.*?\\)</H1>.*?<IMG SRC=\"(/gfx/secure/[^\"]+)\" [^>]+>.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">";
                            } else {
                                // Favorit
                                String favorit = favorites[i].trim();

                                // Wenn Favorit leer, dann abbrechen
                                if (favorit.length() == 0)
                                    continue;

                                favPattern = "<FORM ACTION=\"([^\"]+)\" [^>]+ NAME=\"download_form\" [^>]+>.*?<H1>[^<>]+ \\(.*?" + favorit + ".*?, .*?Url.*?\\)</H1>.*?<IMG SRC=\"(/gfx/secure/[^\"]+)\" [^>]+>.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">.*?<INPUT TYPE=\"HIDDEN\" NAME=\"([^\"]+)\" VALUE=\"([^\"]+)\">";;
                            }

                            results = getAllSimpleMatches(htmlCode, Pattern.compile(favPattern, Pattern.MULTILINE | Pattern.DOTALL));

                            // Favorit/Default-Download gefunden?
                            if (results != null && results.isEmpty() == false) {
                                break;
                            }
                        } // end for
                        // Links herausfiltern
            
                        String formURL = "http://" + host + results.get(0).get(0);
                        String postvar = results.get(0).get(2) + "=" + results.get(0).get(3) + "&" + results.get(0).get(4) + "=" + results.get(0).get(5) + "&" + results.get(0).get(6) + "=" + results.get(0).get(7) + "&" + results.get(0).get(8) + "=" + results.get(0).get(9);
                        String captchaurl = "http://" + host + results.get(0).get(1);
                        File file = this.getLocalCaptchaFile(this);
                        if (!JDUtilities.download(file, captchaurl) || !file.exists()) {
                            logger.severe("Captcha Download fehlgeschlagen: " + captchaurl);
                            step.setParameter(null);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        String plainCaptcha = getCaptchaCode(file, this);
                        String inpHidden = "code=" + plainCaptcha + "&" + postvar;
                        pw = getFirstMatch(reqinfo.getHtmlCode(), PASSWORT, 1);
                        if (pw.matches("") || pw.matches("na") || pw.matches("n\\/a") || pw.matches("n\\|a"))
                            pw = DEFAULT_PASSWORD;
                        else if (pw.matches("(keines|keins|none|no|nein)"))
                            pw = "";

                        reqinfo = postRequest(new URL(formURL), inpHidden);
                        if (reqinfo.getHtmlCode().contains(ERROR_CAPTCHA)) {
                            if (file != null && plainCaptcha != null) {
                                JDUtilities.appendInfoToFilename(file, plainCaptcha, false);
                            }
                            logger.severe("falscher Captcha-Code");
                            continue; // retry

                        } else if (reqinfo.getHtmlCode().contains(ERROR_CAPTCHA_TIME)) {
                            logger.severe("Captcha-Code abgelaufen");
                            continue; // retry
                        }

                        // Captcha erkannt
                        if (file != null && plainCaptcha != null) {
                            JDUtilities.appendInfoToFilename(file, plainCaptcha, true);
                        }
                        break;
                    }
                    System.out.println(reqinfo.getHtmlCode());
                    Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), DL_LINK);
                    firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_MAX, links.size()));

                    for (int i = 0; i < links.size(); i++) {
                        String link = JDUtilities.urlEncode(links.get(i).get(0));
                        link = link.replaceAll("http://.*http://", "http://");
                        System.out.println(link);
                        decryptedLinks.add(new String[]{link, pw, null});

                        firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                    }

                    // Decrypt abschliessen
                    firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                    step.setParameter(decryptedLinks);

                } catch (IOException e) {
                    e.printStackTrace();
                }

        }
        return null;
    }
    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Default Passwort"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), "DEFAULT_PASSWORT", "Passwort").setDefaultValue(DEFAULT_PASSWORD));
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
