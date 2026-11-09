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

class MainActivity : ComponentActivity(), View.OnClickListener {

    lateinit var btnAddMore : Button
    lateinit var btnChooseNext : Button
    lateinit var nameTable : TableLayout
    var rowCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.layout)
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


        this.addNameRow()
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
        }

        newRow.addView(tvTextEdit)
        newRow.addView(tvButton)
        nameTable.addView(newRow)

        rowCount++
    }

    fun chooseNext() {
        var names = ArrayList<String>()
        for (view in nameTable.allViews){
            if (view is TableRow){
                for (v in view.allViews){
                    if (v is EditText){
                        names.add(v.text.toString())
                    }
                }
            }
        }
    }
}