This lib is used to view or edit our database by browser or native app, include encrypt database.

```groovy
dependencies {
    debugImplementation 'io.github.jiangjm424:view-db:+'
}
```

or

```kts
implementation("io.github.jiangjm424:view-db:+")
```

with encrypt database you should add your database password in the db_viewer share preference such
as
```kotlin
val dbName = "CheeseDatabase"
val dbPwd = "cheese"
if (BuildConfig.DEBUG) {
    context.getSharedPreferences("db_viewer", AppCompatActivity.MODE_PRIVATE).edit()
        .putString(dbName, dbPwd).apply()
}
instance = Room.databaseBuilder(
    context.applicationContext,
    CheeseDb::class.java, dbName
).openHelperFactory(SupportFactory(dbPwd.toByteArray()))
    .addCallback(object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            fillInDb(context.applicationContext)
        }
    }).build()
```

Thanks for :
Android-Debug-Database
https://github.com/amitshekhariitbhu/Android-Debug-Database.git
Glance
https://github.com/guolindev/Glance.git
