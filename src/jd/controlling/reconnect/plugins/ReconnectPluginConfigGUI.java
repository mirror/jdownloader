package jd.controlling.reconnect.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JViewport;

import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.StorageEvent;
import org.appwork.storage.StorageValueChangeEvent;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.swing.EDTRunner;

public class ReconnectPluginConfigGUI extends JPanel implements ActionListener, DefaultEventListener<StorageEvent> {

    /**
     * 
     */
    private static final long                     serialVersionUID = 1L;
    private static final ReconnectPluginConfigGUI INSTANCE         = new ReconnectPluginConfigGUI();

    public static ReconnectPluginConfigGUI getInstance() {
        return ReconnectPluginConfigGUI.INSTANCE;
    }

    private JComboBox combobox;
    private JViewport viewPort;

    private ReconnectPluginConfigGUI() {
        super(new MigLayout("ins 5,wrap 1", "[grow,fill]", "[][][grow,fill]"));
        this.initGUI();
        ReconnectPluginController.getInstance().getStorage().getEventSender().addListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
        ReconnectPluginController.getInstance().setActivePlugin((RouterPlugin) this.combobox.getSelectedItem());
        this.viewPort.setView(((RouterPlugin) this.combobox.getSelectedItem()).getGUI());

    }

    private void initGUI() {

        this.combobox = new JComboBox(ReconnectPluginController.getInstance().getPlugins().toArray(new RouterPlugin[] {}));

        this.combobox.setSelectedItem(ReconnectPluginController.getInstance().getActivePlugin());
        this.viewPort = new JViewport();
        this.add(new JLabel(JDL.L("jd.controlling.reconnect.plugins.ReconnectPluginConfigGUI.initGUI.comboboxlabel", "Active Reconnect Plugin:")), "split 2,shrinkx");
        this.add(this.combobox, "growx, pushx");
        this.add(new JSeparator(), "height 16!");
        this.add(this.viewPort);
        this.viewPort.setView(((RouterPlugin) this.combobox.getSelectedItem()).getGUI());

        this.combobox.addActionListener(this);

    }

    /**
     * UPdate GUI
     */
    public void onEvent(final StorageEvent event) {
        if (event instanceof StorageValueChangeEvent<?>) {
            final StorageValueChangeEvent<?> changeEvent = (StorageValueChangeEvent<?>) event;
            if (changeEvent.getKey().equals(ReconnectPluginController.PRO_ACTIVEPLUGIN)) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        // ReconnectPluginConfigGUI.this.viewPort.setView(ReconnectPluginController.getInstance().getActivePlugin().getGUI());
                        ReconnectPluginConfigGUI.this.combobox.setSelectedItem(ReconnectPluginController.getInstance().getActivePlugin());
                    }

                };

            }
        }

    }
}
