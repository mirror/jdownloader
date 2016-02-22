package org.jdownloader.updatev2.gui;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.handler.StorageHandler;

public abstract class LookAndFeelExtension implements LAFSettings {

    public abstract void customizeHeaderScrollPane(JComponent headerScrollPane);

    public abstract void customizeLinksTable(JComponent table, JScrollPane tableScrollPane);

    @Override
    public void setSpeedmeterAntiAliasingEnabled(boolean b) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setPaintStatusbarTopBorder(boolean paintStatusbarTopBorder) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForConfigHeaderTextColor(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForConfigPanelDescriptionText(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForErrorForeground(String errorForeground) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForLinkgrabberDupeHighlighter(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForPanelBackground(String colorHex) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForPanelBorders(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForPanelHeaderBackground(String colorHex) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForPanelHeaderForeground(String colorHex) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForProgressbarForeground1(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForProgressbarForeground2(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForProgressbarForeground3(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForProgressbarForeground4(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForProgressbarForeground5(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForScrollbarsMouseOverState(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForScrollbarsNormalState(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForSpeedMeterAverage(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForSpeedMeterAverageText(String colorHex) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForSpeedmeterCurrentBottom(String colorHex) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForSpeedmeterCurrentTop(String colorHex) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForSpeedmeterLimiterBottom(String colorHex) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForSpeedmeterLimiterTop(String colorHex) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForSpeedMeterText(String colorHex) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableAccountErrorRowBackground(String tableAccountErrorRowBackground) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableAccountErrorRowForeground(String tableAccountErrorRowForeground) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableAccountTempErrorRowBackground(String tableAccountTempErrorRowBackground) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableAccountTempErrorRowForeground(String tableAccountTempErrorRowForeground) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableAlternateRowBackground(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableAlternateRowForeground(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableFilteredView(String colorHex) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableMouseOverRowBackground(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableMouseOverRowForeground(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTablePackageRowBackground(String tablePackageRowBackground) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTablePackageRowForeground(String tablePackageRowForeground) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableRowGap(String str) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableSelectedRowsBackground(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableSelectedRowsForeground(String color) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTableSortedColumnView(String colorHex) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setColorForTooltipForeground(String colorHex) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setConfigLabelDisabledTextColor(String str) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setConfigLabelEnabledTextColor(String str) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setCustomTableRowHeight(int height) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setTableAlternateRowHighlightEnabled(boolean b) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setIconSetID(String themeID) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setProgressColumnTextPosition(HorizontalPostion p) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setProgressColumnFormatAddPercentEnabled(boolean b) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setProgressColumnFractionDigits(int digits) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setConfigPanelLeftIndent(int i) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public StorageHandler<?> _getStorageHandler() {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setLinkTableHorizontalRowLineWeight(int i) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setAnimationEnabled(boolean b) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setFontName(String name) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setFontRespectsSystemDPI(boolean b) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setFontScaleFactor(int b) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setLanguage(String lng) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setTextAntiAliasEnabled(boolean b) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setWindowDecorationEnabled(boolean b) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    @Override
    public void setWindowOpaque(boolean b) {
        throw new WTFException("Not Implemented. Do not use this method");
    }

    public abstract String customizeOverviewPanelInsets();

    public abstract String customizePanelHeaderInsets();

    public abstract int customizeMenuItemIconTextGap();

    public abstract int customizeMenuItemIndentForToggleItems();

    public abstract Insets customizePopupBorderInsets();

    public abstract int customizeLayoutGetDefaultGap();

    public abstract void customizeBoundsForBottombarPopupButton(Rectangle bounds);

    public abstract JComponent customizeLayoutWrapTitledPanels(JComponent pane);

    public abstract void customizeLinkgrabberSidebarHeader(JLabel lbl, JComponent linkGrabberSideBarHeader);

    public abstract void customizePaintHeaderScrollPaneBorder(JComponent headerScrollPane, Graphics g);

    public abstract void customizeMenuBar(JMenuBar menubar);

    public abstract void customizeToolbar(JToolBar toolbar);

    public abstract void customizeMainTabbedPane(JTabbedPane tabbedPane);

    public abstract String customizeLinkPropertiesPanelLayout();

    public abstract void customizeLinkPropertiesPanel(JPanel downloadPropertiesBasePanel);

}
