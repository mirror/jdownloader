package org.jdownloader.extensions.extraction.gui;

import java.awt.Color;

import javax.swing.JLabel;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.components.tooltips.PanelToolTip;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.translate.T;

public class ExtractorToolTip extends PanelToolTip {
    private Color                   color;
    private JLabel                  empty;
    private ExtractionJobTable      table;
    private ExtractionExtension     extractionExtension;
    private ExtractionJobTableModel model;

    public ExtractorToolTip(ExtractionExtension extractionExtension) {

        super(new TooltipPanel("ins 0,wrap 2", "[grow,fill]", "[grow,fill]"));
        this.extractionExtension = extractionExtension;
        color = new Color(LookAndFeelController.getInstance().getLAFOptions().getTooltipForegroundColor());
        empty = new JLabel(T._.tooltip_empty());
        empty.setForeground(color);
        panel.add(empty, "hidemode 3");
        table = new ExtractionJobTable(model = new ExtractionJobTableModel());
        panel.add(table, "hidemode 3");
    }

    public void update() {

        if (extractionExtension.getJobQueue().size() > 0) {
            // ihh
            model.clear();
            model.addAllElements(extractionExtension.getJobQueue().getEntries().toArray(new ExtractionController[] {}));
            table.setVisible(true);
            empty.setVisible(false);
        } else {
            table.setVisible(false);
            empty.setVisible(true);
        }
    }
}
