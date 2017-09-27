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
         * File host script first used for: <a href="http://www.chomikuj.pl/">chomikuj.pl</a><br />
         * examples <a href="http:/diskokosmiko.mx/">diskokosmiko.mx</a>
         */
        ChomikujPlScript,
        /**
         * File host script: <a href="http://www.daddyscripts.com/products.php?name=daddys_file_host">Daddy's File Host</a><br />
         * example: <a href="http://dosya.tc/">dosya.tc</a>
         */
        DaddyScripts_FileHostV2,
        /**
         * Link protector script: <a href="http://www.daddyscripts.com/products.php?name=daddys_link_protector">Daddy's Link
         * Protector</a><br />
         * example: <a href="http://protect-link.org/">protect-link.org</a>
         */
        DaddyScripts_DaddysLinkProtector,
        /**
         * Open source image board: <a href="https://github.com/r888888888/danbooru">danbooru github project</a><br />
         * example: <a href="http://danbooru.donmai.us/">danbooru.donmai.us</a>
         */
        Danbooru,
        /**
         * Porn website/script: <a href="http://www.evilangel.com/">evilangel.com</a><br />
         * example: <a href="http://analacrobats.com/">analacrobats.com</a>
         */
        EvilAngelNetwork,
        /**
         * <a href="http://gempixel.com/project/premium-url-shortener/">Premium URL Shortener</a><br />
         * sold on <a href="http://codecanyon.net/item/premium-url-shortener/3688135">codecanyon.net</a><br />
         * examples <a href="http://cehs.ch/">cehs.ch</a> <a href="http://www.csurl.it">csurl.it</a>
         *
         */
        GemPixel_PremiumURLShortener,
        /**
         * Script used by some video/porn hosting sites.<br />
         * Can be bought e.g. from here: <a href="http://www.kernel-video-sharing.com/en/"</a> >kernel-video-sharing.com</a>.<br />
         * Demo: <a href="http://www.kvs-demo.com/">kvs-demo.com</a>
         */
        KernelVideoSharing,
        /**
         * Script used by some image hosting sites e.g.: <a href="http:/damimage.com/">damimage.com</a>. <br />
         * Can be bought e.g. from here:
         * <a href="http://codecanyon.net/item/imgshot-image-hosting-script/2558257" >http://codecanyon.net/item/imgshot-image-hosting-
         * script/2558257</a>.<br />
         * <b>Main decrypter class:</b> ImgShotDecrypt<br />
         * <b>Example that suits main decrypter class:</b> <a href="http:/imgshot.com/">imgshot.com</a><br />
         * <b>Example that does NOT suit main decrypter class (needs separate host class):</b> <a href="http:/imgbar.net/">imgbar.net</a>
         * <br />
         * Official Demo: <a href="http://imgshot.com/">imgshot.com</a><br />
         */
        ImageHosting_ImgShot,
        /**
         * Script used by some video hosting sites e.g.: <a href="http:/tn.com.ar/">tn.com.ar</a>. <br />
         * Can be bought e.g. from here: <a href="http://de.corp.kaltura.com/" >http://de.corp.kaltura.com/</a>.<br />
         */
        KalturaVideoPlatform,
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
         * <a href="http://ouo.io/">ouo.io</a> linkcryptor script, usually with reCaptchaV2 as protection. <a
         */
        OuoIoCryptor,
        /**
         * <a href="http://www.hostedtube.com/">hosted tube</a> porn script/template provided by
         * <a href="http://pimproll.com/">pimproll.com</a>
         */
        PimpRoll_HostedTube,
        /**
         * <a href="http://www.brazzers.com/">brazzers.com</a> porn script/template used for a lot of porn paysites.<br />
         * Example#2: <a href="http://babes.com/">babes.com</a><br />
         * They often modify it heavily which is why we usually have a separate plugin for each website.<br />
         */
        PornPortal,
        /**
         * MultiUpload script by unknown, called Qooy Mirrors (taken from paypal description) <a href="http://qooy.com/sale.php">Qooy
         * Mirrors</a>
         */
        Qooy_Mirrors,
        /**
         * Script used by some chinese file-hosting sites. <a href="http:/sx566.com/">sx566.com</a>. Not sure what to call this script.
         *
         * 'Copyright © 2016 百丽资源中心 版权所有 Powered By XDDrive Private V 1.4' http://vip.belle.la/ ?? xddrive private? couldnt find much on
         * google
         */
        Unknown_ChineseFileHosting,
        /**
         * Script used by some multihosters e.g. <a href="http://www.premiumax.net/">premiumax.net</a>. Not sure what to call this script.
         */
        Unknown_MultihosterScript,
        /**
         * the template that supports mirror stack type sites.. Not sure what to call this script.
         */
        Unknown_MirrorStack,
        /**
         * <b>Minimum requirements:</b> Usage of "flowplayer" <br />
         * <b>Additional requirements to be added to main class:</b> NO account support, offline via http code 404<br />
         * <b>Main host class:</b> UnknownPornScript1<br />
         * <b>Example that suits main host class:</b> <a href="http:/dansmovies.com/">dansmovies.com</a><br />
         * <b>Example that does NOT suit main host class (needs separate class):</b> -<br />
         * <b>Example that needs a decrypter class :</b> -<br />
         */
        UnknownPornScript1,
        /**
         * <b>Minimum requirements:</b> Website has to belong to that specified porn network/company <br />
         * <b>Additional requirements to be added to main class:</b> NO account support<br />
         * <b>Main host class:</b> UnknownPornScript2<br />
         * <b>Example that suits main host class:</b> <a href="http:/pornmaki.com/">pornmaki.com</a><br />
         * <b>Example that does NOT suit main host class (needs separate class):</b> -<br />
         * <b>Example that needs a decrypter class :</b> -<br />
         */
        UnknownPornScript2,
        /**
         * <b>Minimum requirements:</b> Website has to fit script <br />
         * <b>Additional requirements to be added to main class:</b> NO account support<br />
         * <b>Main host class:</b> UnknownPornScript3<br />
         * <b>Example that suits main host class:</b> <a href="http:/xxxkinky.com/">xxxkinky.com</a><br />
         * <b>Example that does NOT suit main host class (needs separate class):</b> -<br />
         * <b>Example that needs a decrypter class :</b> -<br />
         */
        UnknownPornScript3,
        /**
         * <b>Minimum requirements:</b> Website has to fit script <br />
         * <b>Additional requirements to be added to main class:</b> NO account support<br />
         * <b>Main host class:</b> UnknownPornScript4<br />
         * <b>Example that suits main host class:</b> <a href="http:/pornyeah.com/">pornyeah.com</a><br />
         * <b>Example that does NOT suit main host class (needs separate class):</b> <a href="http:/eroxia.com/">eroxia.com</a><br />
         * <b>Example that needs a decrypter class :</b> <a href="http:/eroxia.com/">eroxia.com</a><br />
         */
        UnknownPornScript4,
        /**
         * <b>Minimum requirements:</b> Usage of "jwplayer" <br />
         * <b>Additional requirements to be added to main class:</b> Usage of "jwplayer" - one of the 2 basic jwplayer js/html sources, NO
         * account support<br />
         * <b>Main host class:</b> UnknownPornScript5<br />
         * <b>Example that suits main host class:</b> <a href="http:/boyfriendtv.com/">boyfriendtv.com</a><br />
         * <b>Example that does NOT suit main host class [needs separate class]:</b> <a href="http:/pornhd.com/">pornhd.com</a><br />
         * <b>Example that needs a decrypter class :</b> -<br />
         */
        UnknownPornScript5,
        /**
         * <b>Minimum requirements:</b> Website has to fit script <br />
         * <b>Additional requirements to be added to main class:</b> NO account support<br />
         * <b>Main host class:</b> -<br />
         * <b>Example that suits main host class:</b> -<br />
         * <b>Example that does NOT suit main host class [needs separate class]:</b> <a href="http:/fux.com/">fux.com</a><br />
         * <b>Example that needs a decrypter class :</b> -<br />
         */
        UnknownPornScript6,
        /**
         * <b>Minimum requirements:</b> Website has to fit script <br />
         * <b>Additional requirements to be added to main class:</b> -<br />
         * <b>Main host class:</b> -<br />
         * <b>Example that suits main host class:</b> -<br />
         * <b>Example that does NOT suit main host class [needs separate class]:</b> NONE<br />
         * <b>Example that needs a decrypter class :</b> -<br />
         */
        UnknownPornScript7,
        /**
         * <b>Minimum requirements:</b> Website has to fit script <br />
         * <b>Additional requirements to be added to main class:</b> -<br />
         * <b>Main host class:</b> -<br />
         * <b>Example that suits main host class:</b> -<br />
         * <b>Example that does NOT suit main host class [needs separate class]:</b> NONE<br />
         * <b>Example that needs a decrypter class :</b> -<br />
         */
        UnknownPornScript8,
        /**
         * <b>Minimum requirements:</b> Website has to fit script <br />
         * <b>Main host class:</b> UnknownPornScript9<br />
         * <b>Examples that suit :</b> <a href="http:/nuvid.com/">nuvid.com</a><br />
         * , <a href="http:/hd21.com/">hd21.com</a><br />
         * <b>Example that does NOT suit main host class [needs separate class]:</b> <a href="http:/drtuber.com/">drtuber.com</a><br />
         * <br />
         * <b>Example that needs a decrypter class :</b> -<br />
         */
        UnknownPornScript9,
        /**
         * Script used by some video hosting sites. <a href="http://cloudy.ec/">cloudy.ec</a>. Not sure what to call this script.
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
        SafeLinking_SafeLinking,
        /**
         * Old link crypt script e.g. <a href="http://save-link.info">save-link.info</a> <br />
         * <b>Main decrypter class:</b> Zro10BasicDecrypt. <br />
         */
        Zero10BasicDecrypt,
        /**
         * <a href="https://shorte.st/">shorte.st</a> plugin, used to detect new/old domains that are using their service.
         */
        ShorteSt_ShorteSt,
        /**
         * <a href="https://adf.ly/">adf.ly</a> plugin, used to detect new/old domains that are using their service.
         */
        AdfLy_AdfLy,
        /**
         * URL shortening service provider. <br />
         * <a href="http://click.tf/services.html">A list of all supported domains</a>
         */
        URLShortnerLLP_URLShortner,
        /**
         * URL shortening or monetising platform by <a href="https://www.mightyscripts.com/">mightyscripts.com</a><br />
         * <a href="https://www.mightyscripts.com/downloads/adlinkfly-monetized-url-shortener/">AdLinkFly – Monetized URL
         * Shortener</a><br />
         * <a href="https://www.mightyscripts.com/downloads/mighty-url-shortener-short-url-script/">Mighty URL Shortener | Short URL
         * Script</a><br />
         * They seem to share the same HTML frame work.
         */
        MightyScript_AdLinkFly,
        MightyScript_MightyURLShortener,
        /**
         * <a href="https://www.abdulibrahim.com/url-shortener-with-ads-and-powerful-admin-panel/">URL Shortener with Ads and Powerful Admin
         * Panel</a><br />
         * <a href="http://abdulibrahim.com/short/">demo site</a><br />
         * <br />
         * common traits <br />
         * top panel 1/9th screen (site title left, right(menu) FIXED<br />
         * middle panel 5/9th screen (sitename + add link centred) <br />
         * bottom panel 2/9th screen (left, middle social media , right<br />
         * bottom bottom 0.8/9th screen ( copy right centred)<br />
         */
        AbdulIbrahim_URLShortener,
        /**
         * <a href="https://codecanyon.net/item/url-shortener-script-with-statistics/4461940">URL Shortener Script with Statistics @
         * codecanyon.net market place</a> & <a href="https://codepulsar.com/short1/index.php?a=short">demo site</a> <br />
         *
         */
        PricopAlexandru_URLShorterner1,
        /**
         * <a href="https://codecanyon.net/item/advanced-php-url-shortener/3450738">Advanced PHP URL Shortener @ codecanyon.net market
         * place</a> & <a href="https://codepulsar.com/short2/index.php?a=short">demo site</a> <br/>
         * his user/profile name is Pricop, on demo site for url shortener it has his <a href="https://twitter.com/pricop2007">twitter
         * profile</a> mentioned (also profile picture on twitter is the same as codecanyon). In which his
         * <a href="https://pricop.info/">personal website</a> is then linked <br />
         * common traits<br />
         * chain link image top left in title bar next to site name.
         */
        PricopAlexandru_URLShorterner2,
        /**
         * do not use, raztoki will set as required.
         */
        NotApplicable,
        /**
         * typically word press sites have common traits like file paths(css/plugins/js/etc).
         */
        WordPress_Wordpress;
    }
}
