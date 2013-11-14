package org.jdownloader.extensions.extraction.gui.bubble;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionEvent;
import org.jdownloader.extensions.extraction.Item;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.AbstractBubbleContentPanel;
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

        duration = addPair(_GUI._.ReconnectDialog_layoutDialogContent_duration(), IconKey.ICON_WAIT);

        archive = addPair(T._.archive(), IconKey.ICON_EXTRACT);

        extractTo = addPair(T._.archive_folder(), IconKey.ICON_FOLDER);

        file = addPair(T._.archive_file(), IconKey.ICON_FILE);

        status = addPair(T._.archive_status(), IconKey.ICON_MEDIA_PLAYBACK_START);

        progressCircle.setIndeterminate(true);
    }

    @Override
    public void stop() {
        super.stop();
    }

    public void update(final ExtractionController caller, final ExtractionEvent event) {

        double prog = caller.getProgress();
        progressCircle.setValue((int) (prog * 100));
        progressCircle.setIndeterminate((prog < .01));
        progressCircle.setStringPainted(true);

        duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
        if (event != null) {

            archive.setText(caller.getArchiv().getFirstArchiveFile().getName());
            archive.setTooltip(caller.getArchiv().getFirstArchiveFile().getFilePath());
            if (event.getType() == null) {
                System.out.println(1);
            }
            switch (event.getType()) {
            case PASSWORD_NEEDED_TO_CONTINUE:
                status.setText(T._.password_required());
                break;
            case START_CRACK_PASSWORD:
                status.setText(T._.password_search());
                break;
            case FINISHED:
            case CLEANUP:
                if (caller.isSuccessful()) {
                    status.setText(_GUI._.lit_successfull());
                } else {
                    status.setText(_GUI._.lit_failed());
                }

                break;
            case EXTRACTION_FAILED:
            case EXTRACTION_FAILED_CRC:
            case NOT_ENOUGH_SPACE:

                status.setText(_GUI._.lit_failed());
                break;

            case EXTRACTING:
            case ACTIVE_ITEM:
                status.setText(T._.extracting_in_progress());
                Item item = caller.getCurrentActiveItem();
                if (item != null && item.getFile() != null) {
                    file.setText(caller.getCurrentActiveItem().getFile().getName());
                    extractTo.setText(caller.getCurrentActiveItem().getFile().getParent());
                }
                break;

            }
        }

    }

}
