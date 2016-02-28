package org.jdownloader.credits;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.gui.translate._GUI;

public class CreditsManager {
    private ArrayList<Credit> list;

    private CreditsManager() {
        list = new ArrayList<Credit>();
        add(new Credit("RTMPDump", _GUI.T.credits_rtmpdump_description(), "http://rtmpdump.mplayerhq.hu", CouplingType.EXECUTABLE, License.GPL_V2));
        add(new Credit("Cling", _GUI.T.credits_cling_description(), "http://4thline.org/projects/cling/", CouplingType.LIBRARY, License.LGPL_V2_OR_LATER, License.CDDL));
        add(new Credit("7zipJBindings", _GUI.T.credits_7zipjbindings_description(), "http://sevenzipjbind.sourceforge.net/", CouplingType.LIBRARY, License.LGPL_V2_OR_LATER, new License("unRAR restriction", null, "licenses/cling.license")));
        add(new Credit("Synthetica", _GUI.T.credits_synthetica_description(), "http://www.javasoft.de/synthetica/", CouplingType.INTERFACE, new License("Paid License", "Licensee AppWork GmbH; LRN (#112044); Enterprise License", "licenses/synthetica.license")));
        add(new Credit("Icons8 IconSet", _GUI.T.credits_icons8_description(), "http://www.icons8.com", CouplingType.ICONS, License.CC_BY_ND_3, new License("Licensee AppWork GmbH", "Licensee AppWork GmbH; Captain Vector Plan", "licenses/Icons8_Paid_License.pdf")));
        add(new Credit("Tango IconSet", _GUI.T.credits_tango_description(), "http://tango.freedesktop.org/Tango_Icon_Library", CouplingType.ICONS, License.PUBLIC_DOMAIN));
        add(new Credit("FatCow IconSet", _GUI.T.credits_fatcow_description(), "http://www.fatcow.com/free-icons", CouplingType.ICONS, License.CC_BY_US_3));
        add(new Credit("Picol IconSet", _GUI.T.credits_picol_description(), "http://www.picol.org/icon_library.php", CouplingType.ICONS, License.CC_BY_SA_3));
        add(new Credit("Aha Soft Application IconSet", _GUI.T.credits_aha_description(), "http://www.small-icons.com/packs/24x24-free-application-icons.htm", CouplingType.ICONS, License.CC_BY_SA_3));
        add(new Credit("Oxygen Icon Set", _GUI.T.credits_oxygen_description(), "http://www.oxygen-icons.org/", CouplingType.ICONS, License.LGPL_V3));

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
