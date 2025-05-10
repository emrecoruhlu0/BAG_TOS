module com.bag_tos.common {
    // Jackson bağımlılıkları
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    // Dışa açılacak paketleri belirtin
    exports com.bag_tos.common.message;
    exports com.bag_tos.common.message.request;
    exports com.bag_tos.common.message.response;
    exports com.bag_tos.common.model;
    exports com.bag_tos.common.util;
}