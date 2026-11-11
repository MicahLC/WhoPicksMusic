package com.example.whopicksmusic

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.allViews
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class MainActivity : ComponentActivity(), View.OnClickListener {

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
                val persons = personDao.getAll()
                personsList = persons
                Log.d("PERSON-DB", "got a list of size ${personsList.size} on app load")
                for (person in persons) {
                    addNameRow(person)
                }
                chooseablePersons = personsList.toMutableList()
                chooseablePersons.removeIf { p ->  p.hasPicked }
                Log.d("PERSON-DB", "there are ${chooseablePersons.size} person(s) left who have not picked")
            }
        }
        textChosen.text = time.toString()
    }

    override fun onClick(v: View?) {
        /*when(v?.id){
            R.id.addMore ->{
                // duplicate 2nd row layout in name table?
                nameTable.ch
            }
        }*/
    }

    fun addNameRow(person: Person?) {
        val newRow = TableRow(this)
        val tvTextEdit = EditText(this)
        val tvDeleteButton = Button(this)
        var person2 = person

        if (person2 != null) {
            tvTextEdit.setText(person2.name)
        } else {
            val n = NameOnly("Name")
            person2 = Person(-1, n.name, false, -1)
            scope.launch {
                val pd = db.personDao()
                val id = pd.insertNewPerson(n)
                Log.d("PERSON-DB","added blank Name to db, id was $id")
                var newPersons = pd.loadAllByIds(arrayOf(id))
                person2 = newPersons[0]
                personsList.add(person2)
            }
            tvTextEdit.setText(person2.name)
        }
        personRowMap[newRow] = person2

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

        tvDeleteButton.text = "Delete"

        tvDeleteButton.setOnClickListener {
            // remove this row from table
            nameTable.removeView(newRow)
            // remove person from database
            scope.launch {
                val result = db.personDao().delete(person2)
                Log.d("PERSON-DB", "successfully deleted $result row(s)")
            }
            personsList.remove(person2)
            rowCount--
            if (rowCount == 0) {
                btnChooseNext.isEnabled = false
            }
            resetNameList()
        }

        newRow.addView(tvTextEdit)
        newRow.addView(tvDeleteButton)
        nameTable.addView(newRow)

        rowCount++
        btnChooseNext.isEnabled = true
        resetNameList()
    }

    fun chooseNext() {
        if (rowCount == 0) return
        if (chooseablePersons.isEmpty()) {
            chooseablePersons = personsList.toMutableList()
            chooseablePersons.forEach { p -> p.hasPicked = false }
            Log.d("PERSON-DB", "At top of order, we have ${chooseablePersons.size} choose-able person(s)")
        }
        val chosenIdx = Random.nextInt(chooseablePersons.size)
        val entry = chooseablePersons.removeAt(chosenIdx)
        entry.hasPicked = true
        textChosen.text = entry.name
        scope.launch {
            db.personDao().updateUsers(*(personsList.toTypedArray()))
        }
    }

    fun resetNameList() {
        chooseablePersons.clear()
        // set text?
        textChosen.text = "Click \"Choose...\" to find out!"
    }
}