package jd.wizardry7;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import jd.wizardry7.controller.WizardController;
import jd.wizardry7.view.DefaultFooter;
import jd.wizardry7.view.DefaultHeader;
import jd.wizardry7.view.DefaultWizardPage;
import jd.wizardry7.view.pages.CheckAGB;
import jd.wizardry7.view.pages.DownloadFolder;
import jd.wizardry7.view.pages.Finished;
import jd.wizardry7.view.pages.Reconnect;
import jd.wizardry7.view.pages.Welcome;



public class WizardPages {
	
	private static final ImageIcon aboutIcon      = getImageIcon("res/about.gif");
	private static final ImageIcon backwardIcon    = getImageIcon("res/leftarrow.png");
	private static final ImageIcon cancelIcon      = getImageIcon("res/stop.png");
	private static final ImageIcon finishIcon      = getImageIcon("res/finish.gif");
	private static final ImageIcon forwardIcon     = getImageIcon("res/rightarrow.png");
	
	
	private static JButton getAboutButton() {
	    return getButton("About", "Visit the JDownloader Website", aboutIcon, WizardController.helpAction);
	}

	private static JButton getBackButton() {
		return getButton("Previous", "Previous-Tooltip", backwardIcon, WizardController.backwardAction);
	}

	
	
	private static JButton getButton(String text, String tooltip, ImageIcon imageIcon, Action action) {
        JButton button = new JButton(action);
        button.setText(text);
        button.setToolTipText(tooltip);
        button.setIcon(imageIcon);
        return button;
    }
	
	private static JButton getCancelButton() {
		return getButton("Cancel", "BUTTONS_CANCEL_TOOLTIP", cancelIcon, WizardController.cancelAction);
	}

    private static JButton getFinishButton() {
        return getButton("Finish", "Start using JDownloader", finishIcon, WizardController.finishedAction);
    }
		
	private static ImageIcon getImageIcon(String icon) {
        return new ImageIcon(icon);
    }

    private static JButton getNextButton() {
		JButton button = getButton("Next", "BUTTONS_NEXT_TOOLTIP", forwardIcon, WizardController.forwardAction);
		button.setHorizontalTextPosition(SwingConstants.LEFT);
		return button;
	}
    


    public static DefaultWizardPage[] getWizardPages() {
		DefaultWizardPage[] wizardPages = new DefaultWizardPage[]{
				Welcome.getInstance(),
				CheckAGB.getInstance(),
				DownloadFolder.getInstance(),
				Reconnect.getInstance(),
				Finished.getInstance(),
		};
		
		ImageIcon headerIcon;
		int step = 0;
		int of = wizardPages.length;
		
		step++;
		headerIcon = getImageIcon("res/userinfo.jpg");
		Welcome.getInstance().setHeader(new DefaultHeader("Welcome", "The JDownloader Wizard allows you to easily get started.", headerIcon, step, of));
		Welcome.getInstance().setFooter(DefaultFooter.createFooter(new JButton[]{getAboutButton()}, null, getNextButton(), null, getCancelButton()));
		
        step++;
        headerIcon = getImageIcon("res/agb.jpg");
        CheckAGB.getInstance().setHeader(new DefaultHeader("AGBs", "You have to read and accept our AGBs prior to using JDownloader", headerIcon, step, of));
        CheckAGB.getInstance().setFooter(DefaultFooter.createFooter(new JButton[]{getAboutButton()}, getBackButton(), getNextButton(), null, getCancelButton()));

        step++;
        headerIcon = getImageIcon("res/agb.jpg");
        DownloadFolder.getInstance().setHeader(new DefaultHeader("AGBs", "You have to read and accept our AGBs prior to using JDownloader", headerIcon, step, of));
        DownloadFolder.getInstance().setFooter(DefaultFooter.createFooter(new JButton[]{getAboutButton()}, getBackButton(), getNextButton(), null, getCancelButton()));

        step++;
        headerIcon = getImageIcon("res/agb.jpg");
        Reconnect.getInstance().setHeader(new DefaultHeader("AGBs", "You have to read and accept our AGBs prior to using JDownloader", headerIcon, step, of));
        Reconnect.getInstance().setFooter(DefaultFooter.createFooter(new JButton[]{getAboutButton()}, getBackButton(), getNextButton(), null, getCancelButton()));

        
		step++;
		headerIcon = getImageIcon("res/agb.jpg");
		Finished.getInstance().setHeader(new DefaultHeader("Congratulations!", "With these Settings JDownloader should run smoothly.", headerIcon, step, of));
		Finished.getInstance().setFooter(DefaultFooter.createFooter(new JButton[]{getAboutButton()}, getBackButton(), null, null, getFinishButton()));

		return wizardPages;
	}

}
