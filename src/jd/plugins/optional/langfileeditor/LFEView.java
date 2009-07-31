package jd.plugins.optional.langfileeditor;

import javax.swing.Icon;
import javax.swing.JMenuBar;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.views.ClosableView;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class LFEView extends ClosableView {
    private static final String JDL_PREFIX = "jd.plugins.optional.langfileeditor.LFEView.";
    private LangFileEditor lfe;
    private LFEGui lfeGui;

    public LFEView(SwitchPanel panel, LangFileEditor langFileEditor) {
        super();
        this.setContent(panel);
        this.lfeGui = (LFEGui) panel;
        lfe = langFileEditor;
        this.getBroadcaster().addListener(new SwitchPanelListener() {

            @Override
            public void onPanelEvent(SwitchPanelEvent event) {
                switch (event.getID()) {
                case SwitchPanelEvent.ON_REMOVE:
                    lfe.activateAction.setSelected(false);
                }

            }

        });
        init();
    }

    protected void initMenu(JMenuBar menubar) {
        lfeGui.initMenu(menubar);
    }

    @Override
    public Icon getIcon() {

        return JDTheme.II(lfe.getIconKey(), 16, 16);
    }

    @Override
    public String getTitle() {

        return JDL.L(JDL_PREFIX + "title", "Translation Editor");
    }

    @Override
    public String getTooltip() {
        // TODO Auto-generated method stub
        return JDL.L(JDL_PREFIX + "tooltip", "Edit a translation file");

    }

    @Override
    protected void onHide() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onShow() {
        // TODO Auto-generated method stub

    }

}
