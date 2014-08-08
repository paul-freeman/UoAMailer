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

	public function hasUploadDirAccess($dir) {
		$user = $this->getUser();
		if ($user === null)
			return false;

		// do we own the dir?
		if (strpos($dir, "uploads/".$user) === 0)
			return true;

		// check if we have dir access as part of the group
		$handle = fopen($dir."/groupMembers", "r");
		if (!$handle)
			return false;
		while (($line = fgets($handle)) !== false) {
			if (strcmp($user, trim($line)) == 0) {
				fclose($handle);
				return true;
			}
		}
		fclose($handle);
		return false;
	}
}

?>
