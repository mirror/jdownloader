package org.jdownloader.gui.laf.plain;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.updatev2.gui.DefaultLookAndFeelExtension;
import org.jdownloader.updatev2.gui.LAFOptions;

public class PlainLookAndFeelExtension extends DefaultLookAndFeelExtension {

    @Override
    public void customizeHeaderScrollPane(JComponent headerScrollPane) {
        headerScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, LAFOptions.getInstance().getColorForPanelBorders()));

    }

    public void customizePaintHeaderScrollPaneBorder(JComponent pane, Graphics g) {
        // do nothing
    }

    @Override
    public void customizeLinkgrabberSidebarHeader(JLabel lbl, JComponent linkGrabberSideBarHeader) {
        SwingUtils.toBold(lbl);
    }

    @Override
    public JComponent customizeLayoutWrapTitledPanels(JComponent c) {
        JScrollPane pane = (JScrollPane) c;
        MigPanel wrapper = new MigPanel("ins 1", "[grow,fill]", "[grow,fill]");
        wrapper.setOpaque(false);
        wrapper.setBackground(null);
        wrapper.add(pane);

        if (pane instanceof org.jdownloader.gui.components.OverviewHeaderScrollPane) {
            Component view = pane.getViewport().getView();
            if (!"wrapper".equals(view.getName())) {

                MigPanel viewWrapper = new MigPanel("ins 2", "[grow,fill]", "[grow,fill]");
                viewWrapper.setName("wrapper");
                viewWrapper.setOpaque(false);
                viewWrapper.setBackground(null);
                viewWrapper.add(view);
                pane.setViewportView(viewWrapper);
            }
        }
        return wrapper;
    }

    @Override
    public void customizeBoundsForBottombarPopupButton(Rectangle bounds) {
        bounds.x -= 3;
        bounds.width += 2;
    }

    @Override
    public int customizeLayoutGetDefaultGap() {
        // focus border gap replaces gap
        return 0;
    }

    @Override
    public void customizeLinksTable(JComponent table, JScrollPane tableScrollPane) {
        // tableScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, LAFOptions.getInstance().getColorForPanelBorders()));
    }

    @Override
    public int customizeMenuItemIconTextGap() {
        return super.customizeMenuItemIconTextGap();
    }

    @Override
    public String customizeOverviewPanelInsets() {
        return "2 2 2 2";
    }

    @Override
    public String customizePanelHeaderInsets() {
        return "0 0 1 3";
    }

    @Override
    public String getColorForConfigHeaderTextColor() {
        return "ff4080f3";
    }

    @Override
    public String getColorForPanelBorders() {
        return "ffCCCCCC";
    }

    @Override
    public String getColorForPanelHeaderBackground() {
        return "ff8caeca";
    }

    @Override
    public String getColorForTooltipForeground() {
        return "ff8caeca";
    }

    @Override
    public int getCustomTableRowHeight() {
        return 26;
    }

    @Override
    public String getIconSetID() {
        return "flat";
    }

}
