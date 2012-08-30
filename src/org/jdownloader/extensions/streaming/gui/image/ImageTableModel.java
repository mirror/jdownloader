package org.jdownloader.extensions.streaming.gui.image;

import java.awt.Point;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.IconLabelToolTip;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.Application;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.T;
import org.jdownloader.extensions.streaming.gui.MediaTableModel;
import org.jdownloader.extensions.streaming.mediaarchive.ImageListController;
import org.jdownloader.extensions.streaming.mediaarchive.ImageMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveController;

public class ImageTableModel extends MediaTableModel<ImageMediaItem, ImageListController> {

    public ImageTableModel(StreamingExtension plg, MediaArchiveController mediaArchiveController) {
        super("ImageTableModel", plg, mediaArchiveController.getImageController());

    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<ImageMediaItem>(T._.gui_video_name()) {

            public ExtTooltip createToolTip(final Point position, final ImageMediaItem obj) {

                try {
                    return new IconLabelToolTip(obj.getName(), new ImageIcon(IconIO.getScaledInstance(ImageProvider.read(Application.getResource(obj.getThumbnailPath())), 320, 240))) {
                        {
                            label.setHorizontalTextPosition(SwingConstants.CENTER);
                            label.setVerticalTextPosition(SwingConstants.BOTTOM);
                        }
                    };
                } catch (IOException e) {

                }
                return super.createToolTip(position, obj);
            }

            protected Icon getIcon(final ImageMediaItem value) {
                try {

                    // of course this is a performance desaster!
                    return new ImageIcon(IconIO.getScaledInstance(ImageProvider.read(Application.getResource(value.getThumbnailPath())), 32, 32));
                } catch (Throwable e) {

                }
                return null;
            }

            @Override
            public String getStringValue(ImageMediaItem value) {

                return value.getName();
            }
        });

        addColumn(new ExtTextColumn<ImageMediaItem>(T._.gui_video_format()) {

            @Override
            public String getStringValue(ImageMediaItem value) {
                return (value.getContainerFormat() == null ? "Unknown" : value.getContainerFormat()) + " " + value.getWidth() + "x" + value.getHeight();
            }
        });

        addColumn(new ExtTextColumn<ImageMediaItem>("DLNA Profiles") {

            @Override
            public String getStringValue(ImageMediaItem value) {
                return Arrays.toString(value.getDlnaProfiles());
            }
        });
    }
}
