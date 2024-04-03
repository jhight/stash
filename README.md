<p align="center">
    <img width="128" src="icon.png" align="center" alt="Stash" />
    <h1 align="center">stash</h1>
    <p align="center">An encrypted Android DataStore for storing small amounts of sensitive data.</p>
    <p><br/></p>
</p>

## About
Stash is a tiny, encrypted Android DataStore that makes it easy to securely store small amounts of sensitive data using the Android Keystore system. Under the hood, stash serializes data into JSON via [`kotlinx.serialization`](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md).

## Reading
You can either read the entire contents of a stash into a @Serializable or read individual properties. Because all data is serialized prior to encryption, you can use the two approaches interchangeably.
```kotlin
@Serializable
data class Profile (
    val username: String,
    val accountId: Int
)

// uses an AES-256 cipher with a default key managed by Android Keystore
val stash = Stash(file)

// or, if you'd rather use a custom key
val stash = Stash(file, Aes256AndroidKeystoreCryptoProvider("myKeyAlias"))

// reads contents into profile
stash.read<Profile> { profile ->
  println("Username: ${profile.username}")
}

// or, if you'd rather read individual properties
val username = stash.get<String>("username")
val accountId = stash.get<Int>("accountId")
```

## Writing
Likewise, writing to a stash can either be done all at once via a @Serializable or by writing individual properties.
```kotlin
val profile = Profile("jdoe", 12345)

// because Stash is a DataStore, writes are done atomically
stash.write(profile)

// or, if you'd rather write individual properties
stash.write("username", "jdoe")
stash.write("accountId", 12345)
```
