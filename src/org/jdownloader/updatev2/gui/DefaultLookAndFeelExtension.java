package org.jdownloader.updatev2.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;

import org.jdownloader.gui.translate._GUI;

public class DefaultLookAndFeelExtension extends LookAndFeelExtension {
    public static final String ALICE_BLUE_APPROX                                 = "ffF5FCFF";
    public static final String ALPHA_CC_TROPICAL_BLUE_APPROX                     = "ccCAE8FA";

    public static final String BLACK                                             = "FF000000";
    public static final String DE_JAVASOFT_PLAF_SYNTHETICA_SIMPLE2D_MENU_PAINTER = "de.javasoft.plaf.synthetica.simple2D.MenuPainter";
    public static final String GRAY                                              = "ffC0C0C0";
    public static final String GREEN                                             = "FF00FF00";
    public static final String JAGGED_ICE_APPROX                                 = "ffD7E7F0";
    public static final String MYSTIC_APPROX                                     = "FFDEE7ED";
    public static final String ORANGE                                            = "ffFFC800";
    public static final String PIGEON_POST_APPROX                                = "ffABC7D8";
    public static final String RED                                               = "FFFF0000";
    public static final String TROPICAL_BLUE_APPROX                              = "ffCAE8FA";

    public DefaultLookAndFeelExtension() {

    }

    @Override
    public void customizeBoundsForBottombarPopupButton(Rectangle bounds) {
        bounds.x -= 3;
        bounds.width += 2;
    }

    @Override
    public void customizeHeaderScrollPane(JComponent headerScrollPane) {
    }

    @Override
    public int customizeLayoutGetDefaultGap() {
        return 2;
    }

    @Override
    public JComponent customizeLayoutWrapTitledPanels(JComponent c) {

        return c;
    }

    @Override
    public void customizeLinkgrabberSidebarHeader(JLabel lbl, JComponent linkGrabberSideBarHeader) {

    }

    @Override
    public void customizeLinksTable(JComponent c, JScrollPane tableScrollPane) {
        tableScrollPane.setBorder(null);
        // LAFSettings.class
    }

    @Override
    public int customizeMenuItemIconTextGap() {
        return 7;
    }

    @Override
    public int customizeMenuItemIndentForToggleItems() {
        return 26;
    }

    @Override
    public String customizeOverviewPanelInsets() {
        return "0 0 1 0";
    }

    @Override
    public void customizePaintHeaderScrollPaneBorder(JComponent c, Graphics g) {
        // do NOT use HeaderScrollPane casting here!. java compiler will pre-optimize the code to require HeaderScrollPane to load this
        // class
        final JScrollPane scrollPane = (JScrollPane) c;
        final Color headerColor = (LAFOptions.getInstance().getColorForPanelHeaderBackground());
        final Color headerlineColor = (LAFOptions.getInstance().getColorForPanelBorders());
        if (scrollPane.getColumnHeader() != null) {
            // here it is safe because java compiler cannot pre-optimize the if branch, because HeaderScrollPane is only required within
            // this statement
            g.setColor(headerColor);
            final int in = scrollPane.getBorder().getBorderInsets(scrollPane).top;
            final int headerHeight = ((org.jdownloader.gui.views.components.HeaderScrollPane) scrollPane).getHeaderHeight();
            g.fillRect(1, 1, scrollPane.getWidth() - 2, headerHeight + in - 1);
            g.setColor(headerlineColor);
            g.drawLine(1, headerHeight + in - 1, scrollPane.getWidth() - 2, headerHeight + in - 1);
        }
    }

    @Override
    public String customizePanelHeaderInsets() {
        return "0 0 1 0";
    }

    @Override
    public Insets customizePopupBorderInsets() {
        return new Insets(0, 2, 4, 2);
    }

    @Override
    public String getColorForConfigHeaderTextColor() {
        return "FF202020";
    }

    @Override
    public String getColorForConfigPanelDescriptionText() {
        return "FF808080";
    }

    @Override
    public String getColorForErrorForeground() {
        return RED;
    }

    @Override
    public String getColorForLinkgrabberDupeHighlighter() {
        return "33FF0000";
    }

    @Override
    public String getColorForPanelBackground() {
        return ALICE_BLUE_APPROX;
    }

    @Override
    public String getColorForPanelBorders() {
        return GRAY;
    }

    @Override
    public String getColorForPanelHeaderBackground() {
        return JAGGED_ICE_APPROX;
    }

    @Override
    public String getColorForPanelHeaderForeground() {
        return BLACK;
    }

    @Override
    public String getColorForProgressbarForeground1() {
        return "5F70CCFF";
    }

    @Override
    public String getColorForProgressbarForeground2() {
        return "5F80C7F7";
    }

    @Override
    public String getColorForProgressbarForeground3() {
        return "8078C0EF";
    }

    @Override
    public String getColorForProgressbarForeground4() {
        return "5F80C7F7";
    }

