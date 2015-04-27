$.extend($.easing,
{
    easeInBack: function (x, t, b, c, d, s) {
        if (s == undefined) s = 1.70158;
        return c*(t/=d)*t*((s+1)*t - s) + b;
    },
    easeOutBack: function (x, t, b, c, d, s) {
        if (s == undefined) s = 1.70158;
        return c*((t=t/d-1)*t*((s+1)*t + s) + 1) + b;
    }
});/* Plax version 1.3.1 */

/*
 * Copyright (c) 2011 Cameron McEfee
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

(function ($) {

  var maxfps             = 25,
      delay              = 1 / maxfps * 1000,
      lastRender         = new Date().getTime(),
      layers             = [],
      plaxActivityTarget = $(window),
      motionMax          = 1,
      motionStartX       = null,
      motionStartY       = null,
      ignoreMoveable     = false

  // Public Methods
  $.fn.plaxify = function (params){
    return this.each(function () {
      var layerExistsAt = -1
      var layer         = {
        "xRange": $(this).data('xrange') || 0,
        "yRange": $(this).data('yrange') || 0,
        "invert": $(this).data('invert') || false,
        "background": $(this).data('background') || false
      }

      for (var i=0;i<layers.length;i++){
        if (this === layers[i].obj.get(0)){
          layerExistsAt = i
        }
      }

      for (var param in params) {
        if (layer[param] == 0) {
          layer[param] = params[param]
        }
      }

      layer.inversionFactor = (layer.invert ? -1 : 1) // inversion factor for
                                                        // calculations

      // Add an object to the list of things to parallax
      layer.obj    = $(this)
      if(layer.background) {
        // animate using the element's background
        pos = (layer.obj.css('background-position') || "0px 0px").split(/ /)
        if(pos.length != 2) {
          return
        }
        x = pos[0].match(/^((-?\d+)\s*px|0+\s*%|left)$/)
        y = pos[1].match(/^((-?\d+)\s*px|0+\s*%|top)$/)
        if(!x || !y) {
          // no can-doesville, babydoll, we need pixels or top/left as initial
            // values (it mightbe possible to construct a temporary image from
            // the background-image property and get the dimensions and run some
            // numbers, but that'll almost definitely be slow)
          return
        }
        layer.startX = x[2] || 0
        layer.startY = y[2] || 0
      } else {

        // Figure out where the element is positioned, then reposition it from
        // the top/left
        var position = layer.obj.position()
        layer.obj.css({
          'top'   : position.top,
          'left'  : position.left,
          'right' :'',
          'bottom':''
        })
        layer.startX = this.offsetLeft
        layer.startY = this.offsetTop
      }

      layer.startX -= layer.inversionFactor * Math.floor(layer.xRange/2)
      layer.startY -= layer.inversionFactor * Math.floor(layer.yRange/2)
      if(layerExistsAt >= 0){
        layers.splice(layerExistsAt,1,layer)
      } else {
        layers.push(layer)
      }
      
    })
  }

  // Determine if the device has an accelerometer
  //
  // returns true if the browser has window.DeviceMotionEvent (mobile)
  function moveable(){
    return (ignoreMoveable==true) ? false : window.DeviceOrientationEvent != undefined
  }

  // The values pulled from the gyroscope of a motion device.
  //
  // Returns an object literal with x and y as options.
  function valuesFromMotion(e) {
    x = e.gamma
    y = e.beta

    // Swap x and y in Landscape orientation
    if (Math.abs(window.orientation) === 90) {
      var a = x;
      x = y;
      y = a;
    }

    // Invert x and y in upsidedown orientations
    if (window.orientation < 0) {
      x = -x;
      y = -y;
    }

    motionStartX = (motionStartX == null) ? x : motionStartX
    motionStartY = (motionStartY == null) ? y : motionStartY

    return {
      x: x - motionStartX,
      y: y - motionStartY
    }
  }

  // Move the elements in the `layers` array within their ranges,
  // based on mouse or motion input
  //
  // Parameters
  //
  // e - mousemove or devicemotion event
  //
  // returns nothing
  function plaxifier(e) {
    if (new Date().getTime() < lastRender + delay) return
      lastRender = new Date().getTime()
    var leftOffset = (plaxActivityTarget.offset() != null) ? plaxActivityTarget.offset().left : 0,
        topOffset  = (plaxActivityTarget.offset() != null) ? plaxActivityTarget.offset().top : 0,
        x          = e.pageX-leftOffset,
        y          = e.pageY-topOffset

    if (
      x < 0 || x > plaxActivityTarget.width() ||
      y < 0 || y > plaxActivityTarget.height()
    ) return

    if(moveable()){
      if(e.gamma == undefined){
        ignoreMoveable = true
        return
      }
      values = valuesFromMotion(e)

      // Admittedly fuzzy measurements
      x = values.x / 30
      y = values.y / 30
    }

    var hRatio = x/((moveable() == true) ? motionMax : plaxActivityTarget.width()),
        vRatio = y/((moveable() == true) ? motionMax : plaxActivityTarget.height()),
        layer, i

    for (i = layers.length; i--;) {
      layer = layers[i]
      newX = layer.startX + layer.inversionFactor*(layer.xRange*hRatio)
      newY = layer.startY + layer.inversionFactor*(layer.yRange*vRatio)
      if(layer.background) {
        layer.obj.css('background-position', newX+'px '+newY+'px')
      } else {
        layer.obj
          .css('left', newX)
          .css('top', newY)
      }
    }
  }

  $.plax = {
    // Begin parallaxing
    //
    // Parameters
    //
    // opts - options for plax
    // activityTarget - optional; plax will only work within the bounds of this
    // element, if supplied.
    //
    // Examples
    //
    // $.plax.enable({ "activityTarget": $('#myPlaxDiv')})
    // # plax only happens when the mouse is over #myPlaxDiv
    //
    // returns nothing
    enable: function(opts){
      $(document).bind('mousemove.plax', function (e) {
        if(opts){
          plaxActivityTarget = opts.activityTarget || $(window)
        }
        plaxifier(e)
      })

      if(moveable()){
        window.ondeviceorientation = function(e){plaxifier(e)}
      }

    },

    // Stop parallaxing
    //
    // Examples
    //
    // $.plax.disable()
    // # plax no longer runs
    //
    // $.plax.disable({ "clearLayers": true })
    // # plax no longer runs and all layers are forgotten
    //
    // returns nothing
    disable: function(opts){
      $(document).unbind('mousemove.plax')
      window.ondeviceorientation = undefined
      if (opts && typeof opts.clearLayers === 'boolean' && opts.clearLayers) layers = []
    }
  }

  if (typeof ender !== 'undefined') {
    $.ender($.fn, true)
  }

})(function () {
  return typeof jQuery !== 'undefined' ? jQuery : ender
}());$(document).ready(function () {
    if($("#home").length > 0)
    {
        
        function scrollTop(){
            // IE8 + box-sizing: border-box; = fail.
            return Math.max($('html').scrollTop(), $(window).scrollTop());
        }
        
        function scrollTo(speed,element){
            var pos = element.offset().top;
            // if($("html").hasClass("lt-ie9"))
            // return true;
            $("html, body").animate(
                    { scrollTop: pos},
                    (speed != null && typeof(speed)==='string') ? speed : "normal");            
            return false;
        }
        scrollToFeatures = function(speed){
            return scrollTo(speed,$("#features"));
        };

        scrollToFeatureList = function(speed){
            return scrollTo(speed,$("#features-list"));
        };
        scrollToDownload = function(speed){
            return scrollTo(speed,$("#home"));
        };

        $('#mainnav').find('a[href="#features"]').click(scrollToFeatures);
        $('#features-teaser').find('a[href="#features"]').click(scrollToFeatureList);
        $('#backtodownload').click(scrollToDownload);
        
        var m = /(iPad|iPhone|Android|mobile)/.test(navigator.platform+navigator.userAgent) ? 5 : 1;
        $('#plax').find('img')
        .filter("#plax-1").plaxify({xRange: m*6*1.0, yRange: m*3*1.0, invert: true}).end()
        .filter("#plax-2").plaxify({xRange: m*6*2.0, yRange: m*3*2.0}).end()
        .filter("#plax-3").plaxify({xRange: m*6*4.5, yRange: m*3*4.5}).end()
        .filter("#plax-4").plaxify({xRange: m*6*7.0, yRange: m*3*7.0}).end()
        .filter("#plax-5").plaxify({xRange: m*6*9.5, yRange: m*3*9.5}).end();
        $.plax.enable(/* { "activityTarget": $('#introduction')} */);
        
        /*
         * var thirdFeature = $("#features-list").children(".feature").eq(2);
         * var scrollDownBanner = $("#scrolldownbanner");
         * $(window).scroll(function () { //Robot animation //scroll-down-button
         * var pos = ($(window).scrollTop() + $(window).height()) -
         * thirdFeature.offset().top;
         * scrollDownBanner.css("margin-left","-"+Math.max(0,pos)+"px");
         * scrollDownBanner.css("opacity",1-Math.min(1,pos/300)) });
         */
        
        
        var slideOut = $("#slideOut");
        var mediumblue = $("#home").find(".bg-bluemedium");
        var downloadSelection = $("#download-other-2");
        function closeSlideOut(callback){
            if(!slideOut.is(":visible"))
                return callback();
            closeDownloadSelection();
            mediumblue.removeClass("noshadow");
            slideOut.slideUp("slow","easeInBack",function(){
                slideOut.find(".panel").hide();
                $.isFunction(callback) && callback();
            });
        }
        
        function openSlideOut(element){
            element = $(element);
            if(element.is(":visible"))
                return;
            if(slideOut.is(":visible"))
                return closeSlideOut(function(){openSlideOut(element);});
            element.show();
            mediumblue.addClass("noshadow");
            slideOut.slideDown("slow","easeOutBack", function(){
                var pos = slideOut.offset().top;
                var x = pos + slideOut.height() - $(window).height();
                // console.log(slideOut[0].offsetTop," ",slideOut.offset().top);
                // console.log(pos,x,scrollTop());
                // console.log($(window).scrollTop(),"
                // ",$(document).scrollTop()," ",$('html').scrollTop(),"
                // ",$('body').scrollTop());
                if( scrollTop() < x ) 
                    $("html, body").animate({scrollTop: x},'normal');
            });
            
        }
        
        function getOS(){
            if(navigator.platform.indexOf("Win") >= 0)
                {
                    return "windows";
                    console.log("Windows detected");
                }
            else if(navigator.platform.indexOf("Linux") >= 0)
                {
                    return "linux";
                    console.log("Linux detected");
                }
            else if(navigator.platform.indexOf("Mac") >= 0)
                {
                    return "mac";
                    console.log("MacOS detected");
                }
            else if(navigator.platform.indexOf("iPad") >= 0)
                {
                    return "iDevice";
                    console.log("iPad detected");
                }
            else if(navigator.platform.indexOf("iPhone") >= 0)
                {
                    return "iDevice";
                    console.log("iPhone detected");
                }
            else if(navigator.platform.indexOf("iPod") >= 0)
                {
                    return "iDevice";
                    console.log("iPod detected");
                }
            else if(navigator.platform.indexOf("android") >= 0)
                {   
                    return "android";
                    console.log("android detected");
                }
            else 
                {
                    return "windows";// fallback
                    console.log("no OS detected - windows fallback");
                }
        }
        
        function getBrowser(){
            if(navigator.userAgent.indexOf("Firefox") >= 0)
                return "firefox";
            else if(navigator.userAgent.indexOf("Chrome") >= 0)
                return "chrome";
            else if(navigator.userAgent.indexOf("Opera") >= 0)
                return "opera";
            else if(navigator.userAgent.indexOf("Apple") >= 0)
                return "safari";
            else if(navigator.userAgent.indexOf("MSIE") >= 0)
                return "ie";
            return "windows";// fallback
        }

        
        var os = getOS();
        // console.log(os);
        // console.log("OS: " + os);
        var browser = getBrowser();
        // console.log("browser: " + browser);
        
        if (os == "iDevice"){
            var iDeviceDownload = $(".iDeviceDownload");
            iDeviceDownload.show();
        }
        else if (os == "android"){
            var androidDownload =$(".androidDownload");
            androidDownload.show();
        }
        else{
        var stepsToShowBlock = $(".browser"+"."+os+"."+browser);
        var stepsToShowInlineBlock = $("#install-helper").find("div").filter("."+os+".os-1, "+"."+os+".os-2");
        var downloadToShow = $(".downloadCardOS"+"."+os);
        
        stepsToShowBlock.show();
        stepsToShowInlineBlock.css('display', 'inline-block');
        downloadToShow.css('display', 'inline-block');
        $(".adsbygoogle").css('display', 'inline-block');
        }
        
        function getDownloadLinkEnding() {
                if(os === "windows")
                    return "_x64.exe";
                else if(os === "linux")
                    return "_x64.sh";
                else if(os === "mac")
                    return ".dmg";
                return "windows";// fallback
        }
        
        var downloadLinkEnding = getDownloadLinkEnding();
        
        // $(".download").attr("href",
        // "http://installer.jdownloader.org/JD2SilentSetup" +
        // downloadLinkEnding);
                
        $(".download").click(function(){
            openSlideOut("#install-helper");
            return true;
        });
        $(".download-other-os").click(function(){
            openSlideOut("#download-other");
            return false;
        });
        
        $(".ad-free-installer").click(function(){
            openSlideOut("#ad-free-installer");
            return false;
        });

        function closeDownloadSelection(callback,exclude){
            downloadSelection.children("div:visible").not(exclude).slideUp("normal")
            .promise().done(callback);
        }
        
        $("#download-other").children(".linux").find("a").click(function(){
            var target = $($(this).attr("href"));
            closeDownloadSelection(function(){
                target.slideDown("normal");
            },target);
            return false;
        });
        $(".closebutton").click(closeSlideOut);
        
        if(location.hash.indexOf("features") >= 0)
            $(window).load(function(){window.setTimeout(function(){scrollToFeatures("slow");},300);});
        if(location.hash.indexOf("download") >= 0)
            $(".download").click();
    }

    if($("#development").length > 0)
    {

        scrollToQuickstart = function(speed){
            return scrollTo(speed,$("#quickstart"));
        };
        $('#development').find('a[href="#quickstart"]').click(scrollToQuickstart);

        scrollToTranslation = function(speed){
            return scrollTo(speed,$("#translation"));
        };
        $('#development').find('a[href="#translation"]').click(scrollToTranslation);

        scrollToDlcapi = function(speed){
            return scrollTo(speed,$("#dlcapi"));
        };
        $('#development').find('a[href="#dlcapi"]').click(scrollToDlcapi);
    }

    if($("#support").length > 0)
    {

        scrollToKnowledgebase = function(speed){
            return scrollTo(speed,$("#menucontainer"));
        };
        $('#support').find('a[href="#menucontainer"]').click(scrollToKnowledgebase);

        scrollToRouter = function(speed){
            return scrollTo(speed,$("#howtoreconnectrouter"));
        };
        $('#support').find('a[href="#howtoreconnectrouter"]').click(scrollToRouter);

        scrollToCable = function(speed){
            return scrollTo(speed,$("#howtoreconnectcable"));
        };
        $('#support').find('a[href="#howtoreconnectcable"]').click(scrollToCable);

        scrollToModem = function(speed){
            return scrollTo(speed,$("#howtoreconnectmodem"));
        };
        $('#support').find('a[href="#howtoreconnectmodem"]').click(scrollToModem);


    }

    
    // Google Analytics for the Download Button
    
    
    $('.download').on('click', function() {
          ga('send', 'event', 'button', 'click', 'download');
          console.log("download-button event sent");
        });
    
    $('.download-other-os').on('click', function() {
          ga('send', 'event', 'button', 'click', 'other-os');
          console.log("other-os event sent");
        });
    
    $('.ad-free-installer').on('click', function() {
          ga('send', 'event', 'button', 'click', 'ad-free-installer');
          console.log("ad-free-installer event sent");
        });
        
    $('#backtodownload').on('click', function() {
          ga('send', 'event', 'button', 'click', 'back-to-download');
          console.log("back to download event sent");
        });

});

