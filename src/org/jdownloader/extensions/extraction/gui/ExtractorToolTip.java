package org.jdownloader.extensions.extraction.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JLabel;


import org.appwork.swing.components.tooltips.PanelToolTip;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.updatev2.gui.LAFOptions;

public class ExtractorToolTip extends PanelToolTip {
    /**
	 * 
	 */
    private static final long       serialVersionUID = 2863662451264465139L;
    private Color                   color;
    private JLabel                  empty;
    private ExtractionJobTable      table;
    private ExtractionExtension     extractionExtension;
    private ExtractionJobTableModel model;

    public ExtractorToolTip(ExtractionExtension extractionExtension) {

        super(new TooltipPanel("ins 0,wrap 2", "[grow,fill]", "[grow,fill]"));
        this.extractionExtension = extractionExtension;
        color = (LAFOptions.getInstance().getColorForTooltipForeground());
        empty = new JLabel(T._.tooltip_empty());
        empty.setForeground(color);
        panel.add(empty, "hidemode 3");
        table = new ExtractionJobTable(model = new ExtractionJobTableModel((LAFOptions.getInstance().getColorForTooltipForeground())));

        panel.add(table, "hidemode 3");
    }

    public void update() {

        if (extractionExtension.getJobQueue().size() > 0) {
            // ihh
            model.clear();
            ExtractionController[] list = extractionExtension.getJobQueue().getEntries().toArray(new ExtractionController[] {});
            model.addAllElements(list);
            // estimate required widths
            int width = 0;
            FontMetrics fm = getFontMetrics(new Font("Arial", Font.PLAIN, 12));
            for (ExtractionController ec : list) {
                String n = ec.getArchiv().getName();
                width = Math.max(width, fm.stringWidth(n));
            }

            table.setPreferredSize(new Dimension(width + 110 + 150, list.length * 22));
            table.setVisible(true);
            empty.setVisible(false);
            panel.repaint();
        } else {
            table.setVisible(false);
            empty.setVisible(true);
        }
    }
}
