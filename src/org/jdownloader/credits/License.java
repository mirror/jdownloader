package org.jdownloader.credits;

public class License {
    public static final License GPL_V2           = new License("GPL v2", "GNU General Public License 2", "licenses/gplv2.txt");
    public static final License GPL_V2_OR_LATER  = new License("GPL v2 or later", "GNU General Public License 2", "licenses/gplv2.txt");

    public static final License LGPL_V2          = new License("LGPL v2", "GNU Lesser General Public License 2", "licenses/lgplv2.txt");
    public static final License LGPL_V2_OR_LATER = new License("LGPL v2 or later", "GNU Lesser General Public License 2", "licenses/lgplv2.txt");

    public static final License CDDL             = new License("CDDL", "Common Development and Distribution License", "licenses/cddl.txt");
    public static final License CC_BY_ND_3       = new License("CC BY-ND 3.0", "Creative Commons Attribution-NoDerivs 3.0 Unported", "licenses/CreativeCommonsAttributionNonDerivs3.txt");
    public static final License PUBLIC_DOMAIN    = new License("Public Domain", "Public Domain", "licenses/PublicDomain.txt");
    public static final License CC_BY_US_3       = new License("CC BY 3.0 US", "Creative Commons Attribution 3.0 United States", "licenses/CreativeCommonsAttributionUS3.txt");
    public static final License CC_BY_SA_3       = new License("CC BY-SA 3.0", "Creative Commons Attribution-ShareAlike 3.0 Unported", "licenses/CreativeCommonsAttributionShareAlikeUnported3.txt");
    public static final License LGPL_V3          = new License("LGPL v3", "GNU Lesser General Public License 3", "licenses/lgplv3.txt");
    public static final License LGPL_V3_OR_LATER = new License("LGPL v3 or later", "GNU Lesser General Public License 3", "licenses/lgplv3.txt");
    public static final License AFL_V2_1         = new License("AFL v2.1", "Academic Free License 2.1", "licenses/afl21.txt");

    private String              label;

    public String getLabel() {
        return label;
    }

    public String getRelPath() {
        return relPath;
    }

    private String relPath;
    private String longName;

    public String getLongName() {
        return longName;
    }

    public License(String name, String longName, String relPath) {
        this.label = name;
        this.relPath = relPath;
        this.longName = longName;
    }

}
