package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class CodedTo extends PluginForDecrypt {
    private static final String  CODER          = "Bo0nZ";

    private static final String  HOST           = "coded.to";

    private static final String  PLUGIN_NAME    = HOST;

    private static final String  PLUGIN_VERSION = "1.0.0.0";

    private static final String  PLUGIN_ID      = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    //http://www.coded.to/?jump=6f4920ea25403ec77bee9efce43ea25e
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://.*?coded\\.to/.*?\\?jump\\=[a-zA-Z0-9]{32}", Pattern.CASE_INSENSITIVE);

    /*
     * Suchmasken
     */
    private static final String  FILES          = "<a target=\"_blank\" href=\"down.php?id=°\">";

    private static final String  LINK           = "<iframe src=\"°\" height=\"100%\" width=\"100%\"";

    private static final String  PASSWORD       = "Bitte gib das erforderliche Passwort ein";

    public CodedTo() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
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
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
            Vector<Vector<String>> files;
            try {
                String strURL = parameter;
                URL url = new URL(strURL);
                RequestInfo reqinfo = getRequest(url); // Seite aufrufen

                // solange bis Passwort eingegeben und Links gefunden wurden
                while (true) {
                    // Im HTML-Code nach "files"/"Forms" suchen
                    files = getAllSimpleMatches(reqinfo.getHtmlCode(), FILES);

                    // Passwort-Abfrage vorhanden und keine Links gefunden
                    if (reqinfo.getHtmlCode().contains(PASSWORD) && files.isEmpty()) {
                        String pwd = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");
                        reqinfo = postRequest(url, null, strURL, null, "pwd=" + pwd + "&pwsub=OK", true);
                    }
                    else {
                        break; // keine Passwort-Abfrage
                    }
                }

                // Anzahl Links
                progress.setRange(files.size());

                for (int i = 0; i < files.size(); i++) {
                    reqinfo = getRequest(new URL("http://www.coded.to/down.php?id=" + files.get(i).get(0)));

                    String html = JDUtilities.htmlDecode(reqinfo.getHtmlCode());
                    String newLink = getSimpleMatch(html, LINK, 0);

                    decryptedLinks.add(newLink);
                    progress.increase(1);
                }

                // Decrypt abschliessen

                step.setParameter(decryptedLinks);
            }
            catch (IOException e) {
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
