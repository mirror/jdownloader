package jd.plugins.optional.awesomebar.awesome;

import java.awt.Component;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.utils.JDUtilities;


import net.miginfocom.swing.MigLayout;

public class AwesomeUtils {
	
	private static final MigLayout ProposalListElementMigLayout = new MigLayout("insets 0, fill","[left]");
	
	public static final ImageIcon defaultIcon = new ImageIcon(JDUtilities.getResourceFile("jd/img/awesomebar/default.png").getAbsolutePath());
	
	public static Component createProposalListElement(AwesomeProposalRequestListener source, AwesomeProposalRequest request){
		String matchingKeyword = null;
		for(String keyword : source.getKeywords())
		{
			if(keyword.startsWith(request.getCommand())){
				matchingKeyword = keyword;
				break;
			}
		}
		if(matchingKeyword == null)
		{
		    matchingKeyword = source.getKeywords().get(0);
		}
		return createProposalListElement(source,request,matchingKeyword);
	}
	
	   public static Component createProposalListElement(AwesomeProposalRequestListener source, AwesomeProposalRequest request, String keyword){
	        JPanel panel = new JPanel(ProposalListElementMigLayout);
	        panel.setOpaque(false);
	        
	        //TODO: Better image implementation
	        String icon = JDUtilities.getResourceFile("jd/img/awesomebar/"+keyword+".png").getAbsolutePath();

	        JLabel img;
	        if(new File(icon).exists()){
	            img = new JLabel(new ImageIcon(icon));
	        }
	        else
	        {
	            img = new JLabel(defaultIcon);
	        }
	        JLabel label = new JLabel(keyword+" "+request.getParams());
	        panel.add(img);
	        panel.add(label);

	        
	        
	        return panel;
	    }
	
}