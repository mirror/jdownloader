package org.jdownloader.updatev2.gui;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.HexColorString;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.StorageHandlerFactoryAnnotation;
import org.appwork.swing.synthetica.SyntheticaSettings;

@StorageHandlerFactoryAnnotation(LAFSettingsStorageHandlerFactory.class)
public interface LAFSettings extends SyntheticaSettings {
    // Static Mappings for interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings
    public static final String ALICE_BLUE_APPROX             = "ffF5FCFF";
    public static final String ALPHA_CC_TROPICAL_BLUE_APPROX = "ccCAE8FA";
    public static final String BLACK                         = "FF000000";
    public static final String GRAY                          = "ffC0C0C0";
    public static final String GREEN                         = "FF00FF00";
    public static final String JAGGED_ICE_APPROX             = "ffD7E7F0";
    public static final String MYSTIC_APPROX                 = "FFDEE7ED";
    public static final String ORANGE                        = "ffFFC800";
    public static final String PIGEON_POST_APPROX            = "ffABC7D8";
    public static final String RED                           = "FFFF0000";
    public static final String TROPICAL_BLUE_APPROX          = "ffCAE8FA";

    @AboutConfig
    @DescriptionForConfigEntry("Paint all labels/text with or without antialias. Default value is false.")
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isSpeedmeterAntiAliasingEnabled();

    void setSpeedmeterAntiAliasingEnabled(boolean b);

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForErrorForeground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setPanelBackgroundColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    String getColorForPanelBackground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setPanelHeaderColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    String getColorForPanelHeaderBackground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForPanelHeaderForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    String getColorForScrollbarsMouseOverState();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    String getColorForScrollbarsNormalState();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForSpeedMeterAverage();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForSpeedMeterAverageText();

    void setColorForSpeedMeterAverageText(String colorHex);

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForSpeedMeterText();

