<?php
error_reporting(E_ALL);
ini_set('display_errors', true);
ini_set('safe_mode', false);
ini_set('allow_url_fopen', true);
ini_set('allow_url_include', true);

ini_set('allow_url_include', 'on');

set_error_handler("var_dump");

?>

<?php

include_once 'JSONHandler.php';


if (!isset($_REQUEST['json']))
	die("not a json request");

dispatchJSON($_REQUEST['json']);

?>
