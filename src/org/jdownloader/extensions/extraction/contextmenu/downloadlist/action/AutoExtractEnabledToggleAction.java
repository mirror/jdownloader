package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.ImageIcon;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveSettings.BooleanStatus;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionAction;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class AutoExtractEnabledToggleAction extends AbstractExtractionAction {

    protected List<Archive> archives;

    public AutoExtractEnabledToggleAction(final SelectionInfo<?, ?> selection) {
        super(selection);
        setName(_.contextmenu_autoextract());
        setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("unpack", 18), NewTheme.I().getImage("refresh", 12), 0, 0, 10, 10)));
        setSelected(false);
        setEnabled(false);

    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();
        if (archives!=null&&archives.size() > 0) setSelected(_getExtension().isAutoExtractEnabled(archives.get(0)));
    }

    public void actionPerformed(ActionEvent e) {
        for (Archive archive : archives) {
            archive.getSettings().setAutoExtract(isSelected() ? BooleanStatus.TRUE : BooleanStatus.FALSE);
        }
        Dialog.getInstance().showMessageDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, isSelected() ? _.set_autoextract_true() : _.set_autoextract_false());
    }

}
