package org.jdownloader.updatev2;

import javax.swing.JComponent;

public interface SponsoringPanelInterface {
    public JComponent getPanel();

    void setHttpClient(SimpleHttpInterface client);

    public void init();
}
