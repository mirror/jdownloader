package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;

import jd.controlling.IOEQ;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.ExtensionAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.translate.ExtractionTranslation;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class SetExtractPasswordAction extends ExtensionAction<ExtractionExtension, ExtractionConfig, ExtractionTranslation> {

    protected List<Archive> archives;

    public SetExtractPasswordAction(final SelectionInfo<?, ?> selection) {
        setName(_.contextmenu_password());
        setIconKey("password");
        setEnabled(false);

        //
        if (selection == null) return;
        IOEQ.add(new Runnable() {

            @Override
            public void run() {

                archives = ArchiveValidator.validate((SelectionInfo<FilePackage, DownloadLink>) selection).getArchives();
                if (archives != null && archives.size() > 0) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            setSelected(_getExtension().isAutoExtractEnabled(archives.get(0)));

                            setEnabled(true);
                        }
                    };
                }

            }

        });

    }

    public void actionPerformed(ActionEvent e) {

        try {
            StringBuilder sb = new StringBuilder();
            HashSet<String> list = new HashSet<String>();

            for (Archive archive : archives) {
                HashSet<String> pws = archive.getSettings().getPasswords();
                if (pws != null) list.addAll(pws);
            }

            if (list != null && list.size() > 0) {
                for (String s : list) {
                    if (sb.length() > 0) sb.append("\r\n");
                    sb.append(s);
                }
            }
            String pw = null;

            if (archives.size() > 1) {
                pw = Dialog.getInstance().showInputDialog(0, _.context_password(), (list == null || list.size() == 0) ? _.context_password_msg_multi() : _.context_password_msg2_multi(sb.toString()), null, NewTheme.getInstance().getIcon("password", 32), null, null);

            } else {
                pw = Dialog.getInstance().showInputDialog(0, _.context_password(), (list == null || list.size() == 0) ? _.context_password_msg(archives.get(0).getName()) : _.context_password_msg2(archives.get(0).getName(), sb.toString()), null, NewTheme.getInstance().getIcon("password", 32), null, null);

            }
            if (!StringUtils.isEmpty(pw)) {

                list.add(pw);
                for (Archive archive : archives) {
                    archive.getSettings().setPasswords(list);
                }
            }

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }
}
