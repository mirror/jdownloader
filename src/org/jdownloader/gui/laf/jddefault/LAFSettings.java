package org.jdownloader.gui.laf.jddefault;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntArrayValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.jdownloader.settings.HexColorString;

public interface LAFSettings extends ConfigInterface {
    public static final String DE_JAVASOFT_PLAF_SYNTHETICA_SIMPLE2D_MENU_PAINTER = "de.javasoft.plaf.synthetica.simple2D.MenuPainter";
    public static final String ALICE_BLUE_APPROX                                 = "ffF5FCFF";

    public static final String ALPHA_CC_TROPICAL_BLUE_APPROX                     = "ccCAE8FA";
    public static final String BLACK                                             = "FF000000";
    public static final String GRAY                                              = "ffC0C0C0";
    public static final String GREEN                                             = "FF00FF00";
    public static final String JAGGED_ICE_APPROX                                 = "ffD7E7F0";
    public static final String MYSTIC_APPROX                                     = "FFDEE7ED";
    public static final String ORANGE                                            = "ffFFC800";
    public static final String PIGEON_POST_APPROX                                = "ffABC7D8";
    public static final String RED                                               = "FFFF0000";
    public static final String TROPICAL_BLUE_APPROX                              = "ffCAE8FA";

    // private String colorForDownloadOverviewHeader = null;
    // private String colorForErrorForeground = "FFFF0000";
    //
    // private String colorForSortedColumnView = toHex(Color.ORANGE);
    //
    // private String colorForFilteredTableView = toHex(Color.GREEN);
    //
    // private String menuBackgroundPainterClass = "de.javasoft.plaf.synthetica.simple2D.MenuPainter";
    //
    // private boolean paintStatusbarTopBorder = false;
    //
    // private String colorForPanelBackground = "ffF5FCFF";
    //
    // private String colorForPanelHeader = "ffD7E7F0";
    //
    // private String colorForPanelHeaderForeground = BLACK;
    // private String colorForPanelHeaderLine = "ffC0C0C0";
    //
    // private int[] popupBorderInsets = new int[] { 0, 2, 4, 2 };
    //
    // private String colorForTablePackageRowBackground = "FFDEE7ED";
    // private String colorForTablePackageRowForeground = BLACK;
    // private String colorForTooltipForeground = "ffF5FCFF";
    // private String colorForSelectedRowsForeground = BLACK;
    // private String colorForSelectedRowsBackground = "ffCAE8FA";
    // private String colorForMouseOverRowBackground = "ccCAE8FA";
    // private String colorForMouseOverRowForeground = null;
    // private String colorForAlternateRowForeground = null;
    // private String colorForAlternateRowBackground = "03000000";
    // private String colorForScrollbarsNormalState = "ffD7E7F0";
    // private String colorForScrollbarsMouseOverState = "ffABC7D8";
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue(RED)
    public String getColorForErrorForeground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setPanelBackgroundColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue(ALICE_BLUE_APPROX)
    String getColorForPanelBackground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setPanelHeaderColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue(JAGGED_ICE_APPROX)
    String getColorForPanelHeaderBackground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setPanelHeaderLineColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue(GRAY)
    String getColorForPanelHeaderLine();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue(PIGEON_POST_APPROX)
    String getColorForScrollbarsMouseOverState();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue(JAGGED_ICE_APPROX)
    String getColorForScrollbarsNormalState();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue("06000000")
    String getColorForTableAlternateRowBackground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForTableAlternateRowForeground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setHighlightColor2(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue(GREEN)
    String getColorForTableFilteredView();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue("ffC9E0ED")
    String getColorForTableMouseOverRowBackground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForTableMouseOverRowForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue(MYSTIC_APPROX)
    public String getColorForTablePackageRowBackground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    public String getColorForTablePackageRowForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue(TROPICAL_BLUE_APPROX)
    String getColorForTableSelectedRowsBackground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForTableSelectedRowsForeground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setHighlightColor1(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue(ORANGE)
    String getColorForTableSortedColumnView();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setTooltipForegroundColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue(ALICE_BLUE_APPROX)
    public String getColorForTooltipForeground();

    public void setColorForErrorForeground(String errorForeground);

    void setColorForPanelBackground(String colorHex);

    void setColorForPanelHeaderBackground(String colorHex);

    void setColorForPanelHeaderLine(String colorHex);

    void setColorForScrollbarsMouseOverState(String color);

    void setColorForScrollbarsNormalState(String color);

    void setColorForTableAlternateRowBackground(String color);

    void setColorForTableAlternateRowForeground(String color);

    void setColorForTableFilteredView(String colorHex);

    void setColorForTableMouseOverRowBackground(String color);

    void setColorForTableMouseOverRowForeground(String color);

    public void setColorForTablePackageRowBackground(String tablePackageRowBackground);

    public void setColorForTablePackageRowForeground(String tablePackageRowForeground);

    void setColorForTableSelectedRowsBackground(String color);

    void setColorForTableSelectedRowsForeground(String color);

    void setColorForTableSortedColumnView(String colorHex);

    public void setColorForTooltipForeground(String colorHex);

    public abstract void setPopupBorderInsets(int[] popupBorderInsets);

    public abstract void setPaintStatusbarTopBorder(boolean paintStatusbarTopBorder);

    public abstract void setMenuBackgroundPainterClass(String menuBackgroundPainterClass);

    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(false)
    public abstract boolean isPaintStatusbarTopBorder();

    @AboutConfig
    @DefaultIntArrayValue({ 0, 2, 4, 2 })
    public abstract int[] getPopupBorderInsets();

    @DescriptionForConfigEntry("This Painter class is used to paint Main Menu Items without a popdown menu")
    @AboutConfig
    @RequiresRestart
    @DefaultStringValue(DE_JAVASOFT_PLAF_SYNTHETICA_SIMPLE2D_MENU_PAINTER)
    public abstract String getMenuBackgroundPainterClass();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    @DefaultStringValue(BLACK)
    public String getColorForPanelHeaderForeground();

    void setColorForPanelHeaderForeground(String colorHex);
}
