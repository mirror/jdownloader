//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//     along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.laf;

import javax.swing.UIManager;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.synthetica.SyntheticaHelper;
import org.appwork.utils.swing.dialog.LAFManagerInterface;
import org.jdownloader.gui.laf.JDDefaultLookAndFeel;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class LookAndFeelController implements LAFManagerInterface {
    private static final String                DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL = JDDefaultLookAndFeel.class.getName();
    private static final LookAndFeelController INSTANCE                                                      = new LookAndFeelController();

    /**
     * get the only existing instance of LookAndFeelController. This is a singleton
     * 
     * @return
     */
    public static LookAndFeelController getInstance() {
        return LookAndFeelController.INSTANCE;
    }

    private LAFOptions                     lafOptions;
    private GraphicalUserInterfaceSettings config;
    private String                         laf = null;

    /**
     * Create a new instance of LookAndFeelController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private LookAndFeelController() {
        config = JsonConfig.create(GraphicalUserInterfaceSettings.class);
    }

    /**
     * Config parameter to store the users laf selection
     */

    public static final String DEFAULT_PREFIX = "LAF_CFG";
    private static boolean     uiInitated     = false;

    /**
     * setups the correct Look and Feel
     */
    public synchronized void setUIManager() {
        if (uiInitated) return;
        uiInitated = true;
        long t = System.currentTimeMillis();
        try {
            // de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setLookAndFeel("de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel");
            // if (true) return;

            laf = DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL;
            LogController.GL.info("Use Look & Feel: " + laf);

            if (laf.contains("Synthetica") || laf.equals(DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL)) {

                //
                try {

                    ExtTooltip.createConfig(ExtTooltip.DEFAULT).setForegroundColor(getLAFOptions().getTooltipForegroundColor());

                    SyntheticaHelper.init(laf);

                } catch (Throwable e) {
                    LogController.CL().log(e);
                }
            } else {
                /* init for all other laf */
                UIManager.setLookAndFeel(laf);
            }

        } catch (Throwable e) {
            LogController.CL().log(e);
        } finally {
            LogController.GL.info("LAF init: " + (System.currentTimeMillis() - t));
        }
    }

    public LAFOptions getLAFOptions() {
        if (lafOptions != null) return lafOptions;
        synchronized (this) {
            if (lafOptions != null) return lafOptions;
            String str = null;
            try {
                if (laf != null) str = NewTheme.I().getText("laf/" + laf + "/options.json");
            } catch (final Throwable e) {
                LogController.CL().log(e);
            }
            if (str != null) {
                lafOptions = JSonStorage.restoreFromString(str, new TypeRef<LAFOptions>() {
                }, new LAFOptions());
            } else {
                LogController.GL.info("Not LAF Options found: " + laf + ".json");
                lafOptions = new LAFOptions();
            }
        }
        return lafOptions;
    }

    @Override
    public void init() {
        setUIManager();
    }
}