$(function(){
    if($("#news").length > 0) {
        
        $("#news").find("h2").not(".ignoreyellow").filter(":even").addClass("yellow");
        
        var loaded = false;
        var sharebutton = $(".sharebutton");
        
        function showShareButtons(){
            if(!loaded)
            {
                loaded = true;
                $(this).text(":-)");
                $.when(
                    $.getScript("//platform.twitter.com/widgets.js"),
                    $.getScript("https://apis.google.com/js/plusone.js"),
                    $.getScript("//connect.facebook.net/de_DE/all.js#xfbml=1&appId=371203709564816",function(){
                        FB.XFBML.parse();
                    })
                ).then(showShareButtons.bind(this),showShareButtons.bind(this));
                
            } else {
                $(this).text("Share");
                $(this).parents(".post").find(".sharebuttons:hidden").show();
            }
        }
        
        sharebutton.click(showShareButtons);
        
    }
});$(document).ready(function () {
    if($("#news").length > 0)
    {
        var m = /(iPad|iPhone|Android|mobile)/.test(navigator.platform+navigator.userAgent) ? 5 : 1;
        $('#plax').find('img')
        .filter("#plax-1").plaxify({xRange: m*6*1.0, yRange: m*3*1.0, invert: true}).end()
        .filter("#plax-2").plaxify({xRange: m*6*2.0, yRange: m*3*2.0}).end()
        .filter("#plax-3").plaxify({xRange: m*6*4.5, yRange: m*3*4.5}).end()
        .filter("#plax-4").plaxify({xRange: m*6*7.0, yRange: m*3*7.0}).end()
        .filter("#plax-5").plaxify({xRange: m*6*9.5, yRange: m*3*9.5}).end();
        $.plax.enable(/* { "activityTarget": $('#introduction')} */);
    }
});$(function(){
    $("#prettify").click(function(){$(this).slideUp();prettyPrint();});
});$(function(){
    if($("#support").length > 0){
        var menuCategories = $("#knowledgebase").children(".categories").children("div");
        var current = "mostpopular";
        var navLinks = $("#knowledgebase").children("nav").find("a");
        navLinks.click(function(){
            var cat = $(this).attr("href").replace(/^#/,"");
            if(cat===current)
                return;
            current = cat;
            
            navLinks.filter(".active").removeClass("active");
            $(this).addClass("active");
            menuCategories.filter(":visible").hide('normal');
            menuCategories.filter("."+cat).show('normal');
            return false;
        });
        $(document).on("click",".categories a",function(){
            var to = $($(this).attr("href")).offset().top;
            $("html, body").animate({scrollTop: to},'normal');
        })
        
        $(document).on("keydown",function(e) {
            console.log(e);
              switch(e.keyCode) { 
                 // User pressed "up" arrow
                 case 38:
                     navLinks.filter(".active").prev().click();
                     return false;
                 break;
                 case 40:
                     navLinks.filter(".active").next().click();
                     return false;
                 break;
              }
           });



        scrollToTranslation = function(speed){
            return scrollTo(speed,$("#translation"));
        };
        $('#support').find('a[href="#translation"]').click(scrollToTranslation);

        scrollToRouter = function(speed){
            return scrollTo(speed,$("#howtoreconnectrouter"));
        };
        $('#support').find('a[href="#howtoreconnectrouter"]').click(scrollToRouter);
        
    }
});$(function(){
    var latesttweet = $("#latesttweet");
    if(latesttweet.length > 0)
    {
        var bird = latesttweet.find(".bird");
        latesttweet.on("mouseleave",function(){
            bird.css("opacity",0).animate({opacity:1},1000);
        });
    }
});$(function() {
    var loginForm = $("#login");
    var loginButton = loginForm.find(".loginbutton");
    
    var showLoginError = function(){
        loginForm.find("progress").stop(true,true).slideUp("fast");
        loginForm.find(".loginfailed").slideDown("normal");
        window.setTimeout(function(){
            loginForm.find(".loginfailed").stop(true,true).slideUp("fast");
            loginButton.attr('disabled',false);
        },3000);
        
    };
    
    loginButton.click(function(){
        loginButton.attr('disabled',true);

        loginForm.find("progress").stop(true,true).slideDown("fast")
        loginForm.find(".loginfailed").stop(true,true).slideUp("fast");
        
        var data = {
                username: loginForm.find("#username").val(),
                password: loginForm.find("#password").val(),
                ajax: "true"
        };
        if(data.username == "" || data.password == "")
        {
            showLoginError();
            return false;
        }
        
        jQuery.post(loginForm.attr("action"), data, function(result){
            if(result == "true")
            {
                location.href = loginForm.find('input[name="redirect"]').val();;
            }
            else
                showLoginError();
         });
        return false;
    });
});$(document).ready(function () {
    $("nav").find("a").filter('[href$="'+location.pathname+location.search+'"]').addClass("current");
});