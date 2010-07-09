package jd.plugins.optional.neembuu;

import javax.swing.Icon;
import javax.swing.JLabel;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.ClosableView;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class NeembuuTab extends ClosableView {

    public NeembuuTab() {
        SwitchPanel frame = new SwitchPanel() {

            @Override
            public void onShow() {
            }

            @Override
            public void onHide() {
            }
        };
        layoutContents(frame);

        this.setContent(frame);
    }

    private void layoutContents(SwitchPanel frame) {
        // Layout your gui here. we use MIglayout
        frame.setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill][]"));
        frame.add(new JLabel("Hello World"));
    }

    @Override
    public String getID() {

        return "neembuugui";
    }

    @Override
    public Icon getIcon() {
        // we should use an own icon later
        return JDTheme.II("gui.images.chat", 16, 16);

    }

    @Override
    public String getTitle() {

        return "Neembuu";
    }

    @Override
    public String getTooltip() {

        return "Neembuu - watch as you download";
    }

    @Override
    protected void onHide() {
        // panel is not visible any more now... not closed.. just not the
        // selected tab any more

    }

    @Override
    protected void onShow() {
        // panel is now the selected tab.

    }

}
