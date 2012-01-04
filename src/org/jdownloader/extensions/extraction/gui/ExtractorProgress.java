package org.jdownloader.extensions.extraction.gui;

import java.awt.Point;

import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionEvent.Type;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ExtractorProgress extends IconedProcessIndicator {
    private ExtractorToolTip tooltip;

    public ExtractorProgress(ExtractionExtension extractionExtension) {
        super(NewTheme.I().getIcon("archive", 16));
        setEnabled(false);

        setTitle(_GUI._.StatusBarImpl_initGUI_extract());

        // IconedProcessIndicator comp = new IconedProcessIndicator(32);
        // comp.valuePainter = valuePainter;
        // comp.nonValuePainter = nonValuePainter;
        // comp.activeValuePainter = activeValuePainter;
        // comp.activeNonValuePainter = activeNonValuePainter;
        // comp.setActive(isActive());
        // comp.setEnabled(isEnabled());
        // comp.setIndeterminate(isIndeterminate());
        // comp.setPreferredSize(new Dimension(32, 32));
        tooltip = new ExtractorToolTip(extractionExtension);
    }

    @Override
    public boolean isTooltipDisabledUntilNextRefocus() {

        return false;
    }

    public ExtTooltip createExtTooltip(final Point mousePosition) {
        tooltip.update();
        return tooltip;

    }

    public void update(Type type, ExtractionController con) {
        System.out.println(type);
        switch (type) {
        case EXTRACTING:
            setValue((int) ((100 * con.getArchiv().getExtracted()) / con.getArchiv().getSize()));
            break;
        case START:
            setIndeterminate(false);
            setValue(10);

        case QUEUED:
            if (con.getExtractionQueue().size() > 0) {
                if (!isEnabled()) {

                    setEnabled(true);
                }
            }
            break;
        case CLEANUP:
            setIndeterminate(true);
            setValue(0);
            if (con.getExtractionQueue().size() <= 1) {
                /*
                 * <=1 because current element is still running at this point
                 */
                if (isEnabled()) {
                    setIndeterminate(false);
                    setEnabled(false);
                }
            }
            break;
        }
        if (tooltip.getParent() != null) {
            // is visible
            tooltip.update();
        }

    }
}