    void setColorForSpeedMeterText(String colorHex);

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForSpeedmeterCurrentBottom();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForSpeedmeterCurrentTop();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForSpeedmeterLimiterBottom();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForSpeedmeterLimiterTop();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForTableAccountErrorRowBackground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForTableAccountErrorRowForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForTableAccountTempErrorRowBackground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForTableAccountTempErrorRowForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    String getColorForTableAlternateRowBackground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    String getColorForTableAlternateRowForeground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setHighlightColor2(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    String getColorForTableFilteredView();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    String getColorForTableMouseOverRowBackground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    String getColorForTableMouseOverRowForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForTablePackageRowBackground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForTablePackageRowForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    String getColorForTableSelectedRowsBackground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    String getColorForTableSelectedRowsForeground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setHighlightColor1(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    String getColorForTableSortedColumnView();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setTooltipForegroundColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForTooltipForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForPanelBorders();

    public void setColorForPanelBorders(String color);

    // @AboutConfig
    // @DefaultIntArrayValue({ 0, 2, 4, 2 })
    // public int[] getPopupBorderInsets();
    //
    // @AboutConfig
    // @DefaultIntArrayValue({ 0, 0, 1, 0 })
    // public int[] getInfoPanelHeaderInsets();
    //
    // public void setInfoPanelHeaderInsets(int[] insets);
    //
    // @AboutConfig
    // @DefaultIntArrayValue({ 0, 0, 0, 0 })
    // public int[] getInfoPanelContentInsets();
    //
    // public void setInfoPanelContentInsets(int[] insets);
    @RequiresRestart("A JDownloader Restart is Required")
    public abstract boolean isPaintStatusbarTopBorder();

    public void setColorForErrorForeground(String errorForeground);

    void setColorForPanelBackground(String colorHex);

    void setColorForPanelHeaderBackground(String colorHex);

    void setColorForPanelHeaderForeground(String colorHex);

    void setColorForScrollbarsMouseOverState(String color);

    void setColorForScrollbarsNormalState(String color);

    public void setColorForSpeedMeterAverage(String color);

    void setColorForSpeedmeterCurrentBottom(String colorHex);

    void setColorForSpeedmeterCurrentTop(String colorHex);

    void setColorForSpeedmeterLimiterBottom(String colorHex);

    void setColorForSpeedmeterLimiterTop(String colorHex);

    public void setColorForTableAccountErrorRowBackground(String tableAccountErrorRowBackground);

    public void setColorForTableAccountErrorRowForeground(String tableAccountErrorRowForeground);

    public void setColorForTableAccountTempErrorRowBackground(String tableAccountTempErrorRowBackground);

    public void setColorForTableAccountTempErrorRowForeground(String tableAccountTempErrorRowForeground);

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

    public abstract void setPaintStatusbarTopBorder(boolean paintStatusbarTopBorder);

    // public abstract void setPopupBorderInsets(int[] popupBorderInsets);
    @DescriptionForConfigEntry("Customized Color for the Progressbar 1/5 in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @HexColorString
    public String getColorForProgressbarForeground1();

    @DescriptionForConfigEntry("Customized Color for the Progressbar 2/5 in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @HexColorString
    public String getColorForProgressbarForeground2();

    @DescriptionForConfigEntry("Customized Color for the Progressbar 3/5 in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @HexColorString
    public String getColorForProgressbarForeground3();

    @DescriptionForConfigEntry("Customized Color for the Progressbar 4/5 in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @HexColorString
    public String getColorForProgressbarForeground4();

    @DescriptionForConfigEntry("Customized Color for the Progressbar 5/5 in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @HexColorString
    public String getColorForProgressbarForeground5();

    public void setColorForProgressbarForeground1(String color);

    public void setColorForProgressbarForeground2(String color);

    public void setColorForProgressbarForeground3(String color);

    public void setColorForProgressbarForeground4(String color);

    public void setColorForProgressbarForeground5(String color);

    @DescriptionForConfigEntry("Customized Color for the Linkgrabber Dupe Highlight in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @HexColorString
    @RequiresRestart("A JDownloader Restart is Required")
    public String getColorForLinkgrabberDupeHighlighter();

    public void setColorForLinkgrabberDupeHighlighter(String color);

    @DescriptionForConfigEntry("Customized Color for the Description text in the config panels in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @HexColorString
    @RequiresRestart("A JDownloader Restart is Required")
    public String getColorForConfigPanelDescriptionText();

    public void setColorForConfigPanelDescriptionText(String color);

    @DescriptionForConfigEntry("Customized Color for the Header text in the config panels in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForConfigHeaderTextColor();

    public void setColorForConfigHeaderTextColor(String color);

    @DescriptionForConfigEntry("Customized Color for the Config Panel Label text(enabled) in the config panels in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getConfigLabelEnabledTextColor();

    @DescriptionForConfigEntry("Customized Color for the Config Panel Label text(disable) in the config panels in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getConfigLabelDisabledTextColor();

    public void setConfigLabelEnabledTextColor(String str);

    public void setConfigLabelDisabledTextColor(String str);

    @DescriptionForConfigEntry("Customized Color for the Table Row gap line in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @HexColorString
    public String getColorForTableRowGap();

    public void setColorForTableRowGap(String str);

    @RequiresRestart("A JDownloader Restart is Required")
    @AboutConfig
    @DescriptionForConfigEntry("Increase this value and set ColorForTableRowGap to show a gap between two links. Check CustomTableRowHeight as well")
    int getLinkTableHorizontalRowLineWeight();

    void setLinkTableHorizontalRowLineWeight(int i);

    @AboutConfig
    @DescriptionForConfigEntry("by default, table row's height dynamicly adapts to the fontsize. Set a value>0 to set your own custom row height.")
    int getCustomTableRowHeight();

    void setCustomTableRowHeight(int height);

    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Every  odd row get's a light shadow if enabled")
    @AboutConfig
    boolean isTableAlternateRowHighlightEnabled();

    void setTableAlternateRowHighlightEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Icon Set ID. Make sure that ./themes/<ID>/ exists")
    String getIconSetID();

    void setIconSetID(String themeID);

    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Choose the text position in the progress column.")
    public HorizontalPostion getProgressColumnTextPosition();

    public void setProgressColumnTextPosition(HorizontalPostion p);

    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Show % Letter in the Progressbar if possible")
    public boolean isProgressColumnFormatAddPercentEnabled();

    public void setProgressColumnFormatAddPercentEnabled(boolean b);

    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Choose the amount of decimal letters for the Progress column. 2->12.34% 1->12.3%")
    public void setProgressColumnFractionDigits(int digits);

    public int getProgressColumnFractionDigits();

    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("The left gap/indent/inset in the config/settings panels")
    public int getConfigPanelLeftIndent();

    public void setConfigPanelLeftIndent(int i);
}
