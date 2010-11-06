<?php
# anti-recaptcha v0.2f (c)opyleft 2010 http://wegeneredv.de/arc **************
# ****************************************************************************
# future fixes and changes:
# - gaining greater recognization rates by using wikipedia as wordlist
#    - see included paper 'antirecaptcha.pdf' for further information or
#      download it from http://wegeneredv.de/antirecaptcha.pdf
# - clean up source / replace repeating code by functions
# - convert to c++ using http://github.com/facebook/hiphop-php
# 
# ****************************************************************************
# Installation:
# copy directly to your jDownloader installation directory (e.g.
# c:\program files\jDowloader and extract.
# -runs only on Windows systems-
#
# ****************************************************************************
# Changelog:
#
# v0.2f
# - seperated both words by 100px
# - cleaned up directories - thx for the help @marc
#   (http://board.gulli.com/member/974202-marcelser/)
# - fixed empty outputs from tesseract, so no captcha popups anymore
# - updates from jDownloader should work again, no need to block them
#
# v0.2e
# - fixed some tesseract issues
#
# v0.2d
# - support for one-click hoster maintained, hotfile and others now work. you
#   have to ignore the updates of jDownloader, because i had to made changes
#   to the file 'outdated.dat'. if this file is changed, jD tries to download
#   an untampered version.
#
# v0.2c
# - applied selective contrast to both words
# - improved success rate to 17% by adding a bigger wordlist to tesseract
# - some hoster aren't supported any longer due to plugin errors caused by jD
#
# v0.2b
# - removed intersection cleaning due to ineffectivity
# - added experimental sharpening
# - changed from VietOCR to tesseract
#
# v0.2a
# - pushed recognization rate to ca. 10%
# - multiple iterations to ocr ('best match')
# - partially cleaned intersections between letters
# - cleaned up sourcecode
# - added support for the following one-click-hoster:
# combozip.com,cramit.in,crazyupload.com,drop.io,enterupload.com,extabit.com,
# filebling.com,filechip.com,filescloud.com,fileserve.com,filesmonster.com,
# filesonic.com,filestab.com,filestrack.com,filevo.com,freakshare.net,
# free-share.ru,hatlimit.com,hidemyass.com,hostingcup.com,maknyos.com,
# mediafire.com,putshare.com,quickupload.com,slingfile.com,tgf-services.com
# uploadfloor.com
#
# v0.1b
# - changed the directory structure
#
# v0.1
# - first version tested
#
# known bugs:
# - none ;)
#
# ****************************************************************************
# variables ******************************************************************
$debug_mode=true;
$enable_sharpen=true;
$shit="!§$%&@";
$white_columns_needed_for_separation=7;
$h2qx_commandline='hq2x.exe contrast.bmp resized.bmp';
$tesseract_commandline='tesseract.exe cropped.jpg tessoutput -l eng nobatch +arc';
$input_image=$argv[1];
$mid_exponential_smoothing_factor=0.15;
$vector_threshold=0.33;
$surrounding_pixel_threshold_min=7;
$surrounding_pixel_threshold_max=8;
$selective_contrast_threshold=1.5;
$contrast=127;
$word_seperator_pixel=100;
$crop_spacer_pixel=10;
# function imagesetpixel_rgb *************************************************
# ****************************************************************************
function imagesetpixel_rgb($im,$x_pos,$y_pos,$r,$g,$b){
	imagesetpixel($im,$x_pos,$y_pos,65536*$r+256*$g+$b);
}
# function readfile_chunked **************************************************
# ****************************************************************************
function readfile_chunked ($filename) {
	$chunksize = 1*(1024*1024); # how many bytes per chunk
	$buffer = '';
	$handle = fopen($filename, 'rb');
	if ($handle === false) {
		return false;
	}
	while (!feof($handle)) {
		$buffer = fread($handle, $chunksize);
	}
	fclose($handle);
	return $buffer;
}
# function imagecreatefrombmp ************************************************
# ****************************************************************************
# save Bitmap-File with GD library
# written by mgutt of http://www.programmierer-forum.de/function-imagecreatefrombmp-laeuft-mit-allen-bitraten-t143137.htm
# based on the function by DHKold of http://www.php.net/manual/de/function.imagecreate.php#53879
if (!function_exists('imagecreatefrombmp')) { function imagecreatefrombmp($filename) {
	# version 1.00
	if (!($fh = fopen($filename, 'rb'))) {
		trigger_error('imagecreatefrombmp: Can not open ' . $filename, E_USER_WARNING);
		return false;
	}
	# read file header
	$meta = unpack('vtype/Vfilesize/Vreserved/Voffset', fread($fh, 14));
	# check for bitmap
	if ($meta['type'] != 19778) {
		trigger_error('imagecreatefrombmp: ' . $filename . ' is not a bitmap!', E_USER_WARNING);
		return false;
	}
	# read image header
	$meta += unpack('Vheadersize/Vwidth/Vheight/vplanes/vbits/Vcompression/Vimagesize/Vxres/Vyres/Vcolors/Vimportant', fread($fh, 40));
	# read additional 16bit header
	if ($meta['bits'] == 16) {
		$meta += unpack('VrMask/VgMask/VbMask', fread($fh, 12));
	}
	# set bytes and padding
	$meta['bytes'] = $meta['bits'] / 8;
	$meta['decal'] = 4 - (4 * (($meta['width'] * $meta['bytes'] / 4)- floor($meta['width'] * $meta['bytes'] / 4)));
	if ($meta['decal'] == 4) {
		$meta['decal'] = 0;
	}
	# obtain imagesize
	if ($meta['imagesize'] < 1) {
		$meta['imagesize'] = $meta['filesize'] - $meta['offset'];
		# in rare cases filesize is equal to offset so we need to read physical size
		if ($meta['imagesize'] < 1) {
			$meta['imagesize'] = @filesize($filename) - $meta['offset'];
			if ($meta['imagesize'] < 1) {
				trigger_error('imagecreatefrombmp: Can not obtain filesize of ' . $filename . '!', E_USER_WARNING);
				return false;
			}
		}
	}
	# calculate colors
	$meta['colors'] = !$meta['colors'] ? pow(2, $meta['bits']) : $meta['colors'];
	# read color palette
	$palette = array();
	if ($meta['bits'] < 16) {
		$palette = unpack('l' . $meta['colors'], fread($fh, $meta['colors'] * 4));
		# in rare cases the color value is signed
		if ($palette[1] < 0) {
			foreach ($palette as $i => $color) {
				$palette[$i] = $color + 16777216;
			}
		}
	}
	# create gd image
	$im = imagecreatetruecolor($meta['width'], $meta['height']);
	$data = fread($fh, $meta['imagesize']);
	$p = 0;
	$vide = chr(0);
	$y = $meta['height'] - 1;
	$error = 'imagecreatefrombmp: ' . $filename . ' has not enough data!';
	# loop through the image data beginning with the lower left corner
	while ($y >= 0) {
		$x = 0;
		while ($x < $meta['width']) {
			switch ($meta['bits']) {
				case 32:
				case 24:
					if (!($part = substr($data, $p, 3))) {
						trigger_error($error, E_USER_WARNING);
						return $im;
					}
					$color = unpack('V', $part . $vide);
					break;
				case 16:
					if (!($part = substr($data, $p, 2))) {
						trigger_error($error, E_USER_WARNING);
						return $im;
					}
					$color = unpack('v', $part);
					$color[1] = (($color[1] & 0xf800) >> 8) * 65536 + (($color[1] & 0x07e0) >> 3) * 256 + (($color[1] & 0x001f) << 3);
					break;
				case 8:
					$color = unpack('n', $vide . substr($data, $p, 1));
					$color[1] = $palette[ $color[1] + 1 ];
					break;
				case 4:
					$color = unpack('n', $vide . substr($data, floor($p), 1));
					$color[1] = ($p * 2) % 2 == 0 ? $color[1] >> 4 : $color[1] & 0x0F;
					$color[1] = $palette[ $color[1] + 1 ];
					break;
				case 1:
					$color = unpack('n', $vide . substr($data, floor($p), 1));
					switch (($p * 8) % 8) {
						case 0:
							$color[1] = $color[1] >> 7;
							break;
						case 1:
							$color[1] = ($color[1] & 0x40) >> 6;
							break;
						case 2:
							$color[1] = ($color[1] & 0x20) >> 5;
							break;
						case 3:
							$color[1] = ($color[1] & 0x10) >> 4;
							break;
						case 4:
							$color[1] = ($color[1] & 0x8) >> 3;
							break;
						case 5:
							$color[1] = ($color[1] & 0x4) >> 2;
							break;
						case 6:
							$color[1] = ($color[1] & 0x2) >> 1;
							break;
						case 7:
							$color[1] = ($color[1] & 0x1);
							break;
					}
					$color[1] = $palette[ $color[1] + 1 ];
					break;
				default:
					trigger_error('imagecreatefrombmp: ' . $filename . ' has ' . $meta['bits'] . ' bits and this is not supported!', E_USER_WARNING);
					return false;
			}
			imagesetpixel($im, $x, $y, $color[1]);
			$x++;
			$p += $meta['bytes'];
		}
		$y--;
		$p += $meta['decal'];
	}
	fclose($fh);
	return $im;
}}
# function imagebmp **********************************************************
# ****************************************************************************
# create Bitmap-File with GD library
# written by mgutt of http://www.programmiererforum.de/imagebmp-gute-funktion-gefunden-t143716.htm
# based on the function by legend(legendsky@hotmail.com) of http://www.ugia.cn/?p=96
function imagebmp($im, $filename='', $bit=24, $compression=0) {
	if (!in_array($bit, array(1, 4, 8, 16, 24, 32))) {
		$bit = 24;
	}
	else if ($bit == 32) {
		$bit = 24;
	}
	$bits = pow(2, $bit);
	imagetruecolortopalette($im, true, $bits);
	$width = imagesx($im);
	$height = imagesy($im);
	$colors_num = imagecolorstotal($im);
	$rgb_quad = ''; 
	if ($bit <= 8) {
		for ($i = 0; $i < $colors_num; $i++) {
			$colors = imagecolorsforindex($im, $i);
			$rgb_quad .= chr($colors['blue']) . chr($colors['green']) . chr($colors['red']) . "\0";
		}
		$bmp_data = '';
		if ($compression == 0 || $bit < 8) {
			$compression = 0;
			$extra = ''; 
			$padding = 4 - ceil($width / (8 / $bit)) % 4;
			if ($padding % 4 != 0) {
				$extra = str_repeat("\0", $padding); 
			}
			for ($j = $height - 1; $j >= 0; $j --) {
				$i = 0;
				while ($i < $width) {
					$bin = 0;
					$limit = $width - $i < 8 / $bit ? (8 / $bit - $width + $i) * $bit : 0;
					for ($k = 8 - $bit; $k >= $limit; $k -= $bit) {
						$index = imagecolorat($im, $i, $j);
						$bin |= $index << $k;
						$i++;
					}
					$bmp_data .= chr($bin);
				}
				$bmp_data .= $extra;
			}
		}
		# RLE8
		else if ($compression == 1 && $bit == 8) {
			for ($j = $height - 1; $j >= 0; $j--) {
				$last_index = "\0";
				$same_num = 0;
				for ($i = 0; $i <= $width; $i++) {
					$index = imagecolorat($im, $i, $j);
					if ($index !== $last_index || $same_num > 255) {
						if ($same_num != 0) {
							$bmp_data .= chr($same_num) . chr($last_index);
						}
						$last_index = $index;
						$same_num = 1;
					}
					else {
						$same_num++;
					}
				}
				$bmp_data .= "\0\0";
			}
			$bmp_data .= "\0\1";
		}
		$size_quad = strlen($rgb_quad);
		$size_data = strlen($bmp_data);
	}
	else {
		$extra = '';
		$padding = 4 - ($width * ($bit / 8)) % 4;
		if ($padding % 4 != 0) {
			$extra = str_repeat("\0", $padding);
		}
		$bmp_data = '';
		for ($j = $height - 1; $j >= 0; $j--) {
			for ($i = 0; $i < $width; $i++) {
				$index  = imagecolorat($im, $i, $j);
				$colors = imagecolorsforindex($im, $index);
				if ($bit == 16) {
					$bin = 0 << $bit;
					$bin |= ($colors['red'] >> 3) << 10;
					$bin |= ($colors['green'] >> 3) << 5;
					$bin |= $colors['blue'] >> 3;
					$bmp_data .= pack("v", $bin);
				}
				else {
					$bmp_data .= pack("c*", $colors['blue'], $colors['green'], $colors['red']);
				}
			}
			$bmp_data .= $extra;
		}
		$size_quad = 0;
		$size_data = strlen($bmp_data);
		$colors_num = 0;
	}
	$file_header = 'BM' . pack('V3', 54 + $size_quad + $size_data, 0, 54 + $size_quad);
	$info_header = pack('V3v2V*', 0x28, $width, $height, 1, $bit, $compression, $size_data, 0, 0, $colors_num, 0);
	if ($filename != '') {
		$fp = fopen($filename, 'wb');
		fwrite($fp, $file_header . $info_header . $rgb_quad . $bmp_data);
		fclose($fp);
		return true;
	}
	echo $file_header . $info_header. $rgb_quad . $bmp_data;
	return true;
}
$source_img=imagecreatefromjpeg($input_image);
# contrast *******************************************************************
# ****************************************************************************
$source_img=imagecreatefromjpeg($input_image);
$height=imagesy($source_img);
$width=imagesx($source_img);
$first_word_start=0;
for($x=0;$x<$width;$x++){
	for($y=0;$y<$height;$y++){
		$c=imagecolorat($source_img,$x,$y);
		if($c==0){
			$first_word_start=$x;
		}
	}
	if($first_word_start>0){
		break;
	}
}
$first_word_end=0;
for($x=$first_word_start;$x<$width;$x++){
	$black_pixel_found=false;
	for($y=0;$y<$height;$y++){
		$c=imagecolorat($source_img,$x,$y);
		if($c==0){
			$black_pixel_found=true;
		}
	}
	if(!$black_pixel_found){
		$white_column_count++;
		if($white_column_count>$white_columns_needed_for_separation){
			$first_word_end=$x-$white_column_count;
		}
	}else{
		$white_column_count=0;
	}
	if($first_word_end>0){
		break;
	}
}
$second_word_start=0;
for($x=$first_word_end+$white_columns_needed_for_separation;$x<$width;$x++){
	for($y=0;$y<$height;$y++){
		$c=imagecolorat($source_img,$x,$y);
		if($c==0){
			$second_word_start=$x;
		}
	}
	if($second_word_start>0){
		break;
	}
}
$second_word_end=0;
for($x=$width-1;$x>$second_word_start;$x--){
	for($y=0;$y<$height;$y++){
		$c=imagecolorat($source_img,$x,$y);
		if($c==0){
			$second_word_end=$x;
		}
	}
	if($second_word_end>0){
		break;
	}
}
# selective contrast *********************************************************
$source_top=null;
$source_bottom=null;
for($x=1;$x<$width-1;$x++){
	for($y=0;$y<$height;$y++){
		$c=imagecolorat($source_img,$x,$y);
		if($c<8355840){
			$source_top[$x]=$y;
			break;
		}
	}
	for($y=$height-1;$y>0;$y--){
		$c=imagecolorat($source_img,$x,$y);
		if($c<8355840){
			$source_bottom[$x]=$y;
			break;
		}
	}
}
$word1_contrast=1;
$word2_contrast=1;
a:
$contrast_img=imagecreatefromjpeg($input_image);
for($x=$first_word_start;$x<$first_word_end;$x++){
	for($y=0;$y<$height;$y++){
		if(($y>=$source_top[$x])&&($y<=$source_bottom[$x])){
			$rgb=imagecolorat($source_img,$x,$y);
			$r=($rgb>>16)&0xFF;
			$g=($rgb>>8)&0xFF;
			$b=$rgb&0xFF;
			if(($r>$word1_contrast)&&($r>$word1_contrast)&&($r>$word1_contrast)){
				imagesetpixel($contrast_img,$x,$y,16777215);
			}else{
				imagesetpixel($contrast_img,$x,$y,0);
			}
		}
	}
}
for($x=$second_word_start;$x<$second_word_end;$x++){
	for($y=0;$y<$height;$y++){
		if(($y>=$source_top[$x])&&($y<=$source_bottom[$x])){
			$rgb=imagecolorat($source_img,$x,$y);
			$r=($rgb>>16)&0xFF;
			$g=($rgb>>8)&0xFF;
			$b=$rgb&0xFF;
			if(($r>$word2_contrast)&&($r>$word2_contrast)&&($r>$word2_contrast)){
				imagesetpixel($contrast_img,$x,$y,16777215);
			}else{
				imagesetpixel($contrast_img,$x,$y,0);
			}
		}
	}
}
$word1_black_pixel=0;
$word1_white_pixel=0;
for($x=$first_word_start;$x<$first_word_end;$x++){
	for($y=0;$y<$height;$y++){
		if(($y>=$source_top[$x]-1)&&($y<=$source_bottom[$x]+1)){
			$c=imagecolorat($contrast_img,$x,$y);
			if($c==0){
				$word1_black_pixel++;
			}else{
				$word1_white_pixel++;
			}
		}
	}
}
$word1_contrast_ratio=($word1_black_pixel/$word1_white_pixel);
$word2_black_pixel=0;
$word2_white_pixel=0;
for($x=$second_word_start;$x<$second_word_end;$x++){
	for($y=0;$y<$height;$y++){
		if(($y>=$source_top[$x]-1)&&($y<=$source_bottom[$x]+1)){
			$c=imagecolorat($contrast_img,$x,$y);
			if($c==0){
				$word2_black_pixel++;
			}else{
				$word2_white_pixel++;
			}
		}
	}
}
$word2_contrast_ratio=($word2_black_pixel/$word2_white_pixel);
if($word1_contrast_ratio<$selective_contrast_threshold){
	$word1_contrast++;
}
if($word2_contrast_ratio<$selective_contrast_threshold){
	$word2_contrast++;
}
if(($word1_contrast_ratio<$selective_contrast_threshold)||($word2_contrast_ratio<$selective_contrast_threshold)){
	imagedestroy($contrast_img);
	goto a;
}
imagedestroy($source_img);
imagebmp($contrast_img,'contrast.bmp');
# resize image ***************************************************************
# ****************************************************************************
//imagebmp($sharp_img,"contrast.bmp");
$h2qx_output=shell_exec($h2qx_commandline);
$img=imagecreatefrombmp('resized.bmp');
$height=imagesy($img);
$width=imagesx($img);
# sharpen ********************************************************************
# ****************************************************************************
$sharp_img=imagecreatetruecolor($width,$height);
imagecopy($sharp_img,$img,0,0,0,0,$width,$height);
imagedestroy($img);
	if($enable_sharpen){
		for($surrounding_pixel_threshold=$surrounding_pixel_threshold_max;$surrounding_pixel_threshold>$surrounding_pixel_threshold_min;$surrounding_pixel_threshold--){
		for($x=1;$x<$width-1;$x++){
			for($y=1;$y<$height-1;$y++){
				$surrounding_pixel_counter_black=0;
				$surrounding_pixel_counter_white=0;
				$c1=imagecolorat($sharp_img,$x-1,$y-1);
				$c2=imagecolorat($sharp_img,$x-1,$y);
				$c3=imagecolorat($sharp_img,$x-1,$y+1);
				$c4=imagecolorat($sharp_img,$x,$y-1);
				$c5=imagecolorat($sharp_img,$x,$y+1);
				$c6=imagecolorat($sharp_img,$x+1,$y-1);
				$c7=imagecolorat($sharp_img,$x+1,$y);
				$c8=imagecolorat($sharp_img,$x+1,$y+1);
				if($c1==0){
					$surrounding_pixel_counter_black++;
				}else{
					$surrounding_pixel_counter_white++;
				}
				if($c2==0){
					$surrounding_pixel_counter_black++;
				}else{
					$surrounding_pixel_counter_white++;
				}
				if($c3==0){
					$surrounding_pixel_counter_black++;
				}else{
					$surrounding_pixel_counter_white++;
				}
				if($c4==0){
					$surrounding_pixel_counter_black++;
				}else{
					$surrounding_pixel_counter_white++;
				}
				if($c5==0){
					$surrounding_pixel_counter_black++;
				}else{
					$surrounding_pixel_counter_white++;
				}
				if($c6==0){
					$surrounding_pixel_counter_black++;
				}else{
					$surrounding_pixel_counter_white++;
				}
				if($c7==0){
					$surrounding_pixel_counter_black++;
				}else{
					$surrounding_pixel_counter_white++;
				}
				if($c8==0){
					$surrounding_pixel_counter_black++;
				}
				if($surrounding_pixel_counter_black>=$surrounding_pixel_threshold){
					imagesetpixel($sharp_img,$x,$y,0);
				}
				if($surrounding_pixel_counter_white>=$surrounding_pixel_threshold){
					imagesetpixel($sharp_img,$x,$y,16777215);
				}
			}
		}
	}
}
# reaglignment ***************************************************************
# ****************************************************************************
$temp=@imagecreatetruecolor($width,$height) or die('Cannot Initialize new GD image stream');
for($x=0;$x<$width;$x++){
	for($y=0;$y<$height;$y++){
		imagesetpixel($temp,$x,$y,16777215);
	}
}
# calculate top and bottom ***************************************************
$top=null;
$bottom=null;
for($x=1;$x<$width-1;$x++){
	for($y=0;$y<$height;$y++){
		$c=imagecolorat($sharp_img,$x,$y);
		if($c==0){
			$top[$x]=$y;
			break;
		}
	}
	for($y=$height-1;$y>0;$y--){
		$c=imagecolorat($sharp_img,$x,$y);
		if($c==0){
			$bottom[$x]=$y;
			break;
		}
	}
}
# calculate mid **************************************************************
$mid=null;
$mid_1stpass=null;
$mid_2ndpass=null;
for($x=0;$x<$width;$x++){
	if($bottom[$x]-$top[$x]==0){
		$mid_1stpass[$x]=abs($height/2);
	}else{
		$mid_1stpass[$x]=abs((1-$mid_exponential_smoothing_factor)*$mid_1stpass[$x-1]+$mid_exponential_smoothing_factor*(($top[$x]+$bottom[$x])/2));
	}
}
for($x=$width-1;$x>=0;$x--){
	if($bottom[$x]-$top[$x]==0){
		$mid_2ndpass[$x]=abs($height/2);
	}else{
		$mid_2ndpass[$x]=abs((1-$mid_exponential_smoothing_factor)*$mid_2ndpass[$x+1]+$mid_exponential_smoothing_factor*(($top[$x]+$bottom[$x])/2));
	}
}
for($x=0;$x<$width;$x++){
	$mid[$x]=(($mid_1stpass[$x]+$mid_2ndpass[$x])/2);
}
$first_word_start=0;
for($x=0;$x<$width;$x++){
	for($y=0;$y<$height;$y++){
		$c=imagecolorat($sharp_img,$x,$y);
		if($c==0){
			$first_word_start=$x;
		}
	}
	if($first_word_start>0){
		break;
	}
}
$first_word_end=0;
for($x=$first_word_start;$x<$width;$x++){
	$black_pixel_found=false;
	for($y=0;$y<$height;$y++){
		$c=imagecolorat($sharp_img,$x,$y);
		if($c==0){
			$black_pixel_found=true;
		}
	}
	if(!$black_pixel_found){
		$white_column_count++;
		if($white_column_count>$white_columns_needed_for_separation){
			$first_word_end=$x-$white_column_count;
		}
	}else{
		$white_column_count=0;
	}
	if($first_word_end>0){
		break;
	}
}
$second_word_start=0;
for($x=$first_word_end+$white_columns_needed_for_separation;$x<$width;$x++){
	for($y=0;$y<$height;$y++){
		$c=imagecolorat($sharp_img,$x,$y);
		if($c==0){
			$second_word_start=$x;
		}
	}
	if($second_word_start>0){
		break;
	}
}
$second_word_end=0;
for($x=$width-1;$x>$second_word_start;$x--){
	for($y=0;$y<$height;$y++){
		$c=imagecolorat($sharp_img,$x,$y);
		if($c==0){
			$second_word_end=$x;
		}
	}
	if($second_word_end>0){
		break;
	}
}
for($count=0;$count<20;$count++){
	$mid[$first_word_start+$count]=$mid[$first_word_start+20];
	$mid[$first_word_end-$count]=$mid[$first_word_end-20];
	$mid[$second_word_start+$count]=$mid[$second_word_start+20];
	$mid[$second_word_end-$count]=$mid[$second_word_end-20];
}
for($x=0;$x<$width;$x++){
	$vector[$x]=(($height/2)-((1-$vector_threshold)*$mid[$x]));
}
# debug **********************************************************************
if($debug_mode){
	$debug=imagecreatefrombmp('resized.bmp');
	for($x=0;$x<$width;$x++){
		imagesetpixel_rgb($debug,$x,$mid[$x],255,0,0);
	}
	imagejpeg($debug,"debug.jpg");
}
# remove distortion **********************************************************
for($x=0;$x<$width;$x++){
	if($bottom[$x]-$top[$x]>0){
		for($y=floor($mid[$x]);$y<$height;$y++){
			if($y<=$bottom[$x]){
				$c=imagecolorat($sharp_img,$x,$y);
				imagesetpixel($temp,$x,$y+$vector[$x],$c);
			}
		}
		for($y=floor($mid[$x])-1;$y>0;$y--){
			if($y>=$top[$x]){
				$c=imagecolorat($sharp_img,$x,$y);
				imagesetpixel($temp,$x,$y+$vector[$x],$c);
			}
		}
	}
}
# seperate both words and crop the *******************************************
$word1_crop_top=0;
for($y=0;$y<$height;$y++){
	for($x=$first_word_start;$x<$first_word_end;$x++){
		$c=imagecolorat($temp,$x,$y);
		if($c==0){
			$word1_crop_top=$y;
		}
	}
	if($word1_crop_top>0){
		break;
	}
}
$word1_crop_bottom=0;
for($y=$height;$y>0;$y--){
	for($x=$first_word_start;$x<$first_word_end;$x++){
		$c=imagecolorat($temp,$x,$y);
		if($c==0){
			$word1_crop_bottom=$y;
		}
	}
	if($word1_crop_bottom<$height){
		break;
	}
}
$word1_new_height=$word1_crop_bottom-$word1_crop_top;
$word1_new_width=$first_word_end-$first_word_start;
$word2_crop_top=0;
for($y=0;$y<$height;$y++){
	for($x=$second_word_start;$x<$second_word_end;$x++){
		$c=imagecolorat($temp,$x,$y);
		if($c==0){
			$word2_crop_top=$y;
		}
	}
	if($word2_crop_top>0){
		break;
	}
}
$word2_crop_bottom=0;
for($y=$height;$y>0;$y--){
	for($x=$second_word_start;$x<$second_word_end;$x++){
		$c=imagecolorat($temp,$x,$y);
		if($c==0){
			$word2_crop_bottom=$y;
		}
	}
	if($word2_crop_bottom<$height){
		break;
	}
}
$word2_new_height=$word2_crop_bottom-$word2_crop_top;
$word2_new_width=$second_word_end-$second_word_start;
if($word1_new_height>$word2_new_height){
	$new_height=$word1_new_height;
}else{
	$new_height=$word2_new_height;
}
$new_width=$word1_new_width+$word_seperator_pixel+$word2_new_width;
$cropped_img=imagecreatetruecolor($new_width+(2*$crop_spacer_pixel),$new_height);
for($x=0;$x<$new_width+(2*$crop_spacer_pixel);$x++){
	for($y=0;$y<$new_height;$y++){
		imagesetpixel($cropped_img,$x,$y,16777215);
	}
}
imagecopyresampled($cropped_img,$temp,$crop_spacer_pixel,0,$first_word_start,$word1_crop_top,$word1_new_width,$word1_new_height,$word1_new_width,$word1_new_height);
imagecopyresampled($cropped_img,$temp,$word1_new_width+$word_seperator_pixel+$crop_spacer_pixel,0,$second_word_start,$word2_crop_top,$word2_new_width,$word2_new_height,$word2_new_width,$word2_new_height);
imagejpeg($cropped_img,'cropped.jpg');
# output to ocr **************************************************************
# ****************************************************************************
$tesseract_output=shell_exec($tesseract_commandline);
$word_tess_text=str_replace("\n","",str_replace("\r","",readfile_chunked('tessoutput.txt')));
if(strlen($word_tess_text)<2){$word_tess_text=$shit;}
if($debug_mode){
	echo "<img src=".$input_image."><br><img src=contrast.bmp><br><img src=debug.jpg><br>";
	imagedestroy($debug);
}else{
	@imagedestroy($temp);
	@imagedestroy($img);
	@unlink('resized.bmp');
	@unlink('cropped1.jpg');
	@unlink('cropped2.jpg');
	@unlink('contrast.bmp');
	@unlink('tessoutput.txt');
	@unlink('debug.jpg');
}
$f=file_put_contents('output.txt',$word_tess_text);
?>
