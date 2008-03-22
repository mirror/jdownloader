//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.controlling.interaction;

import java.io.File;
import java.io.Serializable;
import java.util.Vector;
import jd.config.Configuration;
import jd.update.WebUpdater;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class CaptchaMethodLoader extends Interaction implements Serializable {

    private static final String NAME              = JDLocale.L("interaction.captchaMethodLoader.name", "Captcha Erkennung aktualisieren");

    public static String        PROPERTY_QUESTION = "INTERACTION_" + NAME + "_QUESTION";

    public CaptchaMethodLoader() {}

    @Override
    public boolean doInteraction(Object arg) {
       // if(JDUtilities.getSubConfig("JAC").getBooleanProperty(Configuration.USE_CAPTCHA_EXCHANGE_SERVER, false))return false;
        String method = (String) arg;
        WebUpdater updater = new WebUpdater(null);

        int oldCid = JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1);
        updater.setCid(oldCid);
        logger.finer("Get available files");
        Vector<Vector<String>> files = updater.getAvailableFiles();
        if (files == null) {
            logger.info(updater.getLogger() + "");
            return false;
        }
        updater.filterAvailableUpdates(files, JDUtilities.getResourceFile("."));

        for (int i = files.size() - 1; i >= 0; i--) {
            if (!files.get(i).get(0).startsWith("jd/captcha/methods/" + method)) {
                files.remove(i);
            }
        }
        logger.info("New method files: " + files);
        if (files.size() == 0) {
            //logger.info(updater.getLogger() + "");
            return false;
        }

        String akt;

        int updatedFiles = 0;
        for (int i = files.size() - 1; i >= 0; i--) {
            akt = JDUtilities.getResourceFile(files.elementAt(i).elementAt(0)).getAbsolutePath();
            if (!new File(akt + ".noUpdate").exists()) {
                updatedFiles++;

                if (files.elementAt(i).elementAt(0).indexOf("?") >= 0) {
                    String[] tmp = files.elementAt(i).elementAt(0).split("\\?");
                    logger.info("Webupdater: direktfile: " + tmp[1] + " to " + new File(tmp[0]).getAbsolutePath());
                    JDUtilities.downloadBinary(tmp[0], tmp[1]);
                }
                else {
                    logger.info("Webupdater: file: " + updater.getOnlinePath() + "/" + files.elementAt(i).elementAt(0) + " to " + akt);
                    JDUtilities.downloadBinary(akt, updater.getOnlinePath() + "/" + files.elementAt(i).elementAt(0));
                }
                logger.info("Webupdater: ready");
            }
        }

     

        return true;
    }

    /**
     * Nichts zu tun. WebUpdate ist ein Beispiel für eine ThreadInteraction
     */
    public void run() {}

    public String toString() {
        return NAME;
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    @Override
    public void initConfig() {

    }

    @Override
    public void resetInteraction() {}
}
