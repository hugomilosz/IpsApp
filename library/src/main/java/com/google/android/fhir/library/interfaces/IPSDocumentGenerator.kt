package com.google.android.fhir.library.interfaces

import android.content.Context
import android.widget.CheckBox
import android.widget.LinearLayout
import com.google.android.fhir.library.dataClasses.IPSDocument
import com.google.android.fhir.library.dataClasses.Title
import org.hl7.fhir.r4.model.Resource

interface IPSDocumentGenerator {
  fun getTitlesFromDoc(doc : IPSDocument) : List<Title>

  fun getDataFromDoc(doc : IPSDocument) : Map<Title, List<Resource>>

  fun generateIPS(selectedResources : List<Resource>): IPSDocument

  fun displayOptions(
    context : Context,
    bundle: IPSDocument?,
    checkBoxes: MutableList<CheckBox>,
    checkboxTitleMap: MutableMap<String, String>,
    containerLayout: LinearLayout
  ): Map<Title, List<Resource>>

}