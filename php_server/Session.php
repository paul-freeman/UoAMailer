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
}

?>
