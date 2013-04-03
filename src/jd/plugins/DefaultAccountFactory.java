package jd.plugins;

import org.jdownloader.plugins.accounts.AccountFactory;
import org.jdownloader.plugins.accounts.EditAccountPanel;

public class DefaultAccountFactory extends AccountFactory {

    @Override
    public EditAccountPanel getPanel() {
        DefaultEditAccountPanel panel = new DefaultEditAccountPanel();

        return panel;
    }

}
