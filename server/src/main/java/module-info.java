module com.bag_tos {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.bag_tos to javafx.fxml;
    exports com.bag_tos;
}