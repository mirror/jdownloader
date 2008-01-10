package jd.unrar;

import java.io.File;

import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDUtilities;

public class Utilities {
    public static Object loadObject(File fileInput, boolean asXML) {
      return JDUtilities.loadObject(((SimpleGUI) JDUtilities.getGUI()).getFrame(), fileInput, asXML);
    }
    public static void saveObject(Object objectToSave, File fileOutput, boolean asXML) {
        JDUtilities.saveObject(((SimpleGUI) JDUtilities.getGUI()).getFrame(), objectToSave, fileOutput, null, null, asXML);
    }
}
