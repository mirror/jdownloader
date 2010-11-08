package jd.plugins.optional.jdfeedme;

import javax.swing.Icon;

import jd.gui.swing.jdgui.views.ClosableView;
import jd.utils.JDTheme;

public class JDFeedMeView extends ClosableView {
	
	private static final long serialVersionUID = -8074441650881378626L;
	
	public JDFeedMeView() {
        super();

        init();
    }
	
	@Override
	public Icon getIcon() {
		// TODO Auto-generated method stub
		return JDTheme.II("gui.images.reconnect", ICON_SIZE, ICON_SIZE);
	}

	@Override
	public String getTitle() {
		
		return "JD FeedMe";
	}

	@Override
	public String getTooltip() {
		
		return "Autodownload files from RSS feeds";
	}

	/* CODE_FOR_INTERFACE_7_START */
	@Override
	protected void onHide() {}
	/* CODE_FOR_INTERFACE_7_END */

	/* CODE_FOR_INTERFACE_7_START */
	@Override
	protected void onShow() {}
	/* CODE_FOR_INTERFACE_7_END */

    @Override
    public String getID() {
        return "jdfeedme";
    }

}
