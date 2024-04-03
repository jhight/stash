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
// declare a Serializable type
@Serializable
data class Account(
    val userId: String,
    val balance: Double
)

// uses an AES-256 cipher with a default key managed by Android Keystore
val stash = Stash(file)

// or, if you'd rather use a custom key
val stash = Stash(file, Aes256AndroidKeystoreCryptoProvider("myKeyAlias"))

// to read the data back, use the read function
stash.get<Account> {
    println(it.userId)
    println(it.balance)
}
// or, you can read individual properties
val userId = stash.get<String>("userId")
val balance = stash.get<Double>("balance")
```

## Writing
Likewise, writing to a stash can either be done all at once via a @Serializable or by writing individual properties.
```kotlin
// write the object's contents to the stash
stash.put(Account("user-12345", 99.50))

// alternative, equivalent to the example above
stash.put("userId", "user-12345")
stash.put("balance", 99.50)
```
