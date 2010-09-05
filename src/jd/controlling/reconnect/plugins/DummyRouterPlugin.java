package jd.controlling.reconnect.plugins;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.controlling.ProgressController;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class DummyRouterPlugin extends RouterPlugin {

    private static final DummyRouterPlugin INSTANCE = new DummyRouterPlugin();

    public static RouterPlugin getInstance() {
        // TODO Auto-generated method stub
        return DummyRouterPlugin.INSTANCE;
    }

    @Override
    public boolean canCheckIP() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canRefreshIP() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean doReconnect(final ProgressController progress) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getExternalIP() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JComponent getGUI() {
        final JPanel p = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]"));
        p.add(new JLabel(JDL.L("jd.controlling.reconnect.plugins.DummyRouterPlugin.getGUI", "Please select a Reconnect Plugin")));
        p.add(Box.createGlue());
        return p;

    }

    @Override
    public String getID() {
        // TODO Auto-generated method stub
        return "DummyRouterPlugin";
    }

    @Override
    public String getName() {

        return JDL.L("jd.controlling.reconnect.plugins.DummyRouterPlugin.getName", "No Reconnect Plugin");
    }

}
