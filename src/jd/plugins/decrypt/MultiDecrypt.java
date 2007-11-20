package jd.plugins.decrypt;

import java.io.File;
import java.util.Collection;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.event.PluginEvent;
import jd.plugins.search.FileSearch;

public class MultiDecrypt extends PluginForDecrypt {

    static private String host = "MultiDecrypt";
    private String version = "0.1";
    static private String[] SUPPORTEDHOSTS = new String[]{"stacheldraht.be/show.php"};
    public MultiDecrypt() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        setConfigElements();
    }

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
        return "MultiDecrypt-0.1";
    }
    @Override
    public String getPluginName() {
        return host;
    }
    @Override
    public Pattern getSupportedLinks() {
        String strSupported = this.getProperties().getStringProperty("SUPPORTED", "");
        String[] Supp = strSupported.split(System.getProperty("line.separator"));
        String[] Supported = new String[Supp.length + SUPPORTEDHOSTS.length];
        for (int i = 0; i < Supp.length; i++) {
            Supported[i] = Supp[i];
        }
        for (int i = 0; i < SUPPORTEDHOSTS.length; i++) {
            Supported[Supp.length + i] = SUPPORTEDHOSTS[i];
        }

        String patternStr = "http://[\\w\\.]*?(";
        if (Supported.length > 0) {
            Supported[0] = Supported[0].replaceFirst("http://", "").trim();
            if (Supported[0].matches("www\\.[^\\/]+?\\."))
                Supported[0] = Supported[0].replaceFirst(".*?\\.", "");
            patternStr += Supported[0];
            for (int i = 1; i < Supported.length; i++) {
                Supported[i] = Supported[i].replaceFirst("http://", "").trim();
                if (Supported[i].matches("www\\.[^\\/]+?\\."))
                    Supported[i] = Supported[i].replaceFirst(".*?\\.", "");
                patternStr += "|" + Supported[i];
            }
        }
        patternStr += ").*";
        return Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
    }
    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String[]> decryptedLinks = new Vector<String[]>();
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_MAX, 1));
            FileSearch filesearch = new FileSearch();

            int inst = filesearch.getIntParam(FileSearch.PARAM_INST, 1);
            if (inst != 1)
                filesearch.getProperties().setProperty(FileSearch.PARAM_INST, 1);
            String lnk = parameter.replaceFirst("http://", "").trim();

            if (lnk.matches("[^\\/]+?\\.[^\\/]+?\\."))
                lnk = lnk.replaceFirst(".*?\\.", "").replaceFirst("\\/.*", "");
            else
                lnk = lnk.replaceFirst("\\/.*", "");
            lnk = "http://[\\w\\.]*?" + lnk + ".*";
            decryptedLinks.addAll((Collection<? extends String[]>) ((PluginStep) filesearch.doStep(new PluginStep(PluginStep.STEP_SEARCH, null), parameter)).getParameter());
            for (int i = decryptedLinks.size() - 1; i >= 0 && decryptedLinks.size() > i; i--) {
                String[] link = decryptedLinks.elementAt(i);
                if (link[0] == null || link[0].isEmpty() || link[0].matches(lnk))
                    decryptedLinks.remove(i);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                }
            }
            if (inst != 1)
                filesearch.getProperties().setProperty(FileSearch.PARAM_INST, inst);
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, null));
            logger.info(decryptedLinks.size() + " links Found");
            step.setParameter(decryptedLinks);
        }
        return null;
    }

    private void setConfigElements() {
        String[] hosts = new String[SUPPORTEDHOSTS.length];
            for (int i = 0; i < SUPPORTEDHOSTS.length; i++) {
               String lnk = SUPPORTEDHOSTS[i].replaceFirst("http://", "").trim();

                if (lnk.matches("[^\\/]+?\\.[^\\/]+?\\."))
                    lnk = lnk.replaceFirst(".*?\\.", "").replaceFirst("\\/.*", "");
                else
                    lnk = lnk.replaceFirst("\\/.*", "");
                hosts[i]=lnk;
            }
        if(hosts.length>0)
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, null, null, hosts, "Folgende Seiten sind hier fest eingetragen: "));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hier kann man URLS/Pattern hinzufügen die nach Links durchsucht werden sollen!"));
        ConfigEntry cfgTextField = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, getProperties(), "SUPPORTED", "URLS: ");
        cfgTextField.setDefaultValue("");
        config.addEntry(cfgTextField);
    }

    @Override
    public boolean doBotCheck(File file) {
        // TODO Auto-generated method stub
        return false;
    }
}