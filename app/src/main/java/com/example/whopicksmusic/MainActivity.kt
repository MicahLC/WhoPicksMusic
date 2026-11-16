package com.example.whopicksmusic

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class MainActivity : ComponentActivity() {

    lateinit var textChosen : TextView

    lateinit var btnAddMore : Button
    lateinit var btnChooseNext : Button
    lateinit var nameTable : TableLayout
    var rowCount = 0

    lateinit var db : AppDatabase

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    lateinit var personsList : MutableList<Person>
    var chooseablePersons : MutableList<Person> = ArrayList()

    val personRowMap = HashMap<TableRow, Person>()
    val personToRowMap = HashMap<Long, TableRow>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.layout)

        textChosen = findViewById(R.id.chosenText)

        btnAddMore = findViewById(R.id.addMore)
        btnAddMore.setOnClickListener {
            this.addNameRow(null)
        }
        btnChooseNext = findViewById(R.id.chooseNext)
        btnChooseNext.setOnClickListener {
            this.chooseNext()
        }

        nameTable = findViewById(R.id.nameTable)
        nameTable.setColumnStretchable(0, true)
        nameTable.setColumnStretchable(1, false)

        db =
            Room.databaseBuilder<AppDatabase>(applicationContext, "person-database")
                .fallbackToDestructiveMigration()
                .setDriver(AndroidSQLiteDriver())
                .build()

        // load people from db
        val personDao = db.personDao()
        val time = measureTimeMillis {
            scope.launch {
                personsList = personDao.getAll()
                Log.d("PERSON-DB", "got a list of size ${personsList.size} on app load")
                for (person in personsList) {
                    addNameRow(person)
                }
                chooseablePersons = personsList.toMutableList()
                chooseablePersons.removeIf { p ->  p.hasPicked || p.isSkipped }
                Log.d("PERSON-DB", "there are ${chooseablePersons.size} person(s) left who have not picked and aren't skipped")
                if (chooseablePersons.isEmpty() && personsList.isNotEmpty()) {
                    // are there people we can unskip?
                    chooseablePersons = personsList.filter { p -> p.isSkipped && !p.hasPicked }.toMutableList()
                    Log.d("PERSON-DB", "there are ${chooseablePersons.size} person(s) left who have not picked, they are now marked as not skipped")
                    if (chooseablePersons.isEmpty()) {
                        // everyone has picked, let's reset them all in the list
                        Log.d("PERSON-DB", "Resetting everyone")
                        personsList.forEach { p ->
                            p.hasPicked = false
                            undoSkip(p)
                            chooseablePersons.add(p)
                        }
                    } else {
                        chooseablePersons.forEach { p ->
                            undoSkip(p)
                        }
                    }
                    personDao.updateUsers(*chooseablePersons.toTypedArray())
                }
                personToRowMap.forEach { (person, row) ->
                    Log.d("PERSON-DB", "$person, row $row")
                }
            }
        }
        textChosen.text = time.toString()
    }

    fun addNameRow(person: Person?) {
        val newRow = TableRow(this)
        val tvTextEdit = EditText(this)
        val tvDeleteButton = Button(this)
        tvDeleteButton.text = "Delete"
        tvDeleteButton.setBackgroundResource(R.color.red_chili_red)
        val tvSkipButton = Button(this)
        tvSkipButton.text = "Skip"
        tvSkipButton.setBackgroundResource(R.color.teal_700)

        // use this so we can potentially re-assign
        var person2 = person

        if (person2 != null) {
            tvTextEdit.setText(person2.name)
            if (person2.isSkipped) {
                tvSkipButton.text = "Un-skip"
            }
        } else {
            val n = NameOnly("Name")
            person2 = Person(-1, n.name, false, -1, false)
            scope.launch {
                val pd = db.personDao()
                val id = pd.insertNewPerson(n)
                Log.d("PERSON-DB","added blank Name to db, id was $id")
                val newPersons = pd.loadAllByIds(arrayOf(id))
                person2 = newPersons[0]
                personsList.add(person2)
            }
            tvTextEdit.setText(person2.name)
        }
        personRowMap[newRow] = person2
        personToRowMap[person2.id] = newRow
        Log.d("PERSON-DB", "Created map from ${person2.name}, id ${person2.id} to row")

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                person2.name = s.toString()
                scope.launch {
                    val count = db.personDao().updateUsers(person2)
                    Log.d("PERSON-DB", "Tried to update name of id ${person2.id} to ${s}, count was ${count}")
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        tvTextEdit.addTextChangedListener(textWatcher)


        tvDeleteButton.setOnClickListener {
            // remove this row from table
            nameTable.removeView(newRow)
            // remove person from database
            scope.launch {
                val result = db.personDao().delete(person2)
                Log.d("PERSON-DB", "successfully deleted $result row(s)")
            }
            personsList.remove(person2)
            personToRowMap.remove(person2.id)
            personRowMap.remove(newRow)
            rowCount--
            if (rowCount == 0) {
                btnChooseNext.isEnabled = false
            }
            resetNameList()
        }
        tvSkipButton.setOnClickListener {
            if (person2.hasPicked) {
                // do nothing?
                Log.d("PERSON-DB","can't skip someone who already picked")
                return@setOnClickListener
            }
            if (person2.isSkipped) {
                person2.isSkipped = false
                chooseablePersons.add(person2)
                tvSkipButton.text = "Skip"
            } else {
                if (chooseablePersons.size == 1) {
                    // can't skip the last person
                    Log.d("PERSON-DB","can't skip last person")
                    return@setOnClickListener
                }
                person2.isSkipped = true
                chooseablePersons.remove(person2)
                tvSkipButton.text = "Un-skip"
            }
            scope.launch {
                val count = db.personDao().updateUsers(person2)
                Log.d("PERSON-DB", "Tried to update skip of ${person2.name} to ${person2.isSkipped}, count was $count")
            }
        }

        newRow.addView(tvTextEdit)
        newRow.addView(tvSkipButton)
        newRow.addView(tvDeleteButton)
        nameTable.addView(newRow)

        Log.d("PERSON-DB", "Row ID is now ${newRow.id}")

        rowCount++
        btnChooseNext.isEnabled = true
        resetNameList()
    }

    fun chooseNext() {
        if (rowCount == 0) return
        if (chooseablePersons.isEmpty()) {
            chooseablePersons = personsList.toMutableList()
            chooseablePersons.forEach { p ->
                p.hasPicked = false
                p.isSkipped = false
            }
            Log.d("PERSON-DB", "At top of order, we have ${chooseablePersons.size} choose-able person(s)")
        }
        val chosenIdx = Random.nextInt(chooseablePersons.size)
        val entry = chooseablePersons.removeAt(chosenIdx)
        entry.hasPicked = true
        textChosen.text = entry.name
        // if that was the last person, reset everyone
        if (personsList.all { p -> p.hasPicked }) {
            Log.d("PERSON-DB", "Everyone has picked, resetting the list completely.")
            personsList.forEach { p ->
                p.hasPicked = false
                undoSkip(p)
                chooseablePersons.add(p)
            }
        } else {
        // unskip everyone
        personsList.forEach { p ->
            if (p.isSkipped && !p.hasPicked) {
                undoSkip(p)
                chooseablePersons.add(p)
            }
        }
            }
        updateEveryoneInDb()
    }

    /**
     * Updates internal data and UI to un-skip someone. Does NOT update the DB or chooseablePersons.
     */
    fun undoSkip(person: Person) {
        Log.d("PERSON-DB", "Person ${person.name} was skipped, trying to unskip. personToRowMap has ${personToRowMap.size} entries")
        person.isSkipped = false
        val row = personToRowMap[person.id]
        if (row == null) {
            Log.d("PERSON-DB", "Row didn't exist, bailing")
            return
        }
        val child = row.getChildAt(1)
        Log.d("PERSON-DB", "$child")
        val button = child as Button
        button.text = "Skip"
    }

    fun updateEveryoneInDb() {
        scope.launch {
            val count = db.personDao().updateUsers(*(personsList.toTypedArray()))
            Log.d("PERSON-DB", "Tried to update everyone, actually updated $count")
        }
    }

    fun resetNameList() {
        chooseablePersons.clear()
        // set text?
        textChosen.text = "Click \"Choose...\" to find out!"
    }
}