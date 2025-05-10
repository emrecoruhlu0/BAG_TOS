module com.bag_tos {
    requires javafx.controls;
    requires javafx.fxml;
    // Common modülüne bağımlılık ekleyin
    requires com.bag_tos.common;

    // Dışa açılacak paketleri belirtin
    exports com.bag_tos;
    exports com.bag_tos.roles;
    exports com.bag_tos.roles.mafia;
    exports com.bag_tos.roles.town;
    exports com.bag_tos.roles.naturel;
}