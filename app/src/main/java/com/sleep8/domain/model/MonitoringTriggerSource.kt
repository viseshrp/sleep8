package com.sleep8.domain.model

enum class MonitoringTriggerSource(val value: String) {
    SCHEDULE("schedule"),
    NIGHT_WINDOW_BOUNDARY_ALARM("night window boundary alarm"),
    NIGHT_WINDOW_BACKSTOP("night window backstop"),
    PERIODIC_HEALTH_CHECK("periodic health check"),
    BOOT_OR_TIME_RECONCILE("boot/time reconcile"),
    APP_LAUNCH_RECONCILE("app launch reconcile")
}
