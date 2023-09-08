package com.example.spscagent


import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log

import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest

const val urlApiDomainsList = "https://my.everyday.ua/api/spscMobileAppGetDomainsTest"
const val urlApiProvider = "https://my.everyday.ua/api/spscFakeProvider"

//const val urlApiProvider = "http://ip-api.com/json/"
//const val urlApiDomainsList = "https://my.everyday.ua/api/spscMobileAppGetDomains"

const val urlApiPostResult = "https://my.everyday.ua/api/spscMobileAppPostResult"
const val urlApiPostProviderData = "https://my.everyday.ua/api/spscMobileAppPostProviderData"
const val urlApiShowReport = "https://my.everyday.ua/api/spscMobileAppShowReport"

var gvProviderData: JSONObject? = null

var gvDomainsTotal: Int? = null
var gvDomainsChecked: Int? = null
var gvDomainsAlive: Int? = null
var gvDomains: JSONArray? = null
var resultArray = arrayOf<Array<String>>()
var gvHash: String = ""

var isLoadedProviderData: Boolean = false
var isLoadedDomainsData: Boolean = false
var isOnline: Boolean = false

class MainActivity : AppCompatActivity() {

    lateinit var tvLog: TextView
    lateinit var tvChecked: TextView
    lateinit var tvAlive: TextView
    lateinit var tvProviderIp: TextView
    lateinit var tvProviderName: TextView
    lateinit var tvProviderCity: TextView
    lateinit var btnMain: Button
    lateinit var btnReport: Button
    lateinit var btnCheckConn: Button
    lateinit var tvTotalDomains: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = SCREEN_ORIENTATION_PORTRAIT

        tvChecked = findViewById(R.id.domainsChecked)
        tvAlive = findViewById(R.id.domainsAlive)
        tvLog = findViewById(R.id.tvLog)
        tvProviderIp = findViewById(R.id.ip)
        tvProviderName = findViewById(R.id.providerName)
        tvProviderCity = findViewById(R.id.providerCity)
        tvTotalDomains = findViewById(R.id.domainsCount)
        btnCheckConn = findViewById(R.id.checkConn)
        btnMain = findViewById(R.id.buttonCheck)
        btnReport = findViewById(R.id.btnReport)

        /*********      BTN REPORT CLICK      **********/
        btnCheckConn.setOnClickListener {
            isOnline=isOnline()
            if (isOnline) {
                btnCheckConn.visibility = View.INVISIBLE
                Toast.makeText(this@MainActivity, "Є підключення!!", Toast.LENGTH_SHORT).show()
                GlobalScope.launch(Dispatchers.Default) {
                    getProviderData();
                }
                GlobalScope.launch(Dispatchers.Default) {
                    getDomainsData();
                }
            } else {
                Toast.makeText(this@MainActivity, "Схоже Ви не підключені до Інтернету", Toast.LENGTH_SHORT).show()

            }
        }


