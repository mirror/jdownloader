package jd.plugins.optional.awesomebar.awesome;

import java.awt.Component;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;


import net.miginfocom.swing.MigLayout;

public class AwesomeUtils {
	
	private static final MigLayout ProposalListElementMigLayout = new MigLayout("insets 0, fill","[left]");
	
	public static final ImageIcon defaultIcon = new ImageIcon(".\\src\\awesome\\proposal\\res\\default.png");
	
	public static Component createProposalListElement(AwesomeProposalRequestListener source, AwesomeProposalRequest request){
		JPanel panel = new JPanel(ProposalListElementMigLayout);
		panel.setOpaque(false);
		
		String matchingKeyword = null;
		for(String keyword : source.getKeywords())
		{
			if(keyword.startsWith(request.getCommand())){
				matchingKeyword = keyword;
				break;
			}
		}
		
		//TODO: Better image implementation
		String icon = ".\\src\\awesome\\proposal\\res\\"+matchingKeyword+".png";

		JLabel img;
		if(new File(icon).exists()){
			img = new JLabel(new ImageIcon(icon));
		}
		else
		{
			img = new JLabel(defaultIcon);
		}
		JLabel label = new JLabel(matchingKeyword+" "+request.getParams());
		panel.add(img);
		panel.add(label);

		
		
		return panel;
	}
	
}