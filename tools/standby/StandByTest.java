package standby;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class StandByTest {
    /**
     * TODO: Get the StandByDetector.dll in the java.library.path!
     */
    public static void main(String args[]) {
        System.out.println(System.getProperty("java.library.path"));
        StandByDetector sd = new StandByDetector(new StandByRequestListener() {
            public void standByRequested() {
                System.out.println("standby requested");
            }
        });
        sd.setAllowStandby(false);
        JFrame f = new JFrame();
        f.getContentPane().add(new JLabel("Close to end test"));
        f.setSize(300, 100);
        f.setResizable(false);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}
