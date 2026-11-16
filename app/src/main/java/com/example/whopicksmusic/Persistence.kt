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
    @ColumnInfo(name = "group_id", defaultValue = "-1") var groupId : Int,
    @ColumnInfo(name = "isSkip", defaultValue = "false") var isSkipped : Boolean
)

/**
 * Data class used for creating a new person.
 */
data class NameOnly(val name: String)

/**
 * Data class used for marking someone as picked.
 */
data class PersonPicked(
    val id: Long,
    val hasPicked: Boolean
)

data class PersonSkipped(
    val id: Long,
    val isSkip: Boolean
)

@Dao
interface PersonDao {
    @Query("SELECT * FROM person")
    suspend fun getAll(): MutableList<Person>

    @Query("SELECT * FROM person WHERE id IN (:userIds)")
    suspend fun loadAllByIds(userIds: Array<Long>): MutableList<Person>

    @Query("SELECT * FROM person WHERE group_id = :groupId")
    suspend fun loadByGroupId(groupId: Int): MutableList<Person>

    @Insert(entity = Person::class)
    suspend fun insertNewPerson(nameOnly: NameOnly): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg persons: Person) : List<Long>

    @Update
    suspend fun updateUsers(vararg persons: Person) : Int

    @Delete
    suspend fun delete(person: Person) : Int
}

@Database(entities = [Person::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
}