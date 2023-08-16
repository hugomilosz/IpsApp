package com.example.ipsapp

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.ipsapp.utils.UrlUtils
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpPost
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.entity.StringEntity
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.CloseableHttpClient
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.HttpClients
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.util.EntityUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject


class GenerateSHL : Activity() {

  private val urlUtils = UrlUtils()

  // Need to encode and compress into JWE and JWT tokens here
  private val file = "{\n" +
    "  \"resourceType\" : \"AllergyIntolerance\",\n" +
    "  \"id\" : \"allergyintolerance-with-abatement\",\n" +
    "  \"text\" : {\n" +
    "    \"status\" : \"extensions\",\n" +
    "    \"div\" : \"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><p><b>Generated Narrative: AllergyIntolerance</b><a name=\\\"allergyintolerance-with-abatement\\\"> </a></p><div style=\\\"display: inline-block; background-color: #d9e0e7; padding: 6px; margin: 4px; border: 1px solid #8da1b4; border-radius: 5px; line-height: 60%\\\"><p style=\\\"margin-bottom: 0px\\\">Resource AllergyIntolerance &quot;allergyintolerance-with-abatement&quot; </p></div><p><b>Allergy abatement date</b>: 2010</p><p><b>clinicalStatus</b>: Resolved <span style=\\\"background: LightGoldenRodYellow; margin: 4px; border: 1px solid khaki\\\"> (<a href=\\\"http://terminology.hl7.org/5.0.0/CodeSystem-allergyintolerance-clinical.html\\\">AllergyIntolerance Clinical Status Codes</a>#resolved)</span></p><p><b>verificationStatus</b>: Confirmed <span style=\\\"background: LightGoldenRodYellow; margin: 4px; border: 1px solid khaki\\\"> (<a href=\\\"http://terminology.hl7.org/5.0.0/CodeSystem-allergyintolerance-verification.html\\\">AllergyIntolerance Verification Status</a>#confirmed)</span></p><p><b>code</b>: Ragweed pollen <span style=\\\"background: LightGoldenRodYellow; margin: 4px; border: 1px solid khaki\\\"> (<a href=\\\"https://browser.ihtsdotools.org/\\\">SNOMED CT</a>#256303006)</span></p><p><b>patient</b>: <a href=\\\"Patient-66033.html\\\">Patient/66033</a> &quot; LUX-BRENNARD&quot;</p><p><b>onset</b>: </p></div>\"\n" +
    "  },\n" +
    "  \"extension\" : [{\n" +
    "    \"url\" : \"http://hl7.org/fhir/uv/ips/StructureDefinition/abatement-dateTime-uv-ips\",\n" +
    "    \"valueDateTime\" : \"2010\"\n" +
    "  }],\n" +
    "  \"clinicalStatus\" : {\n" +
    "    \"coding\" : [{\n" +
    "      \"system\" : \"http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical\",\n" +
    "      \"code\" : \"resolved\"\n" +
    "    }]\n" +
    "  },\n" +
    "  \"verificationStatus\" : {\n" +
    "    \"coding\" : [{\n" +
    "      \"system\" : \"http://terminology.hl7.org/CodeSystem/allergyintolerance-verification\",\n" +
    "      \"code\" : \"confirmed\"\n" +
    "    }]\n" +
    "  },\n" +
    "  \"code\" : {\n" +
    "    \"coding\" : [{\n" +
    "      \"system\" : \"http://snomed.info/sct\",\n" +
    "      \"code\" : \"256303006\",\n" +
    "      \"display\" : \"Ragweed pollen\"\n" +
    "    }]\n" +
    "  },\n" +
    "  \"patient\" : {\n" +
    "    \"reference\" : \"Patient/66033\"\n" +
    "  },\n" +
    "  \"_onsetDateTime\" : {\n" +
    "    \"extension\" : [{\n" +
    "      \"url\" : \"http://hl7.org/fhir/StructureDefinition/data-absent-reason\",\n" +
    "      \"valueCode\" : \"unknown\"\n" +
    "    }]\n" +
    "  }\n" +
    "}"

