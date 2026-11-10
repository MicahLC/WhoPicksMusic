package com.example.whopicksmusic

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Update

@Entity
data class Person(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @ColumnInfo(name = "name") var name : String,
    @ColumnInfo(name = "hasPicked", defaultValue = "false") var hasPicked : Boolean,
    @ColumnInfo(name = "group_id", defaultValue = "-1") val groupId : Int
)

data class NameOnly(val name: String)

@Dao
interface PersonDao {
    @Query("SELECT * FROM person")
    suspend fun getAll(): List<Person>

    @Query("SELECT * FROM person WHERE id IN (:userIds)")
    suspend fun loadAllByIds(userIds: IntArray): List<Person>

    @Query("SELECT * FROM person WHERE group_id = :groupId")
    suspend fun loadByGroupId(groupId: Int): List<Person>

    @Insert(entity = Person::class)
    suspend fun insertNewPerson(nameOnly: NameOnly): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg persons: Person) : List<Long>

    @Update
    suspend fun updateUsers(vararg persons: Person)

    @Delete
    suspend fun delete(person: Person) : Int
}

@Database(entities = [Person::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
}