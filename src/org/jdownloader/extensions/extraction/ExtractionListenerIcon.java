package org.jdownloader.extensions.extraction;

import jd.gui.swing.jdgui.JDGui;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.extraction.gui.ExtractorProgress;

public class ExtractionListenerIcon implements ExtractionListener {

    private String            cacheQueue     = "";
    private String            cacheJobName   = "";
    private String            cacheJobStatus = "No Job active";
    private ExtractorProgress icon;

    public ExtractionListenerIcon(ExtractionExtension extractionExtension) {
        icon = new ExtractorProgress(extractionExtension);
        // JDGui.getInstance().getStatusBar().setLayout(new MigLayout("ins 0",
        // "[fill,grow,left][][][]3", "[22!]"));
        JDGui.getInstance().getStatusBar().addProcessIndicator(icon);
    }

    public void onExtractionEvent(final ExtractionEvent event) {
        final ExtractionController con = event.getCaller();

        new EDTRunner() {
            @Override
            protected void runInEDT() {
                icon.update(event.getType(), con);
            }
        };
    }

    public void cleanup() {
        JDGui.getInstance().getStatusBar().removeProcessIndicator(icon);
    }
}