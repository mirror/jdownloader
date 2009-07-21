package jd.gui.skins.jdgui.views;

import javax.swing.Icon;

import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.gui.skins.jdgui.borders.JDBorderFactory;
import jd.gui.skins.jdgui.components.linkgrabberview.LinkGrabberPanel;
import jd.gui.skins.jdgui.components.toolbar.ToolBar;
import jd.gui.skins.jdgui.interfaces.View;
import jd.gui.skins.jdgui.views.info.LinkGrabberInfoPanel;
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
        this.setDefaultInfoPanel(new LinkGrabberInfoPanel());
        ToolBar toolbar = new ToolBar(16);
        toolbar.setOpaque(false);
        toolbar.setBorder(JDBorderFactory.createInsideShadowBorder(0, 0, 5, 0));
        toolbar.setBackground(null);
        this.setToolBar(toolbar);

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
        updateToolbar("linkgrabberview", new String[] {
                "toolbar.control.start",
                "toolbar.control.pause",
                "toolbar.control.stop",
                "toolbar.separator",
                "toolbar.quickconfig.clipboardoberserver",
                "toolbar.quickconfig.reconnecttoggle",
                "toolbar.separator",
                "toolbar.interaction.reconnect",
                "toolbar.interaction.update",
                "toolbar.separator",
                "action.addurl",
                "action.load",
                "toolbar.separator",
                "action.linkgrabber.addall",
                "action.linkgrabber.clearlist"
        });
    }

}