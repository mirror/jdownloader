package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;

public class Safelink extends PluginForDecrypt {
    static private final String host = "safelink.in";

    private String version = "2.0.0.0";
    // rs-M2MjVTNyIjN/lvs0123.part1.rar.html
    // http://www.rapidsafe.net/rs-M2MjVTNyIjN/lvs0123.part1.rar.html
    //   http://www.safelink.in/rc-UjZ4MWOwAjN/DG2.part02.rar.html
    private Pattern patternSupported = Pattern.compile("http://.*?(safelink\\.in|85\\.17\\.177\\.195)/r[cs]\\-[a-zA-Z0-9]{11}/.*", Pattern.CASE_INSENSITIVE);

    public Safelink() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */
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
        return "Salfelink"+version;
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
                System.out.println(parameter);
                Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
                progress.setRange(1);
                parameter=parameter.replaceFirst("http://.*?/r", "http://serienjunkies.org/safe/r");
                System.out.println(parameter);
                decryptedLinks.add(this.createDownloadlink(parameter));
                progress.increase(1);
                // veraltet: firePluginEvent(new PluginEvent(this,
                // PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                currentStep = null;
                step.setParameter(decryptedLinks);
                break;

        }
        return null;
    }
}
