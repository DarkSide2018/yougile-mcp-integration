plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "yougile-alert-bot"

include(":mcp-server")
include(":alert-bot-app")
include(":mtproto-proxy-kotlin")
