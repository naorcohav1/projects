<?php
session_start();
//session 
if (!isset($_SESSION["username"])) {
    session_destroy();
    header("Location: /login.php");
    die();

}




?>