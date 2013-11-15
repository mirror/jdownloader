package org.jdownloader.extensions.extraction.gui.bubble;

import java.util.ArrayList;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.extensions.extraction.CFG_EXTRACTION;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionEvent;
import org.jdownloader.extensions.extraction.Item;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.AbstractBubbleContentPanel;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.translate._GUI;

public class ExtractionBubbleContent extends AbstractBubbleContentPanel {

    private long startTime;

    private Pair duration;

    private Pair archive;

    private Pair status;

    private Pair file;

    private Pair extractTo;

    public ExtractionBubbleContent() {
        super(IconKey.ICON_ARCHIVE);
        startTime = System.currentTimeMillis();
        // super("ins 0,wrap 2", "[][grow,fill]", "[grow,fill]");

        // , _GUI._.balloon_reconnect_start_msg(), NewTheme.I().getIcon("reconnect", 32)

        layoutComponents();

        progressCircle.setIndeterminate(true);
        progressCircle.setValue(0);
    }

    @Override
    public void stop() {
        super.stop();
    }

    public void update(final ExtractionController caller, final ExtractionEvent event) {

        double prog = caller.getProgress();
        if (progressCircle != null) {
            progressCircle.setValue((int) (prog * 100));
            progressCircle.setIndeterminate((prog < .01));
            progressCircle.setStringPainted(true);
        }
        if (duration != null) {

            duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
        }
        if (event != null) {
            if (archive != null) {
                archive.setText(caller.getArchiv().getFirstArchiveFile().getName());
                archive.setTooltip(caller.getArchiv().getFirstArchiveFile().getFilePath());
            }

            switch (event.getType()) {
            case PASSWORD_NEEDED_TO_CONTINUE:
                if (status != null) {
                    status.setText(T._.password_required());
                }
                break;
            case START_CRACK_PASSWORD:
                if (status != null) {
                    status.setText(T._.password_search());
                }
                break;
            case FINISHED:
            case CLEANUP:
                if (status != null) {
                    if (caller.isSuccessful()) {
                        status.setText(_GUI._.lit_successfull());
                    } else {
                        status.setText(_GUI._.lit_failed());
                    }
                }

                break;
            case EXTRACTION_FAILED:
            case EXTRACTION_FAILED_CRC:
            case NOT_ENOUGH_SPACE:
                if (status != null) {
                    status.setText(_GUI._.lit_failed());
                }
                break;

            case EXTRACTING:
            case ACTIVE_ITEM:
                if (status != null) {
                    status.setText(T._.extracting_in_progress());
                }
                Item item = caller.getCurrentActiveItem();
                if (item != null && item.getFile() != null) {
                    if (file != null) {
                        file.setText(caller.getCurrentActiveItem().getFile().getName());
                    }
                    if (extractTo != null) {
                        extractTo.setText(caller.getCurrentActiveItem().getFile().getParent());
                    }
                }
                break;

            }
        }

    }

    protected void addProgress() {

    }

    protected void layoutComponents() {
        if (CFG_EXTRACTION.BUBBLE_CONTENT_CIRCLE_PROGRESS_VISIBLE.isEnabled()) {
            setLayout(new MigLayout("ins 3 3 0 3,wrap 3", "[][fill][grow,fill]", "[]"));
            add(progressCircle, "width 32!,height 32!,pushx,growx,pushy,growy,spany,aligny top");
        } else {
            setLayout(new MigLayout("ins 3 3 0 3,wrap 2", "[fill][grow,fill]", "[]"));
        }

        if (CFG_EXTRACTION.BUBBLE_CONTENT_DURATION_VISIBLE.isEnabled()) {
            duration = addPair(duration, _GUI._.ReconnectDialog_layoutDialogContent_duration(), IconKey.ICON_WAIT);
        }

        if (CFG_EXTRACTION.BUBBLE_CONTENT_ARCHIVENAME_VISIBLE.isEnabled()) {
            archive = addPair(archive, T._.archive(), IconKey.ICON_EXTRACT);
        }

        if (CFG_EXTRACTION.BUBBLE_CONTENT_EXTRACT_TO_FOLDER_VISIBLE.isEnabled()) {
            extractTo = addPair(extractTo, T._.archive_folder(), IconKey.ICON_FOLDER);
        }

        if (CFG_EXTRACTION.BUBBLE_CONTENT_CURRENT_FILE_VISIBLE.isEnabled()) {
            file = addPair(file, T._.archive_file(), IconKey.ICON_FILE);
        }

        if (CFG_EXTRACTION.BUBBLE_CONTENT_STATUS_VISIBLE.isEnabled()) {
            status = addPair(status, T._.archive_status(), IconKey.ICON_MEDIA_PLAYBACK_START);
        }
    }

    public static void fillElements(ArrayList<Element> elements) {

        elements.add(new Element(CFG_EXTRACTION.BUBBLE_CONTENT_CIRCLE_PROGRESS_VISIBLE, T._.bubblecontent_progress(), IconKey.ICON_ARCHIVE));
        elements.add(new Element(CFG_EXTRACTION.BUBBLE_CONTENT_DURATION_VISIBLE, _GUI._.ReconnectDialog_layoutDialogContent_duration(), IconKey.ICON_WAIT));
        elements.add(new Element(CFG_EXTRACTION.BUBBLE_CONTENT_ARCHIVENAME_VISIBLE, T._.archive(), IconKey.ICON_EXTRACT));
        elements.add(new Element(CFG_EXTRACTION.BUBBLE_CONTENT_EXTRACT_TO_FOLDER_VISIBLE, T._.archive_folder(), IconKey.ICON_FOLDER));
        elements.add(new Element(CFG_EXTRACTION.BUBBLE_CONTENT_CURRENT_FILE_VISIBLE, T._.archive_file(), IconKey.ICON_FILE));
        elements.add(new Element(CFG_EXTRACTION.BUBBLE_CONTENT_STATUS_VISIBLE, T._.archive_status(), IconKey.ICON_MEDIA_PLAYBACK_START));

    }

    @Override
    public void updateLayout() {
        removeAll();
        layoutComponents();
        revalidate();
        repaint();
    }
}
