package jd;

import java.awt.Component;

import org.appwork.javaexe.JavaExe_I_TaskbarManagement;
import org.jdownloader.logging.LogController;
import org.jdownloader.logging.LogSource;

public class Main_TaskbarManagement implements JavaExe_I_TaskbarManagement {

    private static final LogSource LOGGER            = LogController.getInstance().getLogger("WindowsTray");
    static {
        LOGGER.setInstantFlush(true);
    }

    static final int               ID_MENU_OPEN      = 1;
    static final int               ID_MENU_OPTION    = 2;
    static final int               ID_MENU_QUIT      = 3;
    static final int               ID_MENU_OPT_PREF  = 4;
    static final int               ID_MENU_OPT_MENU  = 5;

    static final String            LBL_MENU_OPEN     = "Open";
    static final String            LBL_MENU_OPTION   = "Options";
    static final String            LBL_MENU_QUIT     = "Quit";
    static final String            LBL_MENU_OPT_PREF = "Preferences...";
    static final String            LBL_MENU_OPT_MENU = "Menu...";

    static int                     defaultAction     = ID_MENU_OPEN;

    /*******************************************/
    public static String[][] taskGetMenu(boolean isRightClick, int menuID) {
        if (!isRightClick) return null;

        if (menuID <= 0) return new String[][] { { "" + ID_MENU_OPEN, LBL_MENU_OPEN, "", "" }, { "" + ID_MENU_OPTION, LBL_MENU_OPTION, "", "" }, { "", "", "" + MFT_SEPARATOR, "" }, { "" + ID_MENU_QUIT, LBL_MENU_QUIT, "", "" }, };

        switch (menuID) {
        case ID_MENU_OPTION:
            return new String[][] { { "" + ID_MENU_OPT_PREF, LBL_MENU_OPT_PREF, "", "" }, { "" + ID_MENU_OPT_MENU, LBL_MENU_OPT_MENU, "", "" }, };
        }

        return null;
    }

    /*******************************************/
    public static int taskGetDefaultMenuID(boolean isRightClick) {
        return defaultAction;
    }

    /*******************************************/
    public static boolean taskDisplayMenu(boolean isRightClick, Component parent, int x, int y) {
        LOGGER.info("taskDisplayMenu " + isRightClick + "  " + parent + " - " + x + " " + y);
        if (isRightClick) return false;

        return true;
    }

    /*******************************************/
    public static void taskDoAction(boolean isRightClick, int menuID) {
        LOGGER.info("DOACTION " + isRightClick + " - " + menuID);
        switch (menuID) {
        case ID_MENU_OPEN:

            break;

        case ID_MENU_OPT_PREF:

            break;

        case ID_MENU_OPT_MENU:

            break;

        case ID_MENU_QUIT:
            System.exit(0);
        }
    }

    /*******************************************/
    public static String[] taskGetInfo() {

        LOGGER.info("gettaskinfo");
        return new String[] { "JDownloader", // Description
                "" + ACT_CLICK_MENU, // action "1 click-Right"
                "" + ACT_CLICK_NOP, // action "2 click-Right"
                "" + ACT_CLICK_MENU, // action "1 click-Left"
                "" + ACT_CLICK_OPEN // action "2 click-Left"
        };
    }

    // public static String[][] taskGetMenu (boolean isRightClick, int menuID);
    // public static int taskGetDefaultMenuID (boolean isRightClick);
    // public static void taskDoAction (boolean isRightClick, int menuID);
    // public static boolean taskDisplayMenu (boolean isRightClick, Component parent, int x, int y);
    //
    // public static String[] taskGetInfo ();
    public static boolean taskIsShow() {
        LOGGER.info("taskIsShow");
        return true;
    }

    public static void taskInit() {
        LOGGER.info("taskInit");

    }

    public static void taskDoBalloonAction() {
        LOGGER.info("taskDoBalloonAction");
    }

    public static boolean taskIsBalloonShow() {
        LOGGER.info("taskIsBalloonShow");
        return true;
    }

    public static void taskSetBalloonSupported(boolean isSupported) {
        System.out.println("Is supported: " + isSupported);

        LOGGER.info("taskSetBalloonSupported " + isSupported);
    }

    public static String[] taskGetBalloonInfo() {
        LOGGER.info("taskGetBalloonInfo");
        // NIIF_NONE = neutral message.
        // NIIF_INFO = information message.
        // NIIF_WARNING = warning message.
        // NIIF_ERROR = error message.
        // NIIF_USER = message with the application’s icon.
        return new String[] { "Title", "Message\r\nnewline " + System.currentTimeMillis(), "NIIF_WARNING", "20" };
    }
    // public static void taskDataFromService (Serializable data);
    // public static boolean taskIsDataForService ();
    // public static Serializable taskDataForService ();
}
