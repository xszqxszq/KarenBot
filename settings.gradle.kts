plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "KarenBot"
include("shinobu")

include("maimai")
include("otto")
include("meme")
include("admin")
include("text")
include("guess")