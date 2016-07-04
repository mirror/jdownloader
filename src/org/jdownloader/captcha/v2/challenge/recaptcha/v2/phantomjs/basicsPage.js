(function() {
	click = function(el) {
		var ev = document.createEvent("MouseEvent");
		ev.initMouseEvent("click", true /* bubble */, true /* cancelable */, window, null, 23, 2, 0, 0, /* coordinates */
		false, false, false, false, /* modifier keys */
		0 /* left */, null);
		el.dispatchEvent(ev);

	}
	clickBox = function(num) {

		if (document.getElementsByClassName('rc-image-tile-target')[num].parentNode.className) {
			console.log("Already clicked: "+num);
			return;
		}
		boxes = document.getElementsByClassName("rc-image-tile-target");
		console.log("Boxes: " + boxes.length);
		for (i = 0; i < boxes.length; i++) {
			if (i == num) {
				console.log("Click Box " + num + " : " + boxes[i]);
				click(boxes[i]);
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