package jd;

import javax.swing.UIManager;

import org.appwork.swing.components.circlebar.TEst;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaWhiteVisionLookAndFeel;

public class Test {

    /**
     * @param args
     */
    public static void main(String[] args) {
        final String[] li = { "Licensee=RapidShare AG", "LicenseRegistrationNumber=102001", "Product=Synthetica", "LicenseType=Enterprise Site License", "ExpireDate=--.--.----", "MaxVersion=2.999.999" };
        UIManager.put("Synthetica.license.info", li);
        UIManager.put("Synthetica.license.key", "075DD94D-50F2C1E8-B7628175-3031CB2D-F8E8033E");
        UIManager.put("Synthetica.window.decoration", false);
        UIManager.put("Synthetica.focus.textComponents.enabled", false);

        UIManager.put("Synthetica.extendedFileChooser.rememberPreferences", Boolean.FALSE);

        // disables the lines in a tree
        UIManager.put("Synthetica.tree.line.type", "NONE");
        SyntheticaLookAndFeel.setLookAndFeel(SyntheticaWhiteVisionLookAndFeel.class.getName());
        TEst.main(args);
    }

}
