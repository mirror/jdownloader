package org.jdownloader.gui.dialog;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.io.File;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class AskContainerPasswordDialog extends InputDialog implements AskContainerPasswordDialogInterface {
    private final File containerfile;

    public AskContainerPasswordDialog(final String title, String message, final File containerfile) {
        super(UIOManager.LOGIC_COUNTDOWN, title, message, null, new AbstractIcon(IconKey.ICON_PASSWORD, 32), _GUI.T.lit_continue(), null);
        setTimeout(10 * 60 * 1000);
        this.containerfile = containerfile;
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel p = new MigPanel("ins 0,wrap 1", "[]", "[][]");
        if (!StringUtils.isEmpty(message)) {
            textField = new JTextPane() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean getScrollableTracksViewportWidth() {
                    return !BinaryLogic.containsAll(flagMask, Dialog.STYLE_LARGE);
                }

                public boolean getScrollableTracksViewportHeight() {
                    return true;
                }
            };
            final Font font = textField.getFont();
            textField.setContentType("text/plain");
            textField.setFont(font);
            textField.setText(message);
            textField.setEditable(false);
            textField.setBackground(null);
            textField.setOpaque(false);
            textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
            textField.setCaretPosition(0);
            p.add(textField, "pushx, growx");
            // inout dialog can become too large(height) if we do not limit the
            // prefered textFIled size here.
            textField.setPreferredSize(textField.getPreferredSize());
        }
        p.add(SwingUtils.toBold(new JLabel("Path to container:")), "split 2,sizegroup left,alignx left");
        p.add(leftLabel(this.containerfile.getAbsolutePath()));
        p.add(SwingUtils.toBold(new JLabel("Container type")), "split 2,sizegroup left,alignx left");
        final String ext = Files.getExtension(this.containerfile.getName(), true);
        final String containerTypeText;
        if (ext != null) {
            containerTypeText = ext;
        } else {
            containerTypeText = "unknown";
        }
        final JLabel ret = new JLabel(containerTypeText);
        ret.setHorizontalAlignment(SwingConstants.LEFT);
        if (ext != null) {
            try {
                ret.setIcon(CrossSystem.getMime().getFileIcon(ext, 16, 16));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        p.add(ret);
        input = getSmallInputComponent();
        // this.input.setBorder(BorderFactory.createEtchedBorder());
        input.setText(defaultMessage);
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.ExtractionListenerList_layoutDialogContent_password())), "split 2,sizegroup left,alignx left");
        p.add((JComponent) input, "w 450,pushx,growx");
        return p;
    }

    private Component leftLabel(String name) {
        JLabel ret = new JLabel(name);
        ret.setHorizontalAlignment(SwingConstants.LEFT);
        return ret;
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    @Override
    public File getFile() {
        return this.containerfile;
    }
}
