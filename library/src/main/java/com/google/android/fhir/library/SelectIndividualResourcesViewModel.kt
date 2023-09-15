package com.google.android.fhir.library

import android.content.Context
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.lifecycle.ViewModel
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.library.dataClasses.IPSDocument
import com.google.android.fhir.library.dataClasses.Title
import com.google.android.fhir.library.utils.DocumentUtils
import com.google.android.fhir.library.utils.hasCode
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType

class SelectIndividualResourcesViewModel : ViewModel() {
  private var map = mapOf<Title, List<Resource>>()
  private val parser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
  private val documentGenerator = DocumentGenerator()
  private lateinit var patient: Resource
  private val checkBoxes = mutableListOf<CheckBox>()
  private val checkboxTitleMap = mutableMapOf<String, String>()

  fun initializeData(context: Context, containerLayout: LinearLayout, bundle : Bundle) {
    val docUtils = DocumentUtils()
    val doc = docUtils.readFileFromAssets(context, "immunizationBundle.json")
    val ipsDoc = IPSDocument(parser.parseResource(doc) as Bundle)
    ipsDoc.titles = ArrayList(documentGenerator.getTitlesFromDoc(ipsDoc))
    patient =
      bundle.entry.firstOrNull { it.resource.resourceType == ResourceType.Patient }?.resource
        ?: Patient()

    map = documentGenerator.displayOptions(
      context, ipsDoc, checkBoxes, checkboxTitleMap, containerLayout
    )
  }

  fun generateIPSDocument(): IPSDocument {
    val selectedValues = checkBoxes.filter { it.isChecked }.map { checkBox ->
      val title = Title(checkboxTitleMap[checkBox.text.toString()])
      val value = checkBox.text.toString()
      Pair(title, value)
    }
    val outputArray = selectedValues.flatMap { (title, value) ->
      map[title]?.filter { obj ->
        obj.hasCode().first?.coding?.any { it.display == value } == true
      } ?: emptyList()
    } + patient

    return documentGenerator.generateIPS(outputArray)
  }
}