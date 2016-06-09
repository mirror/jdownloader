(function() {

	clickBox = function(num) {
		
		if(document.getElementsByClassName('rc-image-tile-target')[num].parentNode.className){
			return;
		}
		boxes = document.getElementsByClassName("rc-image-tile-target");
		console.log("Boxes: " + boxes.length);
		for (i = 0; i < boxes.length; i++) {
			if (i == num) {
				console.log("Click Box " + num + " : " + boxes[i]);
				boxes[i].click();
				return;
			}
		}

	}
	clickVerify = function() {

		var evt = document.createEvent("MouseEvents");
		evt.initEvent("mouseover", true, true);
		verifyButton = document.getElementById("recaptcha-verify-button");
		verifyButton.dispatchEvent(evt);
		console.log("Dispatch Click event on verify Button " + verifyButton);
		var evt = document.createEvent("MouseEvents");
		evt.initEvent("mousedown", true, true);
		verifyButton.dispatchEvent(evt);

		var evt = document.createEvent("MouseEvents");
		evt.initEvent("mouseup", true, true);
		verifyButton.dispatchEvent(evt);

	}
})()