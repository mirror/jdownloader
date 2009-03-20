package jd.plugins.optional.webinterface.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.util.SC;

public class jd implements EntryPoint {

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        Image img = new Image("http://code.google.com/webtoolkit/logo-185x175.png");
        Button button = new Button("Click me");

        // We can add style names
        button.addStyleName("pc-template-btn");
        // or we can set an id on a specific element for styling
        img.getElement().setId("pc-template-img");

        VerticalPanel vPanel = new VerticalPanel();
        vPanel.setWidth("100%");
        vPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        vPanel.add(img);
        vPanel.add(button);

        // Add image and button to the RootPanel
        RootPanel.get().add(vPanel);

        button.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                SC.say("Test erfolgreich");
            }
        });
    }
}
