package com.example.ipsapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpGet
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpPost
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.entity.StringEntity
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.CloseableHttpClient
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.HttpClients
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.util.EntityUtils
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject


class SuccessfulScan : AppCompatActivity() {

  @RequiresApi(Build.VERSION_CODES.O)
  override fun onCreate (savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.successfulscan)


    val scannedData:String = intent.getStringExtra("scannedData").toString()
    val extractedJson = extractUrl(scannedData);
    val decodedJson = decodeUrl(extractedJson);

    // this gets you the url needed for the POST request
    val json = decodedJson?.let { String(it, StandardCharsets.UTF_8) }
    val jsonObject = JSONObject(json)
    val url = jsonObject.get("url")
    val key = jsonObject.get("key")
    Log.d("url", url.toString())
    Log.d("key", key.toString())
    /*
    will eventually need to check for passcodes etc but just do simple POST for now:
    curl -X POST "https://api.vaxx.link/api/shl/KTseQR32kkQ4Q04aIjM1niOuxDOn1jZ_vYQb5jfIqCs" -H "Content-Type: application/json" -d @test.json
    where test.json = { "recipient": "anystring"}"
    This will give you back a lot of data like:
      {"files":[{"contentType":"application/smart-health-card","embedded":"eyJhbGciOiJkaXIiLCJ...
    */

    // when button is pushed, the inputted data is passed into fetchData()
    val button = findViewById<Button>(R.id.getData)
    button.setOnClickListener {
      val recipientField = findViewById<EditText>(R.id.recipient).text.toString()
      val passcodeField = findViewById<EditText>(R.id.passcode).text.toString()
      fetchData(url.toString(), recipientField, passcodeField, key.toString())
    }
  }


  // Handles the POST request and passes the response body into the next activity
  private fun fetchData (url: String, recipient: String, passcode: String, key: String) {
    GlobalScope.launch(Dispatchers.IO) {
      val httpClient: CloseableHttpClient = HttpClients.createDefault()
      val httpPost: HttpPost = HttpPost(url)
      httpPost.addHeader("Content-Type", "application/smart-health-card")

      // Recipient and passcode entered by the user on this screen
      var jsonData = ""
      if (passcode == "") {
        jsonData = "{\"recipient\":\"${recipient}\"}"
      } else {
        jsonData = "{\"passcode\":\"${passcode}\", \"recipient\":\"${recipient}\"}"
      }
      val entity = StringEntity(jsonData)

      httpPost.entity = entity
      val response = httpClient.execute(httpPost)

      val responseBody = EntityUtils.toString(response.entity, StandardCharsets.UTF_8)
      Log.d("Response status: ", "${response.statusLine.statusCode}")
      Log.d("Response body: ", responseBody)
      httpClient.close()

      val jsonObject = JSONObject(responseBody)

      val filesArray = jsonObject.getJSONArray("files")

      // create a string array and add the 'embedded' data to it
      // need to work out what to do when it has a location instead
      val embeddedList = ArrayList<String>()
      val locationList = ArrayList<String>()
      for (i in 0 until filesArray.length()) {
        val fileObject = filesArray.getJSONObject(i)
        if (fileObject.has("embedded")) {
          val embeddedValue = fileObject.getString("embedded")
          embeddedList.add(embeddedValue)
        } else {
          val loc = fileObject.getString("location")
          getRequest(loc)?.let { embeddedList.add(it) }
          //fetchData(loc, recipient, "", key)
          // locationList.add(loc)
          Log.d("here", loc)
        }
      }

      val embeddedArray = embeddedList.toTypedArray()
      val locationArray = locationList.toTypedArray()
      //Log.d("embedded 2", embeddedArray[1])

      launch(Dispatchers.Main) {
        val i = Intent(this@SuccessfulScan, GetData::class.java)
        i.putExtra("embeddedArray", embeddedArray)
        // i.putExtra("locationArray", locationArray)
        i.putExtra("key", key)
        startActivity(i)
      }
    }
  }

  private fun getRequest(url: String): String? {
    val httpClient: CloseableHttpClient = HttpClients.createDefault()
    val httpGet: HttpGet = HttpGet(url)

    val response = httpClient.execute(httpGet)

    val responseBody = EntityUtils.toString(response.entity, StandardCharsets.UTF_8)
    Log.d("Response status: ", "${response.statusLine.statusCode}")
    Log.d("Response body: ", responseBody)
    httpClient.close()

    return responseBody
  }
}