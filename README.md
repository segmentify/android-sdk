# Segmentify Android SDK
Segmentify  SDK for sending events and rendering recommendations for android based devices

> **Supports Android 4.4(API Level: 24) and higher devices.**
> **Current version : 2.2.9*

## Installation

You can install Segmentify Android SDK to your application by using  [Maven](https://mvnrepository.com/artifact/com.segmentify.sdk/android).

To use Maven, add the project gradle file:


```java
buildscript {
    repositories {
        mavenCentral()
    }
}
```

Please add following line to your gradle file:

Gradle : 
```java
implementation 'com.segmentify.sdk:android:*.*.*'
```
or If you are using another tool, you can add it as follows :

Maven:
```xml
<!-- https://mvnrepository.com/artifact/com.segmentify.sdk/android -->
<dependency>
    <groupId>com.segmentify.sdk</groupId>
    <artifactId>android</artifactId>
    <version>***</version>
</dependency>
```
SBT :
```scala
// https://mvnrepository.com/artifact/com.segmentify.sdk/android
libraryDependencies += "com.segmentify.sdk" % "android" % "*.*.*"
```

Ivy :
```xml
<!-- https://mvnrepository.com/artifact/com.segmentify.sdk/android -->
<dependency org="com.segmentify.sdk" name="android" rev="*.*.*"/>
```

Grape : 
```xml
<!-- https://mvnrepository.com/artifact/com.segmentify.sdk/android -->
<dependency org="com.segmentify.sdk" name="android" rev="*.*.*"/>
```

Leiningen : 
```Clojure 
;; https://mvnrepository.com/artifact/com.segmentify.sdk/android
[com.segmentify.sdk/android "*.*.*"]
```

Buildr : 
```
# https://mvnrepository.com/artifact/com.segmentify.sdk/android
'com.segmentify.sdk:android:jar:*.*.*'

```


## Usage

```kotlin
SegmentifyManager.config(
    context = this, 
    appKey = "YOUR_API_KEY", 
    dataCenterUrl = "https://your-datacenter.url", 
    subDomain = "your-subdomain",
    authToken = "YOUR_AUTH_TOKEN" // Optional: custom Basic Auth token
)
```

You can also update the authentication token or hostname at runtime:

```kotlin
// Update auth token
SegmentifyManager.setAuthToken("NEW_AUTH_TOKEN")

// Update push configuration
SegmentifyManager.setPushConfig(
    dataCenterUrlPush = "https://push-notification-api.url",
    authToken = "OPTIONAL_AUTH_TOKEN"
)

// Update full configuration (including hostname)
SegmentifyManager.setConfig(
    apiKey = "YOUR_API_KEY",
    dataCenterUrl = "https://new-datacenter.url",
    subDomain = "your-subdomain",
    authToken = "OPTIONAL_AUTH_TOKEN"
)
```


To learn more about how to integrate Segmentify Android SDK to your application, please check [Integration Guide](https://www.segmentify.com/dev/integration_android/).

For other integrations you can check [Master Integration](https://www.segmentify.com/dev/) guide too.

## License

Segmentify Android SDK is available under the BSD-2 license.
Please check LICENSE file to learn more about details.


## Push Permission & User Updates 2.2.6
2.2.6 update changes user events in the background. This is a completely internal and will not effect sdk users. Also productId parameter has been removed from push interaction event.

Good Luck!



