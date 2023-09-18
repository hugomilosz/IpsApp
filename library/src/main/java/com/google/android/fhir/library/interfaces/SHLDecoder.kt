package com.google.android.fhir.library.interfaces

import com.google.android.fhir.library.dataClasses.IPSDocument

interface SHLDecoder {

  suspend fun decodeSHLToDocument(recipient: String) : IPSDocument?
  fun storeDocument(doc : IPSDocument)

  fun hasPasscode() : Boolean
}