package jd.plugins.components;

/**
 * used for defining template types. Use by testclass
 *
 * @author raztoki
 *
 */
public class SiteType {

    public static enum SiteTemplate {

        /**
         * Sold on: <a href="http://www.digitaldutch.com/arles/">digitaldutch.com</a><br />
         * examples <a href="http:/palcomix.com/">palcomix.com</a>
         */
        ArlesImageWebPageCreator,
        /**
         * <a href="http://gempixel.com/project/premium-url-shortener/">Premium URL Shortener</a><br />
         * sold on <a href="http://codecanyon.net/item/premium-url-shortener/3688135">codecanyon.net</a><br />
         * examples <a href="http://cehs.ch/">cehs.ch</a> <a href="http://www.csurl.it">csurl.it</a>
         *
         */
        GemPixel_PremiumURLShortener,
        /**
         * Script used by some image hosting sites. <a href="http:/damimage.com/">damimage.com</a>. <br />
         * Can be bought e.g. from here: <a href="http://codecanyon.net/item/imgshot-image-hosting-script/2558257"
         * >http://codecanyon.net/item/imgshot-image-hosting-script/2558257</a>.<br />
         * Demo: <a href="http://imgshot.com/">imgshot.com</a>
         */
        ImageHosting_ImgShot,
        /**
         * Script used by some video/porn hosting sites.<br />
         * Can be bought e.g. from here: <a href="http://www.kernel-video-sharing.com/en/"</a> >kernel-video-sharing.com</a>.<br />
         * Demo: <a href="http://www.kvs-demo.com/">kvs-demo.com</a>
         */
        KernelVideoSharing,
        /**
         * Should cover all given templates.<br />
         * <a href="http://sibsoft.net/xfilesharing.html">XFileSharing<a><br />
         * <a href="http://sibsoft.net/xfilesharing_free.html">XFileSharing FREE (old version)</a><br />
         * <a href="http://sibsoft.net/xvideosharing.html">XVideoSharing<a><br />
         * <a href="http://sibsoft.net/ximagesharing.html">XImageSharing<a><br />
         */
        SibSoft_XFileShare,

        /**
         * <a href="http://sibsoft.net/xlinker.html">XLinker</a>
         */
        SibSoft_XLinker,

        /**
         * <a href="https://mfscripts.com/yetishare/overview.html">YetiShare - PHP File Hosting Site Script</a>
         */
        MFScripts_YetiShare,

        /**
         * <a href="https://mfscripts.com/wurlie/overview.html">Wurlie - PHP Short Url Script</a>
         */
        MFScripts_Wurlie,

        /**
         * <a href="http://www.hostedtube.com/">hosted tube</a> porn script/template provided by <a
         * href="http://pimproll.com/">pimproll.com</a>
         */
        PimpRoll_HostedTube,

        /**
         * MultiUpload script by unknown, called Qooy Mirrors (taken from paypal description) <a href="http://qooy.com/sale.php">Qooy
         * Mirrors</a>
         */
        Qooy_Mirrors,
        /**
         * Script used by some chinese file-hosting sites. <a href="http:/sx566.com/">sx566.com</a>. Not sure what to call this script.
         */
        Unknown_ChineseFileHosting,
        /**
         * the template that supports mirror stack type sites.. Not sure what to call this script.
         */
        Unknown_MirrorStack,
        /**
         * Script used by some video hosting sites. <a href="http:/cloudy.ec/">cloudy.ec</a>. Not sure what to call this script.
         */
        Unknown_VideoHosting,
        /**
         * Turbobit hosted sites. <a href="http://turbobit.net/">turbobit.net</a>
         */
        Turbobit_Turbobit,

        /**
         * linkbucks hosted sites. <a href="http://linkbucks.com/">linkbucks.com</a>
         */
        Linkbucks_Linkbucks,

        /**
         * safelinking hosted sites <a href="http://safelinking.net/">safelinking.net</a>
         */
        SafeLinking_SafeLinking;

    }

}
