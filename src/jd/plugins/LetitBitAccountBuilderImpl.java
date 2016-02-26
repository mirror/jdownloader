package jd.plugins;

import javax.swing.JLabel;

import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.gui.translate._GUI;

public class LetitBitAccountBuilderImpl extends DefaultEditAccountPanel {
    private ExtTextField key;

    @Override
    public Account getAccount() {
        if (StringUtils.isNotEmpty(key.getText())) {
            return new Account("", key.getText());
        }

        return super.getAccount();
    }

    public LetitBitAccountBuilderImpl(InputChangedCallbackInterface callback) {
        super(callback);

        add(new JLabel(_GUI.T.LetitBitAccountFactory_LetitBitPanel_key()), "newline");
        add(this.key = new ExtTextField() {

            @Override
            public void onChanged() {
                getCallback().onChangedInput(this);

            }

        });

        key.setHelpText(_GUI.T.LetitBitAccountFactory_LetitBitPanel_key_help());

    }
}
