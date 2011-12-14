package org.jdownloader.extensions.extraction;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.extraction.translate.T;

public class ExtractionListenerIcon implements ExtractionListener {

    private String cacheQueue     = "";
    private String cacheJobName   = "";
    private String cacheJobStatus = "No Job active";

    public ExtractionListenerIcon() {
    }

    public void onExtractionEvent(final ExtractionEvent event) {
        final ExtractionController con = event.getCaller();

        switch (event.getType()) {
        case QUEUED:
            break;
        case START:
            cacheJobName = con.getArchiv().getFirstArchiveFile().getName();
            cacheJobStatus = T._.plugins_optional_extraction_status_openingarchive();
            break;
        case START_CRACK_PASSWORD:
            cacheJobStatus = "Start password finding";
            break;
        case PASSWORT_CRACKING:
            cacheJobStatus = T._.plugins_optional_extraction_status_crackingpass() + "\n" + con.getCrackProgress() / con.getPasswordListSize() * 100 + "% \t" + con.getCrackProgress() + " / " + con.getPasswordListSize();
            break;
        case PASSWORD_FOUND:
            cacheJobStatus = T._.plugins_optional_extraction_status_passfound();
            break;
        case EXTRACTING:
            cacheJobStatus = T._.plugins_optional_extraction_status_extracting() + "\n" + con.getArchiv().getExtracted() / con.getArchiv().getSize() * 100 + "%";
            break;
        case FINISHED:
            cacheJobStatus = T._.plugins_optional_extraction_status_extractok();
            break;
        case NOT_ENOUGH_SPACE:
            cacheJobStatus = T._.plugins_optional_extraction_status_notenoughspace();
            break;
        case FILE_NOT_FOUND:
            cacheJobStatus = T._.plugins_optional_extraction_filenotfound();
            break;
        case CLEANUP:
            cacheJobName = "";
            cacheJobStatus = "No Job active";
            break;
        }
        final StringBuilder sb = new StringBuilder();

        if (!cacheQueue.equals("")) sb.append(cacheQueue);
        if (!cacheJobName.equals("")) sb.append(cacheJobName);
        sb.append(cacheJobStatus);
        final IconedProcessIndicator pi = JDGui.getInstance().getStatusBar().getExtractionIndicator();
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                switch (event.getType()) {
                case START:
                case QUEUED:
                    if (con.getExtractionQueue().size() > 0) {
                        if (pi != null && !pi.isEnabled()) {
                            pi.setIndeterminate(true);
                            pi.setEnabled(true);
                        }
                    }
                    break;
                case FINISHED:
                    if (con.getExtractionQueue().size() <= 1) {
                        /*
                         * <=1 because current element is still running at this
                         * point
                         */
                        if (pi != null && pi.isEnabled()) {
                            pi.setIndeterminate(false);
                            pi.setEnabled(false);
                        }
                    }
                    break;
                }
                if (pi != null) pi.setDescription(sb.toString());
            }
        };
    }

}