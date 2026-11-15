package co.com.coexito.bupas.core.navigation

sealed class BupasDestination(val route: String) {
    data object Login : BupasDestination("login")
    data object RecoverPassword : BupasDestination("recover_password")
    data object OrdersInbox : BupasDestination("orders_inbox")
    data object OrderDetail : BupasDestination("order_detail/{numeroSolicitud}") {
        fun createRoute(numeroSolicitud: String): String = "order_detail/$numeroSolicitud"
    }
    data object FieldCollection : BupasDestination("field_collection/{numeroSolicitud}") {
        fun createRoute(numeroSolicitud: String): String = "field_collection/$numeroSolicitud"
    }
}
