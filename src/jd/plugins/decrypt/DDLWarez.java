package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.Form;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class DDLWarez extends PluginForDecrypt {
    final static String host             = "ddl-warez.org";

    private String      version          = "1.0.0.0";

    private Pattern     patternSupported = getSupportPattern("http://[*]ddl-warez\\.org/detail\\.php\\?id=[\\d]{4}[*]");
  
    public DDLWarez() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        default_password.add("ddl-warez");
    }

    @Override
    public String getCoder() {
        return "Bo0nZ";
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

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url); // Seite aufrufen
                
                Form []forms = reqinfo.getForms();

                //first form is the search form, not needed
                progress.setRange(forms.length -1);
                
                for(int i=1; i<forms.length; ++i){
                	RequestInfo formInfo = forms[i].getRequestInfo();
                	
                	URL urlredirector = new URL(getBetween(formInfo.getHtmlCode(), "<FRAME SRC=\"", "\""));
                	RequestInfo reqinfoRS = getRequest(urlredirector);

                	decryptedLinks.add(this.createDownloadlink(getBetween(reqinfoRS.getHtmlCode(), "<FRAME SRC=\"", "\"")));
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
