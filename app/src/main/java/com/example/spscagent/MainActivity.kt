package com.example.spscagent


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.math.BigInteger
import java.security.MessageDigest


const val urlApiProvider ="http://ip-api.com/json/"
//const val urlApiDomainsList ="https://my.everyday.ua/api/spscMobileAppGetDomains"
const val urlApiDomainsList ="https://my.everyday.ua/api/spscMobileAppGetDomainsTest"
const val urlApiPostResult ="https://my.everyday.ua/api/spscMobileAppPostResult"
const val urlApiPostProviderData ="https://my.everyday.ua/api/spscMobileAppPostProviderData"
const val urlApiShowReport ="https://my.everyday.ua/api/spscMobileAppShowReport"

var gvProviderData: JSONObject?=null

var gvDomainsTotal: Int?=null
var gvDomainsChecked: Int?=null
var gvDomainsAlive: Int?=null
var gvDomains: JSONArray?=null
var resultArray= arrayOf<Array<String>>()
var gvHash: String=""

class MainActivity : AppCompatActivity() {

    lateinit var tvLog: TextView;
    lateinit var tvChecked: TextView;
    lateinit var tvAlive: TextView;
    lateinit var tvProviderIp: TextView
    lateinit var tvProviderName: TextView
    lateinit var tvProviderCity: TextView
    lateinit var btnMain: Button;
    lateinit var btnReport: Button;
    lateinit var tvTotalDomains: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        tvChecked = findViewById(R.id.domainsChecked);
        tvAlive = findViewById(R.id.domainsAlive);
        tvLog = findViewById(R.id.tvLog);
        tvProviderIp = findViewById(R.id.ip);
        tvProviderName = findViewById(R.id.providerName);
        tvProviderCity = findViewById(R.id.providerCity);
        tvTotalDomains = findViewById(R.id.domainsCount);

        btnMain = findViewById(R.id.buttonCheck);
        btnReport = findViewById(R.id.btnReport);

        GlobalScope.launch (Dispatchers.Default) {
            getProviderData();
        }
        GlobalScope.launch (Dispatchers.Default) {
            getDomainsData();
        }
    }

    suspend fun getProviderData(){
        Log.d("myLog", "getProviderData")
        val client = OkHttpClient()
        val  request = Request.Builder()
            .url(urlApiProvider)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d("myLog", "$urlApiProvider | Error ${response.code} ${response.message}")
                }
                try {
                    gvProviderData= JSONObject(response.body!!.string())
                    runOnUiThread {
                        tvProviderIp.setText(gvProviderData!!.getString("query"));
                        tvProviderName.setText(gvProviderData!!.getString("isp"));
                        tvProviderCity.setText(gvProviderData!!.getString("regionName"));
                    }
                    Log.d("myLog", gvProviderData!!.getString("status"))
                } catch (t: Throwable) {
                    Log.e("myLog", "Could not parse malformed JSON: \"$gvProviderData\"")
                }
            }
        } catch (e: IOException) {
            Log.d("myLog", "$urlApiProvider | Ошибка подключения: $e")
        }
    }
    
    suspend fun getDomainsData(){
        Log.d("myLog", "getDomainsData")
        val client = OkHttpClient()
        val  request = Request.Builder()
            .url(urlApiDomainsList)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d("myLog", "$urlApiDomainsList | Error ${response.code} ${response.message}")
                }
                try {
                    var responseJSON= JSONObject(response.body!!.string())
                    gvDomains = responseJSON.getJSONArray("data")
                    runOnUiThread {
                        gvDomainsTotal=gvDomains?.length()
                        tvTotalDomains!!.text = gvDomainsTotal.toString()
                        showButton()
                    }
                } catch (t: Throwable) {
                    Log.e("myLog", "Could not parse malformed JSON: \"$response.body!!.string()\"")
                }
            }
        } catch (e: IOException) {
            Log.d("myLog", "$urlApiDomainsList | Ошибка подключения: $e")
        }
    }

    suspend fun postResult(){
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
                // Handle this

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

    fun showReportActivity(){
        val reportIntent = Intent(this, ReportActivity::class.java)
        startActivity(reportIntent)
    }

    suspend fun makeNetworkRequest(client: OkHttpClient,index:Int,url: String):String {
        var resCode=""
        var resMessage=""
       val  request = Request.Builder()
           .url(url)
           .build()
       try {
           client.newCall(request).execute().use { response ->
               if (!response.isSuccessful) {

                   Log.d("myLog", "$url | Error ${response.code} ${response.message}")
                   resMessage= "Error ${response.code} ${response.message}"
               }

               Log.d("myLog", "$url | ${response.code} ")
               resCode= "${response.code}"

           }
       } catch (e: IOException) {

           Log.d("myLog", "$url | Ошибка подключения: $e")
           resMessage= "Ошибка подключения: $e"

       }
       resultArray[index][1]="finished"
       resultArray[index][2]="${resCode}"
       resultArray[index][3]="${resMessage}"

       updateProcessState()

       runOnUiThread {

           tvLog.setText("${tvLog.text}\n\n${url} | ${resCode} | ${resMessage}");
       }
       return "${url} | ${resCode} | ${resMessage}"
    }

    private fun showButton() {
        btnMain.visibility= View.VISIBLE
        btnMain.setOnClickListener {
            Toast.makeText(this@MainActivity, "Start checking", Toast.LENGTH_SHORT).show()
            btnMain.visibility= View.INVISIBLE
                val client = OkHttpClient()
                (0 until gvDomains!!.length()).forEach {
                    val domain = gvDomains!![it].toString()
                    resultArray+=arrayOf(domain, "inProgress", "","") //domain, state, code, message
                    GlobalScope.launch (Dispatchers.Default) {
                    val result=makeNetworkRequest(client,it,"https://${domain}")
                }
            }
        }
        btnReport.setOnClickListener {
            Toast.makeText(this@MainActivity, "Start reporting", Toast.LENGTH_SHORT).show()

            val currentTimestamp = System.currentTimeMillis()
            gvHash=md5(gvProviderData!!.getString("query")+currentTimestamp.toString())

            GlobalScope.launch (Dispatchers.Default) {
                postResult()
            }
        }
    }

    private fun updateProcessState() {
        gvDomainsChecked=0
        gvDomainsAlive=0
        for (item in resultArray) {
            if (item[1]=="finished"){
                gvDomainsChecked = gvDomainsChecked!! + 1
            }
            if (item[2]=="200" || item[2]=="203"){
                gvDomainsAlive = gvDomainsAlive!! + 1
            }
        }
        runOnUiThread {
            tvChecked.setText(gvDomainsChecked.toString())
            tvAlive.setText(gvDomainsAlive.toString())
          //  Toast.makeText(this@MainActivity, "Finish checking", Toast.LENGTH_SHORT).show()

        }
        if (gvDomainsChecked== gvDomainsTotal){
            runOnUiThread {
            //    btnMain.visibility = View.VISIBLE
                btnReport.visibility = View.VISIBLE
                Toast.makeText(this@MainActivity, "Finish checking", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d("myLog", "${gvDomainsTotal} / ${gvDomainsChecked} / ${gvDomainsAlive}")
    }

    fun md5(input:String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

}


