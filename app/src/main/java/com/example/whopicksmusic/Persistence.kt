package com.example.whopicksmusic

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase

@Entity
data class Person(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name") val name : String,
    @ColumnInfo(name = "hasPicked") val hasPicked : Boolean,
    @ColumnInfo(name = "group_id") val groupId : Int
)

@Dao
interface PersonDao {
    @Query("SELECT * FROM person")
    suspend fun getAll(): List<Person>

    @Query("SELECT * FROM person WHERE id IN (:userIds)")
    suspend fun loadAllByIds(userIds: IntArray): List<Person>

    @Query("SELECT * FROM person WHERE group_id = :groupId")
    suspend fun loadByGroupId(groupId: Int): List<Person>

    @Insert
    suspend fun insertAll(vararg persons: Person)

    @Delete
    suspend fun delete(person: Person)
}

@Database(entities = [Person::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
}