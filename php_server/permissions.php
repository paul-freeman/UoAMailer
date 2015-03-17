<?php

$root = "uploads";
$user = get_current_user();
shell_exec('/usr/bin/setfacl -R -m "user:'.$user.':rwx" '.$root);


// http://stackoverflow.com/questions/3826963/php-list-all-files-in-directory
function directoryToArray($directory, $recursive) {
    $arrayItems = array();
    if ($handle = opendir($directory)) {
        while (false !== ($file = readdir($handle))) {
            if ($file != "." && $file != "..") {
                if (is_dir($directory. "/" . $file)) {
                    if($recursive) {
                        $arrayItems = array_merge($arrayItems, directoryToArray($directory. "/" . $file, $recursive));
                    }
                    $file = $directory . "/" . $file;
                    $arrayItems[] = preg_replace("/\/\//si", "/", $file);
                } else {
                    $file = $directory . "/" . $file;
                    $arrayItems[] = preg_replace("/\/\//si", "/", $file);
                }
            }
        }
        closedir($handle);
    }
    return $arrayItems;
}


$files = directoryToArray($root, true);
foreach ($files as $file) {
	echo '/usr/bin/setfacl -m "user:'.$user.':rwx" '.$file."\n";

	shell_exec('/usr/bin/setfacl -m "user:'.$user.':rwx" '.$file);
}


?>
