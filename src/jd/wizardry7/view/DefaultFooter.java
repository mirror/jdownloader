package jd.wizardry7.view;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class DefaultFooter extends JPanel {

    private static final long serialVersionUID = 8480183514476816275L;

    public static JPanel buildWizardBar(JButton[] leftAlignedButtons, JButton back, JButton next, JButton overlayedFinish, JButton[] rightAlignedButtons) {

        ButtonBarBuilder builder = new ButtonBarBuilder();

        if (leftAlignedButtons != null) {
            builder.addGriddedButtons(leftAlignedButtons);
            builder.addRelatedGap();
        }
        builder.addGlue();
        if (back != null) {
            builder.addGridded(back);
        }
        if (next != null) {
            builder.addGridded(next);
        }

        // Optionally overlay the finish and next button.
        if (next == null && overlayedFinish != null) {
            System.out.println("overlayedFinish: " + overlayedFinish);
            // builder.nextColumn(-1);
            // builder.add(overlayedFinish);
            // builder.nextColumn();
            builder.addGridded(overlayedFinish);
        }

        if (rightAlignedButtons != null) {
            builder.addRelatedGap();
            builder.addGriddedButtons(rightAlignedButtons);
        }
        return builder.getPanel();
    }

    public static JPanel createFooter(JButton[] leftAlignedButtons, JButton back, JButton next, JButton overlayedFinish, JButton... rightAlignedButtons) {

        FormLayout layout = new FormLayout("p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout); // , new
        // FormDebugPanel());
        builder.setDefaultDialogBorder();
        builder.appendSeparator();
        builder.append(DefaultFooter.buildWizardBar(leftAlignedButtons, back, next, overlayedFinish, rightAlignedButtons));

        return builder.getPanel();
    }

}
