package org.jdownloader.extensions.cvidplay.gui;

import javax.swing.Icon;
import javax.swing.JLabel;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.AddonPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.cvidplay.CVidPlayExtension;
import org.jdownloader.extensions.cvidplay.translate._CVPT;

public class CVidPlayGui extends AddonPanel<CVidPlayExtension> {

    /**
	 * 
	 */
    private static final long   serialVersionUID = 7834140957996658357L;
    private static final String ID               = "CVIDPLAYGUI";
    private SwitchPanel         panel;

    public CVidPlayGui(CVidPlayExtension plg) {
        super(plg);
        this.panel = new SwitchPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]")) {

            /**
			 * 
			 */
            private static final long serialVersionUID = -1376837819858450584L;

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

    private void layoutPanel() {
        panel.add(new JLabel("Hello WOrld"));
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
        return _CVPT._.gui_title();
    }

    @Override
    public String getTooltip() {
        return _CVPT._.gui_tooltip();
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
