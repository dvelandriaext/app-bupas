package co.com.coexito.bupas.core.config

enum class AppEnvironment(val badgeLabel: String?) {
    DEV("DEV"),
    TEST("TEST"),
    PROD(null);

    val isProduction: Boolean
        get() = this == PROD

    companion object {
        fun fromRaw(raw: String): AppEnvironment {
            return entries.firstOrNull {
                it.name.equals(raw.trim(), ignoreCase = true)
            } ?: DEV
        }
    }
}

data class EnvironmentConfig(
    val environment: AppEnvironment,
    val apiBaseUrl: String,
    val useFakeData: Boolean,
    val isHttpLoggingEnabled: Boolean
)
