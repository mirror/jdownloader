package org.jdownloader.captcha.v2.solver;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import jd.controlling.captcha.CaptchaSettings;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class CaptchaResolutorCaptchaSolver extends ChallengeSolver<String> {

    private CaptchaResolutorCaptchaSettings            config;

    private static final CaptchaResolutorCaptchaSolver INSTANCE                  = new CaptchaResolutorCaptchaSolver();
    // Data conect
    private static Socket                              sk;
    private static int                                 PORT                      = 5000;
    private static DataOutputStream                    dos;
    private static ObjectOutputStream                  exit;
    private static DataInputStream                     dis;
    // Literals
    private static final String                        _ADDRESS_SEPARATOR        = "/";
    private static final String                        _REQUEST                  = "peticion";
    private static final String                        _SEPARATOR                = "#";
    public static final String                         NEGATIVE_CREDIT           = "creditoNegativo";
    public static final String                         YOUR_CREDIT_HAS_BEEN_SOLD = "Your credit is exhausted.";
    public static final String                         WRONG_PASSWORD            = "password erroneous";
    public static final String                         ERROR_INVALID_PASSWORD    = "Error, no valid password.";
    public static final String                         SERVER_PRODUCTION         = "ks359250.kimsufi.com";

    public static CaptchaResolutorCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private CaptchaResolutorCaptchaSolver() {
        super(1);
        config = JsonConfig.create(CaptchaResolutorCaptchaSettings.class);
        AdvancedConfigManager.getInstance().register(config);

    }

    @Override
    public boolean canHandle(Challenge<?> c) {

        return config.isEnabled() && super.canHandle(c);
    }

    @Override
    public void solve(final SolverJob<String> job) throws InterruptedException, SolverException {
        if (StringUtils.isEmpty(config.getUser()) || StringUtils.isEmpty(config.getPass())) return;
        if (job.getChallenge() instanceof BasicCaptchaChallenge) {
            job.waitFor(JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());
            checkInterruption();
            BasicCaptchaChallenge challenge = (BasicCaptchaChallenge) job.getChallenge();
            try {
                String address[] = ((String) InetAddress.getByName(SERVER_PRODUCTION).toString()).split(_ADDRESS_SEPARATOR);
                sk = new Socket(address[1], PORT);
                dos = new DataOutputStream(sk.getOutputStream());
                dis = new DataInputStream(sk.getInputStream());
                dos.writeUTF(_REQUEST + _SEPARATOR + config.getUser() + _SEPARATOR + config.getPass());
                String answerServer = dis.readUTF();
                if (answerServer.equalsIgnoreCase(WRONG_PASSWORD)) {
                    JOptionPane.showMessageDialog(null, ERROR_INVALID_PASSWORD); // pending
                } else {
                    if (answerServer.equalsIgnoreCase(NEGATIVE_CREDIT)) {
                        JOptionPane.showMessageDialog(null, YOUR_CREDIT_HAS_BEEN_SOLD); // pending
                    } else {
                        job.getLogger().info("Ask");
                        // Captcha image is sent.
                        exit = new ObjectOutputStream(sk.getOutputStream());
                        BufferedImage bufferedImage = ImageIO.read(challenge.getImageFile());
                        ByteArrayOutputStream salidaImagen = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "jpg", salidaImagen);
                        byte[] bytesImagen = salidaImagen.toByteArray();
                        exit.writeObject(bytesImagen);
                        exit.flush();

                        // captcha code is received in String ret.
                        String ret = dis.readUTF();
                        job.getLogger().info("Answer " + ret);

                        Thread.sleep(1000);

                        job.addAnswer(new CaptchaResponse(challenge, this, ret, 100));
                    }
                }
            } catch (IOException e) {
                job.getLogger().log(e);
            } finally {
                try {
                    sk.close();
                } catch (final Throwable e) {
                }
            }

        }

    }
}
