<?php
//Establish connection with sql server
    $mysql = mysqli_connect("", "ProToAll", "Naorco576!", "ControlPanel");

    if (mysqli_connect_errno()) {
        echo "Failed to connect database: " . mysqli_connect_error();
        exit();
    }
?>
