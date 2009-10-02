package jd.plugins.optional.customizer;

import java.io.Serializable;

import jd.parser.Regex;
import jd.utils.JDUtilities;

public class CustomizeSetting implements Serializable {

    private static final long serialVersionUID = 3295935612660256840L;

    private String name;

    private boolean enabled = true;

    private String regex;

    private String packageName;

    private String downloadDir = JDUtilities.getDefaultDownloadDirectory();

    private boolean extract = true;

    private String password;

    public CustomizeSetting(String name) {
        setName(name);
    }

    public boolean matches(String fileName) {
        return new Regex(fileName, regex).matches();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDownloadDir() {
        return downloadDir;
    }

    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }

    public boolean isExtract() {
        return extract;
    }

    public void setExtract(boolean extract) {
        this.extract = extract;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
