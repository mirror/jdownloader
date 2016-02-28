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

    private String    description;
    private String    homepage;
    private License[] licenses;

    public License[] getLicenses() {
        return licenses;
    }

    public Credit(String name, String description, String homepage, CouplingType type, License... licenses) {
        this.name = name;
        this.type = type;

        this.description = description;
        this.homepage = homepage;
        this.licenses = licenses;
    }

}
