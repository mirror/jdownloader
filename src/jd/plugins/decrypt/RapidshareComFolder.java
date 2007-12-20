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

public class RapidshareComFolder extends PluginForDecrypt {

    static private final String host             = "rapidshare.com folder";

    private String              version          = "1.0.0.0";
    //rapidshare.com/users/AM0F5G
    static private final Pattern patternSupported = Pattern.compile("http://.*?rapidshare\\.com/users/[a-zA-Z0-9]{6}", Pattern.CASE_INSENSITIVE);

    private String              password         = "";

    private Vector<String>      decryptedLinks   = new Vector<String>();

    private URL                 url;

    public RapidshareComFolder() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));

    }

    @Override
    public String getCoder() {
        return "Botzi";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Rapidshare.com Folder-1.0.0.";
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
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            try {
                url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url);

                while (true) {
                    int check = countOccurences(reqinfo.getHtmlCode(), Pattern.compile("method=\"post\">Passwort:"));

                    if (check > 0) {
                        password = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");

                        if (password == null) {
                            step.setParameter(decryptedLinks);
                            return null;
                        }

                        reqinfo = postRequest(url, "password=" + password);
                    }
                    else {
                        break;
                    }
                }

                getLinks(reqinfo.getHtmlCode());
                progress.setRange(decryptedLinks.size());

                for (int i = 0; i < decryptedLinks.size(); i++) {
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

    // Rekursion zum auslesen der
    private void getLinks(String source) {
        RequestInfo reqhelp;
        Vector<Vector<String>> ordner = getAllSimpleMatches(source, "fileicon.gif\"> <input type=\"submit\" name=\"browse\" value=\"°\"");

        for (int i = 0; i < ordner.size(); i++) {
            try {
                String hpost = ordner.get(i).get(0);
                hpost = hpost.replaceAll(" ", "+");
                hpost = hpost.replaceAll("=", "%3D");
                reqhelp = postRequest(url, "password" + password + "=&subpassword=&browse=" + hpost);
                getLinks(reqhelp.getHtmlCode());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        Vector<Vector<String>> links = getAllSimpleMatches(source, "<center> <a href=\"°\" target=\"_blank\"");
        for (int i = 0; i < links.size(); i++) {
            decryptedLinks.add(links.get(i).get(0));
        }
    }
}