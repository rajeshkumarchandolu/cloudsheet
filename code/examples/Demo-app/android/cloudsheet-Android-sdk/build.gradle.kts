plugins {
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("maven-publish")
}

android {
    namespace = "com.opencloudsheet.sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

repositories {
    maven {
        url = uri("https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1")
    }
}

dependencies {
    // All dependencies are internal - consumer only uses CloudSheetSDK APIs
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.microsoft.identity.client:msal:8.1.1")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = findProperty("GROUP") as String
                artifactId = findProperty("ARTIFACT_ID") as String
                version = findProperty("VERSION_NAME") as String

                pom {
                    name.set(findProperty("POM_NAME") as String)
                    description.set(findProperty("POM_DESCRIPTION") as String)
                    url.set(findProperty("POM_URL") as String)

                    licenses {
                        license {
                            name.set(findProperty("POM_LICENSE_NAME") as String)
                            url.set(findProperty("POM_LICENSE_URL") as String)
                        }
                    }
                }
            }
        }

        repositories {
            mavenLocal()
        }
    }
}
