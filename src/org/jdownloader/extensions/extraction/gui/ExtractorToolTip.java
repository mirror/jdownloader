package org.jdownloader.extensions.extraction.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JLabel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.appwork.swing.components.tooltips.PanelToolTip;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
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

        super(new TooltipPanel("ins 0,wrap 1", "[grow,fill]", "[grow,fill]"));
        this.extractionExtension = extractionExtension;
        color = (LAFOptions.getInstance().getColorForTooltipForeground());
        empty = new JLabel(T._.tooltip_empty());
        empty.setForeground(color);
        panel.add(empty, "hidemode 3");
        table = new ExtractionJobTable(model = new ExtractionJobTableModel((LAFOptions.getInstance().getColorForTooltipForeground())));
        model.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                // if (AccountTooltip.this.owner != null) AccountTooltip.this.owner.redraw();
                table.getTableHeader().repaint();
            }
        });
        table.getTableHeader().setOpaque(false);
        JLabel label = new JLabel(_GUI._.ExtractorToolTip_ExtractorToolTip_title(), NewTheme.I().getIcon(IconKey.ICON_EXTRACT, 20), JLabel.LEFT);
        SwingUtils.toBold(label);
        label.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());
        panel.add(label, "gapleft 5");
        panel.add(table.getTableHeader(), "hidemode 3");
        panel.add(table, "hidemode 3");
    }

    public void update() {

        if (extractionExtension.getJobQueue().getJobs().size() > 0) {
            // ihh
            model.clear();
            ExtractionController[] list = extractionExtension.getJobQueue().getJobs().toArray(new ExtractionController[] {});
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
