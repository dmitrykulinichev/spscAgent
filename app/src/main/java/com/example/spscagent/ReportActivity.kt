package com.example.spscagent

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class ReportActivity : AppCompatActivity() {

    lateinit var wv: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        showRandomNumber()
    }
/*
    companion object {
        const val TOTAL_COUNT = "total_count"
    }
*/
    fun showRandomNumber() {
       // val count = intent.getIntExtra(TOTAL_COUNT, 0)
    //    Log.d("myLog", "${count}")
        wv=findViewById(R.id.reportWebView)
        Log.d("myLog", "loadUrl ${urlApiShowReport}/${gvHash}")
        wv.loadUrl("${urlApiShowReport}/${gvHash}")
    }

}