  @RequiresApi(Build.VERSION_CODES.O)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.view_shl)

    val passcode:String = intent.getStringExtra("passcode").toString()
    val labelData:String = intent.getStringExtra("label").toString()
    val expirationDate:String = intent.getStringExtra("expirationDate").toString()
    val codingList = intent.getStringArrayListExtra("codingList")

    val passcodeField = findViewById<TextView>(R.id.passcode)
    val expirationDateField = findViewById<TextView>(R.id.expirationDate)
    passcodeField.text = passcode
    expirationDateField.text = expirationDate

    if (codingList != null) {
      generatePayload(passcode, labelData, expirationDate, codingList)
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  @RequiresApi(Build.VERSION_CODES.O)
  fun generatePayload(passcode: String, labelData: String, expirationDate: String, codingList : ArrayList<String>) {
    val qrView = findViewById<ImageView>(R.id.qrCode)

    GlobalScope.launch(Dispatchers.IO) {
      val httpClient: CloseableHttpClient = HttpClients.createDefault()
      val httpPost = HttpPost("https://api.vaxx.link/api/shl")
      httpPost.addHeader("Content-Type", "application/json")

      // Recipient and passcode entered by the user on this screen
      val jsonData : String
      var flags = ""
      if (passcode != "") {
        flags = "P"
        jsonData = "{\"passcode\" : \"$passcode\"}"
      } else {
        jsonData = "{}"
      }
      val entity = StringEntity(jsonData)

      httpPost.entity = entity
      val response = httpClient.execute(httpPost)

      val responseBody = EntityUtils.toString(response.entity, StandardCharsets.UTF_8)
      Log.d("Response status: ", "${response.statusLine.statusCode}")
      Log.d("Response body: ", responseBody)

      httpClient.close()

      val jsonPostRes = JSONObject(responseBody)


      // Look at this manifest url
      val manifestUrl = "https://api.vaxx.link/api/shl/${jsonPostRes.getString("id")}"
      Log.d("manifest", manifestUrl)
      val key = urlUtils.generateRandomKey()
      val managementToken = jsonPostRes.getString("managementToken")

      var exp = ""
      if (expirationDate != "") {
        exp = urlUtils.dateStringToEpochSeconds(expirationDate).toString()
      }

      val shLinkPayload = constructSHLinkPayload(manifestUrl, labelData, flags, key, exp)

      // fix this link and put the logo in the middle
      // probably don't need the viewer
      val shLink = "https://demo.vaxx.link/viewer#shlink:/${shLinkPayload}"
      // val shLink = "shlink:/$shLinkPayload"

      // val logoPath = "app/src/main/assets/smart-logo.png"
      // val logoScale = 0.06

      // val drawableResource = R.drawable.smart_logo
      // val drawable = ContextCompat.getDrawable(this, drawableResource)

      // if (drawable != null) {

      val qrCodeBitmap = generateQRCode(this@GenerateSHL, shLink)
      if (qrCodeBitmap != null) {
        runOnUiThread {
          qrView.setImageBitmap(qrCodeBitmap)
        }
      }
      println(shLinkPayload)

      val jsonArray = JSONArray()
      for (item in codingList) {
        jsonArray.put(item)
      }

      val jsonObject = org.json.JSONObject()
      jsonObject.put("items", jsonArray)
      urlUtils.postPayload(jsonObject.toString(), manifestUrl, key, managementToken)
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  fun constructSHLinkPayload(
    manifestUrl: String,
    label: String?,
    flags: String?,
    key: String,
    exp: String?,
  ): String {
    val payloadObject = JSONObject()
    payloadObject.put("url", manifestUrl)
    payloadObject.put("key", key)

    payloadObject.put("flag", flags)

    if (label != "") {
      payloadObject.put("label", label)
    }

    if (exp != "") {
      payloadObject.put("exp", exp)
    }

    val jsonPayload = payloadObject.toString()
    return urlUtils.base64UrlEncode(jsonPayload)
  }

  private fun generateQRCode(context: Context, content: String): Bitmap? {
    val logoScale = 0.5
    try {
      val hints = mutableMapOf<EncodeHintType, Any>()
      hints[EncodeHintType.MARGIN] = 2

      val qrCodeWriter = QRCodeWriter()
      val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
      val width = bitMatrix.width
      val height = bitMatrix.height
      val qrCodeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

      for (x in 0 until width) {
        for (y in 0 until height) {
          qrCodeBitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
        }
      }

      val logoDrawable = ContextCompat.getDrawable(context, R.drawable.smart_logo)
      val logoAspectRatio = logoDrawable!!.intrinsicWidth.toFloat() / logoDrawable.intrinsicHeight.toFloat()

      val logoWidth = (width * logoScale).toInt()
      val logoHeight = (logoWidth / logoAspectRatio).toInt()

      val logoBitmap = convertDrawableToBitmap(logoDrawable, logoWidth, logoHeight)


      val backgroundBitmap = Bitmap.createBitmap(logoBitmap.width, logoBitmap.height, Bitmap.Config.RGB_565)
      backgroundBitmap.eraseColor(Color.WHITE)

      val canvas = Canvas(backgroundBitmap)
      canvas.drawBitmap(logoBitmap, 0f, 0f, null)

      val centerX = (width - logoBitmap.width) / 2
      val centerY = (height - logoBitmap.height) / 2

      val finalBitmap = Bitmap.createBitmap(qrCodeBitmap)
      val finalCanvas = Canvas(finalBitmap)
      finalCanvas.drawBitmap(backgroundBitmap, centerX.toFloat(), centerY.toFloat(), null)

      return finalBitmap
    } catch (e: WriterException) {
      e.printStackTrace()
      return null
    }
  }

  private fun convertDrawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
  }
}
