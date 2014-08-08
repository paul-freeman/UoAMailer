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
session_start();

include_once 'Session.php';
include_once 'LDAPLoginUoA.php';


function showLoginView($username = null) {
	echo "<form id='login' action='download.php' method='post' accept-charset='UTF-8'>
	<fieldset >
	<legend>Login</legend>
	<label for='username' >User Name:</label>
	<input type='text' name='username' id='username'";
	if ($username !== null && $username != "")
		echo " value=".$username;
	echo " maxlength=\"50\" />
	
	<label for='password' >Password:</label>
	<input type='password' name='password' id='password' maxlength=\"50\" />
	
	<input type='submit' name='loginButton' value='Login' />
	
	</fieldset>
	</form>";
}

function login() {
	$username = "";
	$password = "";

	if (isset($_POST['username']))
		$username = trim($_POST['username']);
	if (isset($_POST['password']))
		$password = trim($_POST['password']);
	if ($username == "" || $password == "") {
		echo "Enter user name and password!";
		showLoginView($username);
		die();
	}

	$ldapLogin = new LDAPLoginUoA();
    if (!$ldapLogin->login($username, $password)) {
		echo "failed to login: ".$ldapLogin->getLastError()."\n";
		showLoginView();
		die();
    }
    Session::get()->setUser($username);
}

function isLoggedIn() {
	if (Session::get()->getUser() === null || Session::get()->getUser() == "")
		return false;
	return true;
}

if (isset($_GET['dir']))
	$_SESSION['request_download_dir'] = $_GET['dir'];


// main
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (isset($_POST['loginButton'])) {
        login();
        if (isset($_SESSION['request_download_dir'])) {
			Session::get()->setRootDownloadDir($_SESSION['request_download_dir']);
		} else
			Session::get()->setRootDownloadDir("uploads/".Session::get()->getUser());
		if (!Session::get()->hasUploadDirAccess(Session::get()->getRootDownloadDir())) {
			session_destroy();
			die("Sorry you have no access rights for this directory: ".Session::get()->getRootDownloadDir());
		}
		header('Location: explorer.php');
		exit(); 
    } else if (isset($_POST['logoutButton'])) {
        session_destroy();
        header('Location: download.php');
		exit();
    }
}

if (isLoggedIn()) {
	echo "Hello ".Session::get()->getUser()."! Your files: <a href='explorer.php'>files</a>.";
	echo "<form method='POST' action=''><input type='submit' name='logoutButton' value='Logout' /></form>";
} else {
	showLoginView();
}

?> 

