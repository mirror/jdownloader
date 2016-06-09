(function() {
	iframes = document.getElementsByTagName('iframe');
	if (iframes.length >= 1) {
		//check if we passed the test without images
		frameDoc = iframes[0].contentDocument;
		token = frameDoc.getElementById("recaptcha-token").value;
		if (token){
			return true;
		}
	}
	if (iframes.length >= 2) {
		//bubble opened
		frameDoc = iframes[1].contentDocument;

		if (frameDoc.getElementsByClassName("rc-image-tile-target").length > 0) {
			scripts = iframes[1].contentDocument.getElementsByTagName('script');
			for (i = 0; i < scripts.length; i++) {
				index = scripts[i].innerHTML.indexOf('recaptcha.frame.Main.init');
				if (index > -1) {
					return true;
				}
			}
		}
	}

	return false;
})()