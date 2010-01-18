package jd.plugins.optional.awesomebar;

import java.awt.Dimension;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import jd.plugins.optional.awesomebar.awesome.Awesome;
import jd.plugins.optional.awesomebar.awesome.AwesomeAction;
import jd.plugins.optional.awesomebar.awesome.AwesomeProposalDetailPanel;
import jd.plugins.optional.awesomebar.awesome.jlist.AwesomeProposalJList;
import jd.plugins.optional.awesomebar.awesome.jlist.AwesomeProposalListModel;
import jd.plugins.optional.awesomebar.awesome.jlist.AwesomeProposalListSelectionModel;
import net.miginfocom.swing.MigLayout;

public class Awesomebar_testcase extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel jContentPane = null;
    private JTextField jTextField = null;
    private Awesome awesome = new Awesome(); // @jve:decl-index=0:
    private AwesomeProposalJList proposalList = null;
    private AwesomeProposalDetailPanel detailPanel = null;

    /**
     * This is the default constructor
     */
    public Awesomebar_testcase() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setContentPane(getJContentPane());
        this.setTitle("Awesomebar");
        this.setSize(new Dimension(420, 300));

    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            MigLayout layout = new MigLayout("insets 0,fill", "[c]");

            jContentPane = new JPanel();
            jContentPane.setLayout(layout);
            jContentPane.add(getJTextField(), "wrap, w 360px!");
            jContentPane.add(getAwesomeProposalList(), "grow, wrap, width ::400");
            jContentPane.add(getDetailPanel(), "grow, wrap");
        }
        return jContentPane;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getJTextField() {
        if (jTextField == null) {
            jTextField = new JTextField();
            jTextField.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            // Perform Action
                            new AwesomeAction(awesome, getAwesomeProposalList().getSelectedProposal());
                        } else if (e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
                            awesome.requestProposal(getJTextField().getText().substring(0, getJTextField().getCaretPosition()) + e.getKeyChar() + getJTextField().getText().substring(getJTextField().getCaretPosition()));
                        } else {
                            if (getJTextField().getCaretPosition() > 0) {
                                awesome.requestProposal(getJTextField().getText().substring(0, getJTextField().getCaretPosition() - 1) + getJTextField().getText().substring(getJTextField().getCaretPosition()));
                            }
                        }
                    } else {
                        switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            // Change Selection, go down!
                            try {
                                getAwesomeProposalList().getModel().countLock.readLock().lock();
                                if (getAwesomeProposalList().getSelectedIndex() != getAwesomeProposalList().getModel().getSize() - 1) {
                                    getAwesomeProposalList().setSelectedIndices(new int[] { getAwesomeProposalList().getSelectedIndex() + 1 });
                                    getDetailPanel().updateDetailPanel();
                                }
                            } finally {
                                getAwesomeProposalList().getModel().countLock.readLock().unlock();
                            }
                            break;
                        case KeyEvent.VK_UP:
                            // Change Selection, go up!
                            if (getAwesomeProposalList().getSelectedIndex() > 0) {
                                getAwesomeProposalList().setSelectedIndices(new int[] { getAwesomeProposalList().getSelectedIndex() - 1 });
                                getDetailPanel().updateDetailPanel();
                            }
                            break;
                        }
                    }
                }
            });
        }
        return jTextField;
    }

    /**
     * This method initializes jList
     * 
     * @return javax.swing.JList
     */
    private AwesomeProposalJList getAwesomeProposalList() {
        if (proposalList == null) {
            ListSelectionModel selectionModel = new AwesomeProposalListSelectionModel();
            AwesomeProposalListModel listModel = new AwesomeProposalListModel(awesome, selectionModel);
            proposalList = new AwesomeProposalJList(listModel, selectionModel);
        }
        return proposalList;
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private AwesomeProposalDetailPanel getDetailPanel() {
        if (detailPanel == null) {
            detailPanel = new AwesomeProposalDetailPanel(getAwesomeProposalList());
            detailPanel.setLayout(new MigLayout("insets 0,fill"));
            getAwesomeProposalList().getSelectionModel().addListSelectionListener(detailPanel);
        }
        return detailPanel;
    }

} // @jve:decl-index=0:visual-constraint="10,10"
