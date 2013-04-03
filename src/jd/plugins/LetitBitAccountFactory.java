package jd.plugins;

import javax.swing.JLabel;

import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountFactory;
import org.jdownloader.plugins.accounts.EditAccountPanel;

public class LetitBitAccountFactory extends AccountFactory {
    public static class LetitBitPanel extends DefaultEditAccountPanel {
        private ExtTextField key;

        @Override
        public Account getAccount() {
            if (StringUtils.isNotEmpty(key.getText())) { return new Account("", key.getText()); }

            return super.getAccount();
        }

        public LetitBitPanel() {
            super();

            add(new JLabel(_GUI._.LetitBitAccountFactory_LetitBitPanel_key()), "newline");
            add(this.key = new ExtTextField() {

                @Override
                public void onChanged() {
                    if (notifier != null) {
                        notifier.onNotify();
                    }
                }

            });

            key.setHelpText(_GUI._.LetitBitAccountFactory_LetitBitPanel_key_help());

        }
    }

    @Override
    public EditAccountPanel getPanel() {
        return new LetitBitPanel();
    }

}
