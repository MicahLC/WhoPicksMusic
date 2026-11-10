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

    var availableNames = ArrayList<String>()

    lateinit var personsList : List<Person>

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
                .setDriver(AndroidSQLiteDriver())
                .build()

        // start us off with 1 row
        // TODO: save names and bring them back? also save history of chosen names?
        // addNameRow()
        val personDao = db.personDao()
        val time = measureTimeMillis {
            scope.launch {
                val persons = personDao.getAll()
                for (person in persons) {
                    addNameRow(person)
                }
                personsList = persons
                Log.d("MICAH", "got a list of size ${personsList.size} on app load")
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
            person2 = Person(-1, "Name", false, -1)
            scope.launch {
                val ids = db.personDao().insertAll(person2)
                Log.d("MICAH","added blank Name to db, ids was $ids")
                person2.id = ids[0]
            }
            tvTextEdit.setText(person2.name)
        }
        personRowMap[newRow] = person2

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                person2.name = s.toString()
                scope.launch {
                    db.personDao().updateUsers(person2)
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
                Log.d("MICAH", "successfully deleted $result row(s)")
            }
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
        if (availableNames.isEmpty()) {
            for (view in nameTable.allViews) {
                if (view is TableRow) {
                    for (v in view.allViews) {
                        if (v is EditText) {
                            availableNames.add(v.text.toString())
                        }
                    }
                }
            }
        }
        val chosenIdx = Random.nextInt(availableNames.size)
        val entry = availableNames[chosenIdx]
        availableNames.removeAt(chosenIdx)
        textChosen.text = entry
    }

    fun resetNameList() {
        availableNames.clear()
        // set text?
        textChosen.text = "Click \"Choose...\" to find out!"
    }
}