package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface LAFSettings extends ConfigInterface {

    // if (StringUtils.isNotEmpty(c)) lafOptions.setDownloadOverviewHeaderColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForDownloadOverviewHeader();

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
    String getColorForFilteredTableView();

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
    String getColorForPanelHeader();

    // if (StringUtils.isNotEmpty(c)) lafOptions.setPanelHeaderForegroundColor(c);
    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForPanelHeaderForeground();

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
    String getColorForSortedColumnView();

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

    void setColorForDownloadOverviewHeader(String colorHex);

    public void setColorForErrorForeground(String errorForeground);

    void setColorForFilteredTableView(String colorHex);

    void setColorForPanelBackground(String colorHex);

    void setColorForPanelHeader(String colorHex);

    void setColorForPanelHeaderForeground(String colorHex);

    void setColorForPanelHeaderLine(String colorHex);

    void setColorForSortedColumnView(String colorHex);

    public void setColorForTablePackageRowBackground(String tablePackageRowBackground);

    public void setColorForTablePackageRowForeground(String tablePackageRowForeground);

    public void setColorForTooltipForeground(String colorHex);

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForSelectedRowsForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForSelectedRowsBackground();

    void setColorForSelectedRowsForeground(String color);

    void setColorForSelectedRowsBackground(String color);

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForMouseOverRowForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForMouseOverRowBackground();

    void setColorForMouseOverRowForeground(String color);

    void setColorForMouseOverRowBackground(String color);

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForAlternateRowForeground();

    @DescriptionForConfigEntry("Customized Color in aRGB Format (Pure red: #ffFF0000)")
    @AboutConfig
    @RequiresRestart
    @HexColorString
    String getColorForAlternateRowBackground();

    void setColorForAlternateRowForeground(String color);

    void setColorForAlternateRowBackground(String color);

}
