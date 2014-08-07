<?php

class Session {
	private function __construct() {
	}
    
	public static function get() {
		static $sSession = null;
		if ($sSession === NULL)
			$sSession = new Session();
		return $sSession;
	}

	public function clear() {
		$this->setUser(null);
	}

	public function setUser($user) {
		$_SESSION['user'] = $user;
	}

	public function getUser() {
		if (!isset($_SESSION['user']))
			return null;

		return $_SESSION['user'];
	}

	public function setRootDownloadDir($dir) {
		$_SESSION['root_download_dir'] = $dir;
	}
	
	public function getRootDownloadDir() {
		if (!isset($_SESSION['root_download_dir']))
			return null;
		return $_SESSION['root_download_dir'];
	}

	public function hasFileAccess($file) {
		return strpos($file, $this->getRootDownloadDir()) === 0;
	}
}

?>
