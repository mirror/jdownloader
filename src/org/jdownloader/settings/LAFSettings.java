package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface LAFSettings extends ConfigInterface {

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    public String getColorForErrorForeground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setHighlightColor2(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForTableFilteredView();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setPanelBackgroundColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForPanelBackground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setPanelHeaderColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForPanelHeaderBackground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setPanelHeaderLineColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForPanelHeaderLine();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setHighlightColor1(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForTableSortedColumnView();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    public String getColorForTablePackageRowBackground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    public String getColorForTablePackageRowForeground();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setTooltipForegroundColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    public String getColorForTooltipForeground();

    public void setColorForErrorForeground(String errorForeground);

    void setColorForTableFilteredView(String colorHex);

    void setColorForPanelBackground(String colorHex);

    void setColorForPanelHeaderBackground(String colorHex);

    void setColorForPanelHeaderLine(String colorHex);

    void setColorForTableSortedColumnView(String colorHex);

    public void setColorForTablePackageRowBackground(String tablePackageRowBackground);

    public void setColorForTablePackageRowForeground(String tablePackageRowForeground);

    public void setColorForTooltipForeground(String colorHex);

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForTableSelectedRowsForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForTableSelectedRowsBackground();

    void setColorForTableSelectedRowsForeground(String color);

    void setColorForTableSelectedRowsBackground(String color);

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForTableMouseOverRowForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForTableMouseOverRowBackground();

    void setColorForTableMouseOverRowForeground(String color);

    void setColorForTableMouseOverRowBackground(String color);

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForTableAlternateRowForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForTableAlternateRowBackground();

    void setColorForTableAlternateRowForeground(String color);

    void setColorForTableAlternateRowBackground(String color);

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForScrollbarsNormalState();

    void setColorForScrollbarsNormalState(String color);

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForScrollbarsMouseOverState();

    void setColorForScrollbarsMouseOverState(String color);
}
