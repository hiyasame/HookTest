plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        val hook by creating {
            implementationClass = "com.example.javahook.JavaHookPlugin"
            id = "com.example.javahook"
        }
    }
}

dependencies {
    compileOnly("com.android.tools.build:gradle:8.9.0")
    implementation("org.ow2.asm:asm-commons:9.7")
}
