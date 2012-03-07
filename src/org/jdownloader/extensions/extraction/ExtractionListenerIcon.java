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
        JDGui.getInstance().getStatusBar().add(icon);
    }

    public void onExtractionEvent(final ExtractionEvent event) {
        final ExtractionController con = event.getCaller();

        // switch (event.getType()) {
        // case QUEUED:
        // break;
        // case START:
        // cacheJobName = con.getArchiv().getFirstArchiveFile().getName();
        // cacheJobStatus =
        // T._.plugins_optional_extraction_status_openingarchive();
        // break;
        // case START_CRACK_PASSWORD:
        // cacheJobStatus = "Start password finding";
        // break;
        // case PASSWORT_CRACKING:
        // cacheJobStatus =
        // T._.plugins_optional_extraction_status_crackingpass() + "\n" +
        // con.getCrackProgress() / con.getPasswordListSize() * 100 + "% \t" +
        // con.getCrackProgress() + " / " + con.getPasswordListSize();
        // break;
        // case PASSWORD_FOUND:
        // cacheJobStatus = T._.plugins_optional_extraction_status_passfound();
        // break;
        // case EXTRACTING:
        // cacheJobStatus = T._.plugins_optional_extraction_status_extracting()
        // + "\n" + con.getArchiv().getExtracted() / con.getArchiv().getSize() *
        // 100 + "%";
        // break;
        // case FINISHED:
        // cacheJobStatus = T._.plugins_optional_extraction_status_extractok();
        // break;
        // case NOT_ENOUGH_SPACE:
        // cacheJobStatus =
        // T._.plugins_optional_extraction_status_notenoughspace();
        // break;
        // case FILE_NOT_FOUND:
        // cacheJobStatus = T._.plugins_optional_extraction_filenotfound();
        // break;
        // case CLEANUP:
        // cacheJobName = "";
        // cacheJobStatus = "No Job active";
        // break;
        // }
        // final StringBuilder sb = new StringBuilder();
        //
        // if (!cacheQueue.equals("")) sb.append(cacheQueue);
        // if (!cacheJobName.equals("")) sb.append(cacheJobName);
        // sb.append(cacheJobStatus);
        //
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                icon.update(event.getType(), con);
                // switch (event.getType()) {
                // case START:
                // case QUEUED:
                // if (con.getExtractionQueue().size() > 0) {
                // if (icon != null && !icon.isEnabled()) {
                // icon.setIndeterminate(true);
                // icon.setEnabled(true);
                // }
                // }
                // break;
                // case CLEANUP:
                // if (con.getExtractionQueue().size() <= 1) {
                // /*
                // * <=1 because current element is still running at this
                // * point
                // */
                // if (icon != null && icon.isEnabled()) {
                // icon.setIndeterminate(false);
                // icon.setEnabled(false);
                // }
                // }
                // break;
                // }
                // if (icon != null) icon.setDescription(sb.toString());
            }
        };
    }

    public void cleanup() {
        JDGui.getInstance().getStatusBar().remove(icon);
    }
}