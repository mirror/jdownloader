package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionContextAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.ArraySet;
import org.jdownloader.images.NewTheme;

public class SetExtractPasswordAction extends AbstractExtractionContextAction {

    public SetExtractPasswordAction() {
        super();
        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_password());

        setSmallIcon(new ExtractIconVariant(IconKey.ICON_PASSWORD, 18, 12, -1, -1).crop());

    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            StringBuilder sb = new StringBuilder();
            ArraySet<String> list = new ArraySet<String>();

            for (Archive archive : archives) {
                List<String> pws = archive.getSettings().getPasswords();
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
                pw = Dialog.getInstance().showInputDialog(0, org.jdownloader.extensions.extraction.translate.T._.context_password(), (list == null || list.size() == 0) ? org.jdownloader.extensions.extraction.translate.T._.context_password_msg_multi() : org.jdownloader.extensions.extraction.translate.T._.context_password_msg2_multi(sb.toString()), null, NewTheme.getInstance().getIcon("password", 32), null, null);

            } else {
                pw = Dialog.getInstance().showInputDialog(0, org.jdownloader.extensions.extraction.translate.T._.context_password(), (list == null || list.size() == 0) ? org.jdownloader.extensions.extraction.translate.T._.context_password_msg(archives.get(0).getName()) : org.jdownloader.extensions.extraction.translate.T._.context_password_msg2(archives.get(0).getName(), sb.toString()), null, NewTheme.getInstance().getIcon("password", 32), null, null);

            }
            if (!StringUtils.isEmpty(pw)) {

                list.add(pw);
                for (Archive archive : archives) {
                    archive.setPasswords(list);

                }
            }

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }
}
