package com.example.spscagent

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException


class ReportActivity : AppCompatActivity() {

    lateinit var wv: WebView
   // lateinit var btnSave: MenuItem;
    //private var printJob:PrintJob?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
      //  requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        wv=findViewById(R.id.reportWebView)
        showContent()
       // btnSave = findViewById(R.id.btnSave);
        //Log.d("myLog", "postResult")

        val topAppBar = findViewById(R.id.topAppBar) as MaterialToolbar?
        // val topAppBar

        topAppBar?.setNavigationOnClickListener {
            // Handle navigation icon press
        }


        topAppBar?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {
                    // Handle search icon press

                //
                    //saveAsPdf()
                    GlobalScope.launch(Dispatchers.Default) {
                        savePage()



                    }
                    true
                }

                else -> false
            }
        }

        topAppBar?.setNavigationOnClickListener {

            this.finish()
        }


    }



/*
    companion object {
        const val TOTAL_COUNT = "total_count"
    }
*/

    fun showContent() {
       // val count = intent.getIntExtra(TOTAL_COUNT, 0)
    //    Log.d("myLog", "${count}")

        Log.d("myLog", "loadUrl ${urlApiShowReport}/${gvHash}")
       // wv.zoomBy(10F)
        wv.loadUrl("${urlApiShowReport}/${gvHash}")



    //wv.save
    }

    private fun saveAsPdf(){
        Toast.makeText(this, "Saving report...", Toast.LENGTH_SHORT).show()



     // val pm=getSystemService(Context.PRINT_SERVICE) as PrintManager
       // val jobName="jobName"
      //  val printAdapter=wv.createPrintDocumentAdapter(jobName)
     //   val printAttributes= PrintAttributes.Builder()
      ///  pm.print(jobName,printAdapter,printAttributes.build())

     //   wv.loadUrl("${urlApiShowReport}/${gvHash}")
        //wm.
    }


    suspend fun savePage(): String {

        //   Toast.makeText(this@MainActivity, "Getting provider data....", Toast.LENGTH_SHORT).show()
        var result=""
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${urlApiShowReport}/${gvHash}")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d("myLog", "${urlApiShowReport} | Error ${response.code} ${response.message}")
                }
                try {

                    result= response.body!!.string()

                    val destination = File(filesDir, "report.html")
                    writeToFile(destination, result)
                    // check the file exists and its size
                    Log.i(
                        "myLog", "file = " + destination.absolutePath +
                                ", w/exists = " + destination.exists() +
                                ", w/size = " + destination.length()
                    )
                    // get contents of the file
                    val content = readFromFile(destination)
                    Log.i("myLog", content ?: "Failed")

                    // создаём новое намерение
               //     val intent = Intent(Intent.ACTION_VIEW)
                    // устанавливаем флаг для того, чтобы дать внешнему приложению пользоваться нашим FileProvider
                    //intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    // генерируем URI, я определил полномочие как ID приложения в манифесте,
                    // последний параметр это файл, который я хочу открыть
                    val uri: Uri? = FileProvider.getUriForFile(this, "com.example.spscagent",  destination)
                    //    intent.setDataAndType(uri, "application/html");
                 //   val pm: PackageManager = getActivity().getPackageManager()
                  //  if (intent.resolveActivity(pm) != null) {
                  //      startActivity(intent)
                  //  }

                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        //putExtra(Intent.EXTRA_TEXT, destination.absolutePath)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "text/html"
                       // setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        //putExtra(Intent.EXTRA_TEXT, content)
                        //type = "text/html"
                    }.also { intent ->
                        startActivity(intent)
                    }


                   // Log.d("myLog", gvProviderData!!.toString())
                } catch (t: Throwable) {
                    Log.e("myLog", "some my error")
                }
            }
        } catch (e: IOException) {
            Log.d("myLog", "$urlApiShowReport | Ошибка подключения: $e")
        }
       return result
    }

    private fun writeToFile(file: File, text: String?) {
        try {
            val fw = FileWriter(file)
            fw.write(text)
            fw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun readFromFile(file: File?): String? {
        try {
            val text = StringBuilder()
            val br = BufferedReader(FileReader(file))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
                text.append('\n')
            }
            br.close()
            return text.toString()
        } catch (e: IOException) {
            Log.e("myLog", e.message!!)
            e.printStackTrace()
        }
        return null
    }


}


