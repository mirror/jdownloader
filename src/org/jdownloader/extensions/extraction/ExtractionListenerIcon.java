package org.jdownloader.extensions.extraction;

import java.util.LinkedHashSet;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.extraction.translate.T;

public class ExtractionListenerIcon implements ExtractionListener {
    private IconedProcessIndicator pi;
    private LinkedHashSet<String>  queue;

    private String                 cacheQueue     = "";
    private String                 cacheJobName   = "";
    private String                 cacheJobStatus = "No Job active";
    private boolean                running        = false;

    public ExtractionListenerIcon(final IconedProcessIndicator pi) {
        this.pi = pi;
        queue = new LinkedHashSet<String>();
    }

    public void onExtractionEvent(final ExtractionEvent event) {
        final ExtractionController con = event.getCaller();

        switch (event.getType()) {
        case QUEUED:
            if (pi == null) pi = JDGui.getInstance().getStatusBar().getExtractionIndicator();
            queue.add(con.getArchiv().getFirstDownloadLink().getName());
            updateQueue();
            break;
        case START:
            queue.remove(con.getArchiv().getFirstDownloadLink().getName());
            cacheJobName = con.getArchiv().getFirstDownloadLink().getName();
            cacheJobStatus = T._.plugins_optional_extraction_status_openingarchive();
            running = true;
            updateQueue();
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
            running = false;
            break;
        }

        new EDTRunner() {
            @Override
            protected void runInEDT() {
                switch (event.getType()) {
                case QUEUED:
                    if (queue.size() == 1 && !running) {
                        pi.setEnabled(true);
                        pi.setIndeterminate(true);
                    }
                    break;
                case FINISHED:
                    if (queue.size() == 0) {
                        pi.setIndeterminate(false);
                        pi.setEnabled(false);
                    }
                    break;
                }

                StringBuilder sb = new StringBuilder();

                if (!cacheQueue.equals("")) sb.append(cacheQueue);
                if (!cacheJobName.equals("")) sb.append(cacheJobName);
                sb.append(cacheJobStatus);

                pi.setDescription(sb.toString());
            }
        };
    }

    private void updateQueue() {
        final StringBuilder sb = new StringBuilder();

        if (queue.size() > 0) {
            sb.append("Queue:\n");

            int i = 1;
            for (String link : queue) {
                sb.append(" " + i++ + ": " + link + "\n");
            }

            sb.append("\nCurrent:\n");
        }

        cacheQueue = sb.toString();
    }
}