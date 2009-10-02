package jd.plugins.optional.customizer;

import javax.swing.Icon;

import jd.gui.swing.jdgui.views.ClosableView;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class CustomizerView extends ClosableView {

    private static final long serialVersionUID = -8077441680881378656L;
    private static final String JDL_PREFIX = "jd.plugins.optional.customizer.CustomizerView.";

    public CustomizerView() {
        super();

        init();
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.package_opened", 16, 16);
    }

    @Override
    public String getTitle() {
        return JDL.L(JDL_PREFIX + "title", "Package Customizer");
    }

    @Override
    public String getTooltip() {
        return JDL.L(JDL_PREFIX + "tooltip", "Customize your FilePackages");
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
    }

}
