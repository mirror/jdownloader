package jd.gui.skins.simple;

import jd.config.SubConfiguration;

public class SimpleGuiConstants {

    public static final String PARAM_CUSTOM_BROWSER_PARAM = "PARAM_CUSTOM_ROWSER_PARAM";
    public static final String PARAM_CUSTOM_BROWSER = "CUSTOM_ROWSER";
    public static final String PARAM_CUSTOM_BROWSER_USE = "PARAM_CUSTOM_ROWSER_USE";
//    public static final String PARAM_NUM_PREMIUM_CONFIG_FIELDS = "PARAM_NUM_PREMIUM_CONFIG_FIELDS";
    public static final String PARAM_SHOW_SPEEDMETER = "PARAM_SHOW_SPEEDMETER";
    public static final String PARAM_SHOW_SPEEDMETER_WINDOWSIZE = "PARAM_SHOW_SPEEDMETER_WINDOWSIZE";
    public static final String SELECTED_CONFIG_TAB = "SELECTED_CONFIG_TAB";
    public static final String PARAM_INPUTTIMEOUT = "PARAM_INPUTTIMEOUT";

    public static final String PARAM_THEME = "THEME";
    public static final String PARAM_START_DOWNLOADS_AFTER_START = "START_DOWNLOADS_AFTER_START";
    public static final String PARAM_SHOW_SPLASH = "SHOW_SPLASH";
    public static final String PARAM_JAC_LOG = "JAC_DOLOG";
    public static final String PARAM_DISABLE_CONFIRM_DIALOGS = "DISABLE_CONFIRM_DIALOGS";
    public static final String PARAM_BROWSER_VARS = "BROWSER_VARS";
    public static final String PARAM_BROWSER = "BROWSER";
    public transient static SubConfiguration GUI_CONFIG = null;
    public static final String GUICONFIGNAME = "simpleGUI";
    public static final String PARAM_SIDEBAR_COLLAPSED = "PARAM_SIDEBAR_COLLAPSED";
    public static final String ANIMATION_ENABLED = "ANIMATION_ENABLED";
    public static final String DECORATION_ENABLED = "DECORATION_ENABLED";
    public static final String PARAM_INSERT_NEW_LINKS_AT = "PARAM_INSERT_NEW_LINKS_AT2";
    public static final int TOP = 1<<0;
    public static final int BOTTOM = 1<<1;
    public static final String PARAM_START_AFTER_ADDING_LINKS = "PARAM_START_AFTER_ADDING_LINKS";
  
    
    public static boolean isAnimated() {
        // TODO Auto-generated method stub
        return SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getBooleanProperty(SimpleGuiConstants.ANIMATION_ENABLED,true);
    }

}
