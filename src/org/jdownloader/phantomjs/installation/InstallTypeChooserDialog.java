package org.jdownloader.phantomjs.installation;

import java.awt.Dialog.ModalityType;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.statistics.StatsManager.CollectionName;

public class InstallTypeChooserDialog extends AbstractDialog<Object> implements ConfirmDialogInterface {

    private MigPanel p;

    private String   task;

    public InstallTypeChooserDialog(String task) {
        super(0, _GUI.T.phantom_js_installation_dialog_title(), null, _GUI.T.phantom_js_install_now(), null);
        this.task = task;
    }

    @Override
    public boolean isRemoteAPIEnabled() {
        return true;
    }

    public static class FoundException extends Exception {

        private File file;

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public FoundException(File f) {
            file = f;
        }

    }

    private JComponent header(String text) {
        JLabel ret = SwingUtils.toBold(new JLabel(text));
        ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getForeground()));
        return ret;
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    @Override
    public JComponent layoutDialogContent() {
        p = new MigPanel("ins 10 10 10 0, wrap 2", "[][grow,fill]", "[]");

        JLabel lbl;
        p.add(header(_GUI.T.FFMpegInstallTypeChooserDialog_layoutDialogContent_problem()), "spanx");
        p.add(new JLabel(new AbstractIcon(IconKey.ICON_LOGO_PHANTOMJS_LOGO, 32)), "gapleft 10,gapright 10");
        // p.add(textField, "spanx");
        p.add(new JLabel(htmlize(_GUI.T.PhantomJSInstallTypeChooserDialog_message(task))), "spanx");

        p.add(header(_GUI.T.FFMpegInstallTypeChooserDialog_layoutDialogContent_path_chooser()), "spanx");

        // p.add(textField, "gapleft 10,spanx");
        p.add(new JLabel(htmlize(_GUI.T.Phantom_JS_Explain_download())), "gapleft 10,spanx");
        StatsManager.I().track("install/dialog", CollectionName.PJS);
        return p;
    }

    private String htmlize(String html) {

        return "<html>" + html.replaceAll("<.*?>", "") + "</html>";
    }

    @Override
    protected int getPreferredWidth() {
        return Math.max(500, super.getPreferredWidth());
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public String getMessage() {
        return _GUI.T.PhantomJSInstallTypeChooserDialog_message(task) + "\r\n\r\n" + _GUI.T.Phantom_JS_Explain_download();
    }

}
