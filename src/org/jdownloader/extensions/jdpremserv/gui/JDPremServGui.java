package org.jdownloader.extensions.jdpremserv.gui;

import javax.swing.Icon;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.AddonPanel;
import net.miginfocom.swing.MigLayout;

import org.jdownloader.extensions.jdpremserv.PremServExtension;
import org.jdownloader.images.NewTheme;

public class JDPremServGui extends AddonPanel<PremServExtension> {

    private static final long serialVersionUID = 1506791566826744246L;

    public JDPremServGui(PremServExtension owner) {
        super(owner);
        init();
        SwitchPanel frame = new SwitchPanel() {
            private static final long serialVersionUID = -5274058584794029026L;

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
        frame.setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill]"));
        frame.add(new JScrollPane(PremServUserTable.getInstance()));
        // todo : lot of stuff here
    }

    @Override
    public String getID() {
        return "jdpremservgui";
    }

    @Override
    public Icon getIcon() {
        // we should use an own icon later
        return NewTheme.I().getIcon("chat", 16);
    }

    @Override
    public String getTitle() {
        return "PremServ";
    }

    @Override
    public String getTooltip() {
        return "PremServ - Internal";
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

    @Override
    protected void onDeactivated() {
    }

    @Override
    protected void onActivated() {
    }

}