        isOnline=isOnline()
        if (isOnline) {
            GlobalScope.launch(Dispatchers.Default) {
                getProviderData();
            }
            GlobalScope.launch(Dispatchers.Default) {
                getDomainsData();
            }
        } else {
            Toast.makeText(this@MainActivity, "Схоже Ви не підключені до Інтернету", Toast.LENGTH_SHORT).show()
            btnCheckConn.visibility = View.VISIBLE
        }

    }


    //override fun onNavigationClickL


    suspend fun getProviderData() {

     //   Toast.makeText(this@MainActivity, "Getting provider data....", Toast.LENGTH_SHORT).show()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(urlApiProvider)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d("myLog", "$urlApiProvider | Error ${response.code} ${response.message}")
                }
                try {
                    isLoadedProviderData = true
                    gvProviderData = JSONObject(response.body!!.string())
                    runOnUiThread {
                        tvProviderIp.setText(gvProviderData!!.getString("query"));
                        tvProviderName.setText(gvProviderData!!.getString("isp"));
                        tvProviderCity.setText(gvProviderData!!.getString("regionName"));

                        gvHash = md5(gvProviderData!!.getString("query") + System.currentTimeMillis().toString())

                        initUI()
                    }
                    Log.d("myLog", gvProviderData!!.toString())
                } catch (t: Throwable) {
                    Log.e("myLog", "Could not parse malformed JSON: \"$gvProviderData\"")
                }
            }
        } catch (e: IOException) {
            Log.d("myLog", "$urlApiProvider | Ошибка подключения: $e")
        }
    }

    suspend fun getDomainsData() {
       // runOnUiThread {
        //    Toast.makeText(this@MainActivity, "Getting domains data....", Toast.LENGTH_SHORT).show()
       // }
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(urlApiDomainsList)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(
                        "myLog",
                        "$urlApiDomainsList | Error ${response.code} ${response.message}"
                    )
                }
                try {
                    isLoadedDomainsData = true
                    var responseJSON = JSONObject(response.body!!.string())
                    gvDomains = responseJSON.getJSONArray("data")
                    runOnUiThread {
                        gvDomainsTotal = gvDomains?.length()
                        tvTotalDomains!!.text = gvDomainsTotal.toString()
                        initUI()
                    }
                } catch (t: Throwable) {
                    Log.e("myLog", "Could not parse malformed JSON: \"$response.body!!.string()\"")
                }
            }
        } catch (e: IOException) {
            Log.d("myLog", "$urlApiDomainsList | Ошибка подключения: $e")
        }
    }

    suspend fun postResult() {
        Log.d("myLog", "postResult")
        val okHttpClient = OkHttpClient()

        // request 1
        var requestBody = gvProviderData.toString().toRequestBody()
        Log.d("myLog", gvProviderData.toString())
        var request = Request.Builder()
            .post(requestBody)
            .url(urlApiPostProviderData)
            .addHeader("Hash", gvHash)
            .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle this
            }

            override fun onResponse(call: Call, response: Response) {


                // request 2
                val jsonArray = JSONArray(resultArray)
                requestBody = jsonArray.toString().toRequestBody()
                Log.d("myLog", jsonArray.toString())
                request = Request.Builder()
                    .post(requestBody)
                    .url(urlApiPostResult)
                    .addHeader("Hash", gvHash)
                    .build()
                okHttpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // Handle this
                    }
                    override fun onResponse(call: Call, response: Response) {
                        // Handle this
                        showReportActivity()
                    }
                })
            }
        })
    }

    fun showReportActivity() {
        val reportIntent = Intent(this, ReportActivity::class.java)
        startActivity(reportIntent)
    }

    @SuppressLint("SetTextI18n")
    suspend fun makeNetworkRequest(client: OkHttpClient, index: Int, url: String): String {
        var resCode = ""
        var resMessage = ""
        val request = Request.Builder()
            .url(url)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d("myLog", "$url | Error ${response.code} ${response.message}")
                    resMessage = "Error ${response.code} ${response.message}"
                }
                Log.d("myLog", "$url | ${response.code} ")
                resCode = "${response.code}"
            }
        } catch (e: IOException) {
            Log.d("myLog", "$url | Ошибка подключения: $e")
            resMessage = "Ошибка подключения: $e"
        }
        resultArray[index][1] = "finished"
        resultArray[index][2] = resCode
        resultArray[index][3] = resMessage
        updateProcessState()
        runOnUiThread {
            tvLog.setText("${tvLog.text}\n\n${url} | ${resCode} | ${resMessage}");
        }
        return "${url} | ${resCode} | ${resMessage}"
    }

    private fun initUI() {

        if (isLoadedProviderData && isLoadedDomainsData)
        {
            btnMain.visibility = View.VISIBLE
            btnReport.visibility = View.VISIBLE
            btnReport.isEnabled = false

            /*********      BTN MAIN CLICK      **********/
            btnMain.setOnClickListener {

                isOnline=isOnline()
                if (isOnline) {


                    Toast.makeText(this@MainActivity, "Job started......", Toast.LENGTH_SHORT)
                        .show()
                    tvChecked.setText("0")
                    tvAlive.setText("0")
                    btnMain.isEnabled = false
                    btnReport.isEnabled = false
                    resultArray = arrayOf()
                    tvLog.setText("");

                    GlobalScope.launch(Dispatchers.Default) {
                        getProviderData();
                    }

                    val client = OkHttpClient()
                    (0 until gvDomains!!.length()).forEach {
                        val domain = gvDomains!![it].toString()
                        resultArray += arrayOf(
                            domain,
                            "inProgress",
                            "",
                            ""
                        ) //domain, state, code, message
                        GlobalScope.launch(Dispatchers.Default) {
                            val result = makeNetworkRequest(client, it, "https://${domain}")
                        }
                    }
                    Toast.makeText(this@MainActivity, "All requests has sent", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(this@MainActivity, "Схоже Ви не підключені до Інтернету", Toast.LENGTH_SHORT).show()
                    btnCheckConn.visibility = View.VISIBLE
                }



            }

            /*********      BTN REPORT CLICK      **********/
            btnReport.setOnClickListener {
                Toast.makeText(this@MainActivity, "Making report......", Toast.LENGTH_SHORT).show()
                GlobalScope.launch(Dispatchers.Default) {
                    postResult()
                }
            }

        }
    }

    private fun updateProcessState() {
        gvDomainsChecked = 0
        gvDomainsAlive = 0
        for (item in resultArray) {
            if (item[1] == "finished") {
                gvDomainsChecked = gvDomainsChecked!! + 1
            }
            if (item[2] == "200" || item[2] == "203") {
                gvDomainsAlive = gvDomainsAlive!! + 1
            }
        }
        runOnUiThread {
            tvChecked.setText(gvDomainsChecked.toString())
            tvAlive.setText(gvDomainsAlive.toString())
        }
        if (gvDomainsChecked == gvDomainsTotal) {
            runOnUiThread {
               // btnMain.visibility = View.VISIBLE

                btnMain.isEnabled = true
                btnReport.isEnabled = true
                Toast.makeText(this@MainActivity, "Checking has finished", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d("myLog", "${gvDomainsTotal} / ${gvDomainsChecked} / ${gvDomainsAlive}")
    }

    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }



}


