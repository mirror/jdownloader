package jd.plugins.optional.webinterface.gwt.client;

import java.awt.Image;

import org.jvnet.substance.utils.SubstanceInternalFrameTitlePane.ClickListener;

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
