package jd.plugins.decrypt;

import java.io.File;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;

public class FileUploadnet extends PluginForDecrypt {
    static private final String host = "File-Upload.net Decrypter";
    private String version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?member\\.file-upload\\.net/(.*?)/(.*)", Pattern.CASE_INSENSITIVE);

    public FileUploadnet() {
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
        return "File-Upload.net Parser";
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
            String user = new Regex(parameter, "upload\\.net/(.*?)/").getFirstMatch();
            String file = new Regex(parameter, user + "/(.*)").getFirstMatch();
            String link = "http://www.file-upload.net/member/data3.php?user=" + user + "&name=" + file;
            link.replaceAll(" ","%20");
            decryptedLinks.add(this.createDownloadlink(link));
            step.setParameter(decryptedLinks);
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}