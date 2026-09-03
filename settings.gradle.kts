plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "KarenBot"
include("shinobu")

include("maimai")
include("meme")
include("admin")
include("text")
include("chunithm")
include("random")
include("audio")