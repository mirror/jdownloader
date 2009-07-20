package jd.gui.skins.jdgui.views;

import javax.swing.Icon;

import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.gui.skins.jdgui.components.linkgrabberview.LinkGrabberPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class LinkgrabberView extends View {

    private static final long serialVersionUID = -8027069594232979742L;

    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.skins.jdgui.views.linkgrabberview.";

    public LinkgrabberView() {
        super();
        this.setContent(LinkGrabberPanel.getLinkGrabber());
        LinkGrabberController.getInstance().addListener(new LinkGrabberControllerListener() {
            public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
                switch (event.getID()) {
                case LinkGrabberControllerEvent.ADDED:
                    // taskPane.switcher(dlTskPane);
                    break;
                }
            }
        });
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.taskpanes.linkgrabber", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return JDL.L(IDENT_PREFIX + "tab.title", "Linkgrabber");
    }

    @Override
    public String getTooltip() {
        return JDL.L(IDENT_PREFIX + "tab.tooltip", "Collect, add and select links and URLs");
    }

    @Override
    protected void onHide() {
        updateToolbar(null, null);
    }

    @Override
    protected void onShow() {
        updateToolbar("linkgrabberview", new String[] { "toolbar.control.start", "toolbar.control.pause", "toolbar.control.stop", "toolbar.separator", "toolbar.quickconfig.clipboardoberserver", "toolbar.quickconfig.reconnecttoggle", "toolbar.separator", "toolbar.interaction.reconnect", "toolbar.interaction.update", "toolbar.separator", "toolbar.linkgrabber.move.top", "toolbar.linkgrabber.move.bottom" });
    }

}