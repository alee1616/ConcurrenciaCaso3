module com.example.concurrenciacaso3 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.concurrenciacaso3 to javafx.fxml;
    exports com.example.concurrenciacaso3;
}