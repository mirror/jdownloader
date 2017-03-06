package org.jdownloader.credits;

import java.util.ArrayList;
import java.util.List;

public class CreditsManager {
    private ArrayList<Credit> list;

    private CreditsManager() {
        list = new ArrayList<Credit>();
        add(new Credit("RTMPDump", "© 2009 Andrej Stepanchuk;Copyright (C) 2010-2011 Howard Chu", "Protocol plugin for RTMP streams", "http://rtmpdump.mplayerhq.hu", CouplingType.EXECUTABLE, License.GPL_V2));
        add(new Credit("Cling", "© 2015, 4th Line GmbH, Switzerland", "Universal Plug and Play library", "http://4thline.org/projects/cling/", CouplingType.LIBRARY, License.LGPL_V2_OR_LATER, License.CDDL));
        add(new Credit("7zipJBindings", "Boris Brodski", "A java wrapper for 7-Zip C++ library", "http://sevenzipjbind.sourceforge.net/", CouplingType.LIBRARY, License.LGPL_V2_OR_LATER, new License("unRAR restriction", null, "licenses/cling.license")));
        add(new Credit("Synthetica", "Jyloo Software GmbH", "Look and Feel library", "http://www.javasoft.de/synthetica/", CouplingType.INTERFACE, new License("Paid License", "Licensee AppWork GmbH; LRN (#112044); Enterprise License", "licenses/synthetica.license")));
        add(new Credit("Icons8 IconSet", "© 2017 VisualPharm LLC", "Several Icons and Images", "http://www.icons8.com", CouplingType.ICONS, License.CC_BY_ND_3, new License("Licensee AppWork GmbH", "Licensee AppWork GmbH; Captain Vector Plan", "licenses/Icons8_Paid_License.pdf")));
        add(new Credit("Tango IconSet", "freedesktop.org", "Several Icons and Images", "http://tango.freedesktop.org/Tango_Icon_Library", CouplingType.ICONS, License.PUBLIC_DOMAIN));
        add(new Credit("Picol IconSet", "Melih Bilgil", "Several Icons and Images", "http://www.picol.org/icon_library.php", CouplingType.ICONS, License.CC_BY_SA_3));
        add(new Credit("Aha Soft Application IconSet", "© 2005-2016 Aha-Soft", "Several Icons and Images", "http://www.small-icons.com/packs/24x24-free-application-icons.htm", CouplingType.ICONS, License.CC_BY_SA_3));
        add(new Credit("Java D-Bus", "Matthew Johnson & Team", "Desktop-Bus IPC Framework", "https://www.freedesktop.org/wiki/Software/DBusBindings/", CouplingType.LIBRARY, License.AFL_V2_1, License.LGPL_V2));
        add(new Credit("Base64", "© 2004, Mikael Grev, MiG InfoCom AB", "Base64 En/Decoder", "base64@miginfocom.com", CouplingType.LIBRARY, new License("BSD", "BSD License", "licenses/Base64.license")));

    }

    public void add(Credit credit) {
        synchronized (list) {
            list.add(credit);
        }
    }

    public final static CreditsManager INSTANCE = new CreditsManager();

    public static CreditsManager getInstance() {
        return INSTANCE;
    }

    public List<Credit> list() {
        return list;
    }
}
