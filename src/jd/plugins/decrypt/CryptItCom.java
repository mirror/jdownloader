//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

// http://crypt-it.com/s/BXYMBR
// http://crypt-it.com/s/B44Z4A

public class CryptItCom extends PluginForDecrypt {

    static private final String  HOST               = "crypt-it.com";

    private String               VERSION            = "0.2.0";

    private String               CODER              = "jD-Team";

    static private final Pattern patternSupported   = getSupportPattern("(http|ccf)://[*]crypt-it.com/(s|e|d|c)/[a-zA-Z0-9]+");

    private static final String  PATTERN_PW         = "Passworteingabe";

    private String               PASSWORD_PROTECTED = "Passworteingabe erforderlich";

    public CryptItCom() {

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

        if (step.getStep() == PluginStep.STEP_DECRYPT) {

            // surpress jd warning
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            step.setParameter(decryptedLinks);

            parameter = parameter.replace("/s/", "/d/");
            parameter = parameter.replace("/e/", "/d/");
            parameter = parameter.replace("ccf://", "http://");

            try {

                requestInfo = getRequestWithoutHtmlCode(new URL(parameter), null, null, null, true);
               
                if (requestInfo.getConnection().getContentType().indexOf("text/html") >= 0) {
                    requestInfo = readFromURL(requestInfo.getConnection());
                    String cookie = requestInfo.getCookie();
                    if (requestInfo.containsHTML(PATTERN_PW)) {

                        String pass = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"));
                        String postData = "a=pw&pw=" + JDUtilities.urlEncode(pass);
                        requestInfo = postRequest(new URL(parameter), requestInfo.getCookie(), parameter, null, "a=pw&pw=" + pass, false);
                        if (requestInfo.containsHTML(PATTERN_PW)) {

                            logger.warning("Password wrong");
                            JDUtilities.getController().getUiInterface().showMessageDialog(JDLocale.L("plugins.decrypt.general.passwordWrong", "Passwort falsch"));
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return null;

                        }
                    }

                    parameter = parameter.replace("/c/", "/d/");
                    requestInfo = getRequestWithoutHtmlCode(new URL(parameter), cookie, null, null, true);
                }

                String folder = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY);
                String name = this.getFileNameFormHeader(requestInfo.getConnection());

                if (name.equals("redir.ccf") || !name.contains(".ccf")) {

                    logger.severe("Container not found");
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return null;

                }

                // download
                File file = new File(folder, name);
                int i = 0;

                while (file.exists()) {

                    String newName = name.substring(0, name.length() - 4) + "-" + String.valueOf(i) + ".ccf";
                    file = new File(folder, newName);
                    i++;

                }

                logger.info("Download container: " + file.getAbsolutePath());
                JDUtilities.download(file, requestInfo.getConnection());

                // read container
                JDController controller = JDUtilities.getController();
                controller.loadContainerFile(file);

                // delete container
                file.delete();

            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
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
