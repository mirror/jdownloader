package org.jdownloader.extensions.neembuu.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.AddonPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.neembuu.JDDownloadSession;
import org.jdownloader.extensions.neembuu.NeembuuExtension;
import org.jdownloader.extensions.neembuu.translate._NT;

public class NeembuuGui extends AddonPanel<NeembuuExtension> {

    /**
	 * 
	 */
    private static final long   serialVersionUID = -3729817467785635683L;
    private static final String ID               = "NEEMBUUGUI";
    private SwitchPanel         panel;
    private final JTabbedPane   tabbedPane       = new JTabbedPane();

    public NeembuuGui(NeembuuExtension plg) {
        super(plg);
        this.panel = new SwitchPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]")) {

            /**
					 * 
					 */
            private static final long serialVersionUID = -3679648663863943775L;

            @Override
            protected void onShow() {

            }

            @Override
            protected void onHide() {
            }
        };
        // layout all contents in panel
        this.setContent(panel);

        layoutPanel();
    }

    public void addSession(JDDownloadSession jdds) {
        tabbedPane.add(jdds.getDownloadLink().getBrowserUrl(), jdds.getWatchAsYouDownloadSession().getFilePanel());
        /*
         * tabbedPane.setTabComponentAt(
         * tabbedPane.indexOfTab(jdds.getDownloadLink().getBrowserUrl()), new
         * ButtonTabComponent(tabbedPane, jdds, this));
         */
    }

    public void removeSession(JDDownloadSession jdds) {
        tabbedPane.removeTabAt(tabbedPane.indexOfTab(jdds.getDownloadLink().getBrowserUrl()));
    }

    private void layoutPanel() {
        panel.add(tabbedPane);
        JPanel settingsPanel = new JPanel();
        final JLabel jl = new JLabel("Virtual fielsystem unchecked");
        JButton testJpfm = new JButton("Check virtual filesystem capability");
        tabbedPane.addTab("Settings", settingsPanel);
        settingsPanel.add(new JLabel("TODO:A few basic settings to be added here."));
        settingsPanel.add(testJpfm);
        settingsPanel.add(jl);

        testJpfm.addActionListener(new ActionListener() {

            // @Override
            public void actionPerformed(ActionEvent e) {
                boolean usable = getExtension().isUsable();
                if (usable) {
                    jl.setText("Virtual fielsystem works");
                } else {
                    jl.setText("Virtual fielsystem not working");
                }
            }
        });
        // panel.add(new JLabel("Hello WOrld"));
    }

    /**
     * is called if, and only if! the view has been closed
     */
    @Override
    protected void onDeactivated() {
        Log.L.finer("onDeactivated " + getClass().getSimpleName());
    }

    /**
     * is called, if the gui has been opened.
     */
    @Override
    protected void onActivated() {
        Log.L.finer("onActivated " + getClass().getSimpleName());
    }

    @Override
    public Icon getIcon() {
        return this.getExtension().getIcon(16);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getTitle() {
        return _NT._.gui_title();
    }

    @Override
    public String getTooltip() {
        return _NT._.gui_tooltip();
    }

    /**
     * Is called if gui is visible now, and has not been visible before. For
     * example, user starte the extension, opened the view, or switched form a
     * different tab to this one
     */
    @Override
    protected void onShow() {
        Log.L.finer("Shown " + getClass().getSimpleName());
    }

    /**
     * gets called of the extensiongui is not visible any more. for example
     * because it has been closed or user switched to a different tab/view
     */
    @Override
    protected void onHide() {
        Log.L.finer("hidden " + getClass().getSimpleName());
    }

}
