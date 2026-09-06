sed -i '/alias(libs.plugins.kotlin.compose)/a \
    alias(libs.plugins.secrets)\
' app/build.gradle.kts

cat << 'INNEREOF' >> app/build.gradle.kts

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}
INNEREOF