    @Override
    public String getColorForProgressbarForeground5() {
        return "5F70CCFF";
    }

    @Override
    public String getColorForScrollbarsMouseOverState() {
        return PIGEON_POST_APPROX;
    }

    @Override
    public String getColorForScrollbarsNormalState() {
        return JAGGED_ICE_APPROX;
    }

    @Override
    public String getColorForSpeedMeterAverage() {
        return "FF359E35";
    }

    @Override
    public String getColorForSpeedMeterAverageText() {
        return "FF222222";
    }

    @Override
    public String getColorForSpeedmeterCurrentBottom() {
        return "CC3DC83D";
    }

    @Override
    public String getColorForSpeedmeterCurrentTop() {
        return "2051F251";
    }

    @Override
    public String getColorForSpeedmeterLimiterBottom() {
        return "00FF0000";
    }

    @Override
    public String getColorForSpeedmeterLimiterTop() {
        return "ccFF0000";
    }

    @Override
    public String getColorForSpeedMeterText() {
        return "FF222222";
    }

    @Override
    public String getColorForTableAccountErrorRowBackground() {
        return "7FFF0000";
    }

    @Override
    public String getColorForTableAccountErrorRowForeground() {
        return "FF000000";
    }

    @Override
    public String getColorForTableAccountTempErrorRowBackground() {
        return "7FFFC800";
    }

    @Override
    public String getColorForTableAccountTempErrorRowForeground() {
        return "FF000000";
    }

    @Override
    public String getColorForTableAlternateRowBackground() {
        return "06000000";
    }

    @Override
    public String getColorForTableAlternateRowForeground() {
        return null;
    }

    @Override
    public String getColorForTableFilteredView() {
        return GREEN;
    }

    @Override
    public String getColorForTableMouseOverRowBackground() {
        return "ffC9E0ED";
    }

    @Override
    public String getColorForTableMouseOverRowForeground() {
        return null;
    }

    @Override
    public String getColorForTablePackageRowBackground() {
        return MYSTIC_APPROX;
    }

    @Override
    public String getColorForTablePackageRowForeground() {
        return null;
    }

    @Override
    public String getColorForTableRowGap() {
        return null;
    }

    @Override
    public String getColorForTableSelectedRowsBackground() {
        return TROPICAL_BLUE_APPROX;
    }

    @Override
    public String getColorForTableSelectedRowsForeground() {
        return null;
    }

    @Override
    public String getColorForTableSortedColumnView() {
        return ORANGE;
    }

    @Override
    public String getColorForTooltipForeground() {
        return ALICE_BLUE_APPROX;
    }

    @Override
    public String getConfigLabelDisabledTextColor() {
        return "FFA0A0A0";
    }

    @Override
    public String getConfigLabelEnabledTextColor() {
        return "FF202020";
    }

    @Override
    public int getConfigPanelLeftIndent() {
        return 39;
    }

    @Override
    public int getCustomTableRowHeight() {
        return 0;
    }

    @Override
    public String getFontName() {
        return "default";
    }

    @Override
    public int getFontScaleFactor() {
        return 100;
    }

    @Override
    public String getIconSetID() {
        return "standard";
    }

    @Override
    public String getLanguage() {
        return null;
    }

    @Override
    public int getLinkTableHorizontalRowLineWeight() {
        return 0;
    }

    @Override
    public int getProgressColumnFractionDigits() {
        return 2;
    }

    @Override
    public HorizontalPostion getProgressColumnTextPosition() {
        return HorizontalPostion.CENTER;
    }

    @Override
    public boolean isAnimationEnabled() {
        return true;
    }

    @Override
    public boolean isFontRespectsSystemDPI() {
        return true;
    }

    @Override
    public boolean isPaintStatusbarTopBorder() {
        return false;
    }

    @Override
    public boolean isProgressColumnFormatAddPercentEnabled() {
        return true;
    }

    @Override
    public boolean isTableAlternateRowHighlightEnabled() {
        return true;
    }

    @Override
    public boolean isTextAntiAliasEnabled() {
        return false;
    }

    @Override
    public boolean isWindowDecorationEnabled() {
        return false;
    }

    @Override
    public boolean isWindowOpaque() {
        return false;
    }

    @Override
    public void customizeMenuBar(JMenuBar menubar) {
        menubar.add(new JMenu(_GUI.T.MenuBar_loading()));
    }

    @Override
    public void customizeToolbar(JToolBar toolbar) {
        toolbar.setMinimumSize(new Dimension(36, 36));
    }

    @Override
    public boolean isSpeedmeterAntiAliasingEnabled() {
        return false;
    }

    @Override
    public void customizeMainTabbedPane(JTabbedPane tabbedPane) {
    }

    @Override
    public String customizeLinkPropertiesPanelLayout() {
        return "ins 3 0 0 0";
    }

    @Override
    public void customizeLinkPropertiesPanel(JPanel downloadPropertiesBasePanel) {
    }

}
