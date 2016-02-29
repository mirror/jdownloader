package org.jdownloader.credits;

public class Credit {

    private String name;

    public String getName() {
        return name;
    }

    public CouplingType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getHomepage() {
        return homepage;
    }

    private CouplingType type;

    private String       description;
    private String       homepage;
    private License[]    licenses;

    private String       copyright;

    public String getCopyright() {
        return copyright;
    }

    public License[] getLicenses() {
        return licenses;
    }

    public Credit(String name, String copyright, String description, String homepage, CouplingType type, License... licenses) {
        this.name = name;
        this.type = type;
        this.copyright = copyright;
        this.description = description;
        this.homepage = homepage;
        this.licenses = licenses;
    }

}
