package com.example.whopicksmusic

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.allViews
import kotlin.random.Random

class MainActivity : ComponentActivity(), View.OnClickListener {

    lateinit var textChosen : TextView

    lateinit var btnAddMore : Button
    lateinit var btnChooseNext : Button
    lateinit var nameTable : TableLayout
    var rowCount = 0

    var availableNames = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.layout)

        textChosen = findViewById(R.id.chosenText)

        btnAddMore = findViewById(R.id.addMore)
        btnAddMore.setOnClickListener {
            this.addNameRow()
        }
        btnChooseNext = findViewById(R.id.chooseNext)
        btnChooseNext.setOnClickListener {
            this.chooseNext()
        }

        nameTable = findViewById(R.id.nameTable)
        nameTable.setColumnStretchable(0, true)
        nameTable.setColumnStretchable(1, false)

        // start us off with 1 row
        // TODO: save names and bring them back? also save history of chosen names?
        addNameRow()
    }

    override fun onClick(v: View?) {
        /*when(v?.id){
            R.id.addMore ->{
                // duplicate 2nd row layout in name table?
                nameTable.ch
            }
        }*/
    }

    fun addNameRow() {
        val newRow = TableRow(this)
        val tvTextEdit = EditText(this)
        val tvButton = Button(this)

        tvTextEdit.setText("Name")
        tvButton.text = "Delete"

        // val idx = rowCount
        tvButton.setOnClickListener {
            // remove this row from table
            nameTable.removeView(newRow)
            // nameTable.removeViewAt(idx)
            rowCount--
            if (rowCount == 0) {
                btnChooseNext.isEnabled = false
            }
            resetNameList()
        }

        newRow.addView(tvTextEdit)
        newRow.addView(tvButton)
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