package org.jdownloader.extensions.streaming.gui.audio;

import java.awt.Point;

import javax.swing.Icon;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.extensions.streaming.T;
import org.jdownloader.extensions.streaming.gui.MediaTableModel;
import org.jdownloader.extensions.streaming.mediaarchive.AudioListController;
import org.jdownloader.extensions.streaming.mediaarchive.AudioMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveController;

public class AudioTableModel extends MediaTableModel<AudioMediaItem, AudioListController> {

    public AudioTableModel(MediaArchiveController mediaArchiveController) {
        super("AudioTableModel", mediaArchiveController.getAudioController());

    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<AudioMediaItem>(T._.gui_video_name()) {

            public ExtTooltip createToolTip(final Point position, final AudioMediaItem obj) {

                // try {
                // return new IconLabelToolTip(obj.getName(), new ImageIcon(IconIO.getScaledInstance(ImageIO.read(new
                // File(obj.getThumbnailPath())), 320, 240))) {
                // {
                // label.setHorizontalTextPosition(SwingConstants.CENTER);
                // label.setVerticalTextPosition(SwingConstants.BOTTOM);
                // }
                // };
                // } catch (IOException e) {
                //
                // }
                return super.createToolTip(position, obj);
            }

            protected Icon getIcon(final AudioMediaItem value) {
                // try {
                //
                // // of course this is a performance desaster!
                // return new ImageIcon(IconIO.getScaledInstance(ImageIO.read(new File(value.getThumbnailPath())), 32, 32));
                // } catch (Throwable e) {
                //
                // }
                return null;
            }

            @Override
            public String getStringValue(AudioMediaItem value) {

                return value.getName();
            }
        });

        addColumn(new ExtTextColumn<AudioMediaItem>(T._.gui_video_format()) {

            @Override
            public String getStringValue(AudioMediaItem value) {
                return value.getContainerFormat();
            }
        });

        addColumn(new ExtTextColumn<AudioMediaItem>(T._.gui_video_status()) {

            @Override
            public String getStringValue(AudioMediaItem value) {
                return "";
            }
        });
    }
}
