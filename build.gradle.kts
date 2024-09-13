import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.24"
  id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "com.block"
version = "1.2.1"

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
    intellijDependencies()
  }
}

// Set the JVM language level used to build the project.
kotlin {
  jvmToolchain(17)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.commonmark:commonmark:0.22.0")
  intellijPlatform {
    intellijIdeaUltimate("2024.1.5")
    pluginVerifier()
    zipSigner()
    instrumentationTools()
  }
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    sinceBuild.set("232")
    untilBuild.set("242.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
