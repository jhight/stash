<p align="center">
    <img width="128" src="icon.png" align="center" alt="Stash" />
    <h1 align="center">stash</h1>
    <p align="center">An encrypted Android DataStore for storing small amounts of sensitive data.</p>
    <p align="center">
        <a href="https://jitpack.io/#com.jhight/stash"><img src="https://jitpack.io/v/com.jhight/stash.svg" alt="Release"/></a>
        &nbsp;
        <a href="https://github.com/jhight/stash/actions/workflows/unit-tests.yaml"><img src="https://github.com/jhight/stash/actions/workflows/unit-tests.yaml/badge.svg" alt="Unit Tests"/></a>
    </p>
    <p><br/></p>
</p>

## About
Stash is a tiny [Android DataStore](https://developer.android.com/topic/libraries/architecture/datastore) that makes it easy to securely store small amounts of sensitive data. Under the hood, stash serializes data into JSON via [Kotlin Serialization](https://kotlinlang.org/docs/serialization.html) before being encrypted using [Android Keystore](https://developer.android.com/privacy-and-security/keystore). Stash is great for storing things like API keys or authentication tokens.

## Setup
Add the jitpack repository to your project-level gradle settings:
```gradle
repositories {
    //...
    maven { url 'https://jitpack.io' }
}
```

Add the stash dependency to the app gradle module:
```gradle
dependencies {
    // ...
    implementation 'com.jhight:stash:1.0.17'
}
```

## Reading
You can get the entire contents of a stash into a `@Serializable` or get individual properties. The two approaches can be used interchangeably.
```kotlin
// open a stash, uses an AES-256 cipher with a default key managed by Android Keystore
val stash = Stash(File(context.dataDir, "data.stash"))

// or, if you need to use a specific key
val stash = Stash(
    File(context.dataDir, "data.stash"),
    Aes256KeystoreCryptoProvider("myKeyAlias", "keyPassword")
)

@Serializable
data class Account(
    val userId: String,
    val balance: Double
)

// get entire contents of the stash
stash.get<Account> {
    println(it.userId)
    println(it.balance)
}

// or, get individual properties
val userId = stash.get<String>("userId")
val balance = stash.get<Double>("balance")
```

## Writing
Likewise, putting data into a stash can either be done via a `@Serializable` or by individual properties.
```kotlin
// put the object's contents into stash
stash.put(Account("user-12345", 99.50))

// equivalent to example above
stash.put("userId", "user-12345")
stash.put("balance", 99.50)
```
