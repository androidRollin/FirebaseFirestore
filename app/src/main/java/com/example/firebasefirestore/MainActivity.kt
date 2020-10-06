package com.example.firebasefirestore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private val personColllectionRef = Firebase.firestore.collection("persons")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSave.setOnClickListener {
            val person = getOldPerson()
            savePerson(person)
        }

        btnRetrieve.setOnClickListener {
            retrievePerson()
        }

        btnUpdatePerson.setOnClickListener {
            val oldPerson = getOldPerson()
            val newPersonMap = getNewPersonMap()
            updatePerson(oldPerson, newPersonMap)
        }

        btnDeletePerson.setOnClickListener {
            val person = getOldPerson()
            deletePerson(person)
        }

        btnBatchWrite.setOnClickListener {
            changeName("L6DWr4YG6rkgNNk64pci","Mark","Suckerberg")
        }

        btnTransaction.setOnClickListener {
            birthday("L6DWr4YG6rkgNNk64pci")
        }

//        subscribeToRealtimeUpdates()
    }

    private fun birthday(personId: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            Firebase.firestore.runTransaction{transaction ->
                val personRef = personColllectionRef.document(personId)
                val person = transaction.get(personRef)
                val newAge = person["age"] as Long + 1
                transaction.update(personRef, "age", newAge)
            }.await()

        } catch (e: Exception) {
            withContext((Dispatchers.Main)) {
                Toast.makeText(
                    this@MainActivity, e.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    //batch write, write operations only
    private fun changeName(
        personId: String,
        newFirstName: String,
        newLastName: String
    ) = CoroutineScope(Dispatchers.IO).launch {
        try {
            Firebase.firestore.runBatch { batch ->
                val personRef = personColllectionRef.document(personId)
                batch.update(personRef, "firstName", newFirstName)
                batch.update(personRef, "lastName", newLastName)
            }.await()
        } catch (e: Exception) {
            withContext((Dispatchers.Main)) {
                Toast.makeText(
                    this@MainActivity, e.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getNewPersonMap(): Map<String, Any> {
        val firstName = etNewName.text.toString()
        val lastName = etNewLastName.text.toString()
        val age = etNewAge.text.toString()
        val map = mutableMapOf<String, Any>()
        if(firstName.isNotEmpty()){
            map["firstName"] = firstName
        }
        if(lastName.isNotEmpty()){
            map["lastName"] = lastName
        }
        if(age.isNotEmpty()){
            map["age"] = age.toInt()
        }
        return map
    }

    private fun deletePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personColllectionRef
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age)
            .get()
            .await()
        if(personQuery.documents.isNotEmpty()){
            for(document in personQuery){
                try{
                    personColllectionRef.document(document.id).delete().await()
//                    personColllectionRef.document(document.id).update(mapOf(
//                        "firstName" to FieldValue.delete()
//                    ))
                }
                catch (e: Exception){
                    withContext((Dispatchers.Main)){
                        Toast.makeText(this@MainActivity,e.message,
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        else{
            withContext((Dispatchers.Main)){
                Toast.makeText(this@MainActivity,"No person matched the query",
                    Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun updatePerson(person: Person, newPersonMap: Map<String, Any>) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personColllectionRef
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age)
            .get()
            .await()
        if(personQuery.documents.isNotEmpty()){
            for(document in personQuery){
                try{
                    /*Singular change*/
//                    personColllectionRef.document(document.id).update("firstName",person.firstName)
                    personColllectionRef.document(document.id).set(
                        newPersonMap,
                        SetOptions.merge()
                    ).await()
                }
                catch (e: Exception){
                    withContext((Dispatchers.Main)){
                        Toast.makeText(this@MainActivity,e.message,
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        else{
            withContext((Dispatchers.Main)){
                Toast.makeText(this@MainActivity,"No person matched the query",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getOldPerson(): Person{
        val firstName = edttxtFirstName.text.toString()
        val lastName = edttxtLastName.text.toString()
        val age = edttxtAge.text.toString().toInt()
        return Person(firstName, lastName, age)
    }

    private fun subscribeToRealtimeUpdates() {
        personColllectionRef
//            .whereEqualTo("firstName","Peter")
            .addSnapshotListener { value, error ->
            error?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            value?.let {
                val sb = StringBuilder()
                for (document in it) {
                    val person = document.toObject<Person>()
                    sb.append("$person\n")
                }
                txtData.text = sb.toString()
            }
        }
    }

    private fun retrievePerson() = CoroutineScope(Dispatchers.IO).launch {
        val minAge = edttxtMin.text.toString().toInt()
        val maxAge = edttxtMax.text.toString().toInt()
        try{
            //multiple queries in firestore
            val querySnapshot = personColllectionRef
                .whereGreaterThan("age",minAge)
                .whereLessThan("age",maxAge)
//                .whereEqualTo("firstName","Daisy")
                .orderBy("age")
                .get()
                .await()
            val sb = StringBuilder()
            for(document in querySnapshot.documents){
                val person = document.toObject<Person>()
                sb.append("$person\n")
            }
            withContext(Dispatchers.Main){
                txtData.text = sb.toString()
            }
        }catch (e: Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun savePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch{
        try{
            personColllectionRef.add(person).await()
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, "Successfully saved data.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}