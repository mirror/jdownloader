package org.jdownloader.gui.dialog;

import java.awt.Component;
import java.awt.Dialog.ModalityType;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import jd.plugins.CryptedLink;
import jd.plugins.Plugin;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class AskForCryptedLinkPasswordDialog extends InputDialog implements AskCrawlerPasswordDialogInterface {
    private CryptedLink link;
    private Plugin      plugin;

    public AskForCryptedLinkPasswordDialog(String message, CryptedLink link, Plugin plugin) {
        super(UIOManager.LOGIC_COUNTDOWN, _GUI._.AskForPasswordDialog_AskForPasswordDialog_title_(), message, null, new AbstractIcon(IconKey.ICON_PASSWORD, 32), _GUI._.lit_continue(), null);
        this.link = link;
        setTimeout(10 * 60 * 1000);
        this.plugin = plugin;
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

            textField.setContentType("text/plain");

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

        p.add(SwingUtils.toBold(new JLabel(_GUI._.lit_url())), "split 2,sizegroup left,alignx left");
        p.add(leftLabel(link.getCryptedUrl()));
        p.add(SwingUtils.toBold(new JLabel(_GUI._.lit_plugin())), "split 2,sizegroup left,alignx left");

        DomainInfo di = DomainInfo.getInstance(plugin.getHost());

        JLabel ret = new JLabel(di.getTld());
        ret.setHorizontalAlignment(SwingConstants.LEFT);
        ret.setIcon(di.getFavIcon());
        p.add(ret);

        input = getSmallInputComponent();
        // this.input.setBorder(BorderFactory.createEtchedBorder());
        input.setText(defaultMessage);
        p.add(SwingUtils.toBold(new JLabel(_GUI._.ExtractionListenerList_layoutDialogContent_password())), "split 2,sizegroup left,alignx left");
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
    public String getPluginHost() {
        return plugin.getHost();
    }

    @Override
    public String getUrl() {
        return link.getCryptedUrl();
    }
}
