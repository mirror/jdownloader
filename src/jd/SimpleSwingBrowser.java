package jd;

import static javafx.concurrent.Worker.State.FAILED;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;

public class SimpleSwingBrowser extends JFrame {
    public class Handler extends URLStreamHandler {
        private final ClassLoader classLoader;

        public Handler() {
            this.classLoader = getClass().getClassLoader();
        }

        public Handler(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            URL resourceUrl = classLoader.getResource(u.getPath());
            if (resourceUrl == null) {
                throw new IOException("Resource not found: " + u);
            }

            return resourceUrl.openConnection();
        }
    }

    private final JFXPanel jfxPanel = new JFXPanel();
    private WebEngine      engine;

    private final JPanel panel     = new JPanel(new BorderLayout());
    private final JLabel lblStatus = new JLabel();

    private final JButton      btnGo       = new JButton("Go");
    private final JTextField   txtURL      = new JTextField();
    private final JProgressBar progressBar = new JProgressBar();

    public SimpleSwingBrowser() {
        super();
        initComponents();
    }

    private void initComponents() {
        createScene();

        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadURL(txtURL.getText());
            }
        };
        // new Thread()

        btnGo.addActionListener(al);
        txtURL.addActionListener(al);

        progressBar.setPreferredSize(new Dimension(150, 18));
        progressBar.setStringPainted(true);

        JPanel topBar = new JPanel(new BorderLayout(5, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        topBar.add(txtURL, BorderLayout.CENTER);
        topBar.add(btnGo, BorderLayout.EAST);

        JPanel statusBar = new JPanel(new BorderLayout(5, 0));
        statusBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        statusBar.add(lblStatus, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(jfxPanel, BorderLayout.CENTER);
        panel.add(statusBar, BorderLayout.SOUTH);

        getContentPane().add(panel);

        setPreferredSize(new Dimension(1024, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();

    }

    private void createScene() {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                WebView view = new WebView();
                engine = view.getEngine();
                engine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {

                    @Override
                    public void changed(ObservableValue<? extends State> observable, State oldValue, State newState) {
                        System.out.println(observable + " - " + oldValue + " -> " + newState);

                        if (newState == Worker.State.SUCCEEDED) {
                            org.w3c.dom.Document doc = engine.getDocument();
                            // try {
                            // Transformer transformer = TransformerFactory.newInstance().newTransformer();
                            // transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                            // transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                            // transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                            // transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                            // transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                            //
                            // transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(System.out, "UTF-8")));
                            // } catch (Exception ex) {
                            // ex.printStackTrace();
                            // }
                        }
                    }
                });
                engine.titleProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, final String newValue) {

                        System.out.println(observable + " - " + oldValue + " -> " + newValue);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                SimpleSwingBrowser.this.setTitle(newValue);
                            }
                        });
                    }
                });

                engine.setOnStatusChanged(new EventHandler<WebEvent<String>>() {
                    @Override
                    public void handle(final WebEvent<String> event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                lblStatus.setText(event.getData());
                            }
                        });
                    }
                });

                engine.locationProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> ov, String oldValue, final String newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                txtURL.setText(newValue);
                            }
                        });
                    }
                });

                engine.getLoadWorker().workDoneProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, final Number newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setValue(newValue.intValue());
                            }
                        });
                    }
                });

                engine.getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {

                    public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
                        if (engine.getLoadWorker().getState() == FAILED) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    JOptionPane.showMessageDialog(panel, (value != null) ? engine.getLocation() + "\n" + value.getMessage() : engine.getLocation() + "\nUnexpected error.", "Loading error...", JOptionPane.ERROR_MESSAGE);
                                }
                            });
                        }
                    }
                });

                jfxPanel.setScene(new Scene(view));
            }
        });
    }

    public void loadURL(final String url) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                String tmp = toURL(url);

                if (tmp == null) {
                    tmp = toURL("http://" + url);
                }

                engine.load(tmp);
            }
        });
    }

    private static String toURL(String str) {
        try {
            return new URL(str).toExternalForm();
        } catch (MalformedURLException exception) {
            return null;
        }
    }

    public static void main(String[] args) {
        // defaultFac=URL.
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            {
                // URL.class.getDeclaredMethod(", parameterTypes)
            }

            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                // if("file".equals(protocol)){
                return new sun.net.www.protocol.http.Handler();
                // }
                // return null;
            }
        });
        // URL.setURLSOtreamHandlerFactory(protocol -> {
        // if(protocol.equals("classpath")) {
        // return new Handler();
        // } else {
        // return null;
        // }
        // });

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                SimpleSwingBrowser browser = new SimpleSwingBrowser();
                browser.setVisible(true);
                browser.loadURL("http://oracle.com");
            }
        });
    }
}
