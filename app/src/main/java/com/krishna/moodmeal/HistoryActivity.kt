@file:Suppress("DEPRECATION")

package com.krishna.moodmeal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class HistoryActivity : BaseActivity()  {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MealLogsAdapter
    private lateinit var emptyTextView: TextView
    private var mealLogs = listOf<MealLog>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        firestore = FirebaseFirestore.getInstance()

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // This enables caching
            .build()
        firestore.firestoreSettings = settings
        realtimeDatabase = FirebaseDatabase.getInstance("https://moodmeal-158cf-default-rtdb.europe-west1.firebasedatabase.app/")

        val toolbar = findViewById<MaterialToolbar>(R.id.historyTopAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.recyclerViewMealLogs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MealLogsAdapter()
        recyclerView.adapter = adapter

        emptyTextView = findViewById(R.id.textViewEmpty)

        loadMealLogs()
    }

    private fun loadMealLogs() {
        firestore.collection("mealLogs")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    android.util.Log.e("Firestore", "Error listening to meal logs", error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    mealLogs = querySnapshot.documents.mapNotNull { doc ->
                        val mealName = doc.getString("mealName")
                        val mood = doc.getString("mood")
                        val timestamp = doc.getLong("timestamp")
                        if (mealName != null && mood != null && timestamp != null) {
                            MealLog(mealName, mood, timestamp)
                        } else null
                    }
                    adapter.submitList(mealLogs)
                    invalidateOptionsMenu() // Refresh menu to show/hide delete button
                    updateEmptyState()
                }
            }
    }

    private fun updateEmptyState() {
        if (mealLogs.isEmpty()) {
            recyclerView.visibility = RecyclerView.GONE
            emptyTextView.visibility = TextView.VISIBLE
        } else {
            recyclerView.visibility = RecyclerView.VISIBLE
            emptyTextView.visibility = TextView.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.historyactivity_app_bar_menu, menu)
        return true
    }


    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_delete_all)?.isVisible = mealLogs.isNotEmpty()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_delete_all -> {
                confirmDeleteAllMealLogs()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmDeleteAllMealLogs() {
        AlertDialog.Builder(this)
            .setTitle("Delete All Logs")
            .setMessage("Are you sure you want to delete all meal logs?")
            .setPositiveButton("Delete") { _, _ -> deleteAllMealLogs() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAllMealLogs() {
        if (!isNetworkAvailable()) {
            showNoNetworkToast()
            return
        }
        firestore.collection("mealLogs")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = firestore.batch()

                for (document in querySnapshot.documents) {
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "All meal logs deleted", Toast.LENGTH_SHORT).show()
                        clearRecentMealInMainActivity()
                        clearRealtimeDatabaseLogs()

                        // Send result to MainActivity
                        val resultIntent = Intent()
                        resultIntent.putExtra("historyCleared", true)
                        setResult(RESULT_OK, resultIntent)

                        finish() // Close HistoryActivity
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("Firestore", "Error deleting meal logs: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("Firestore", "Error fetching meal logs: ${e.message}")
            }
    }

    private fun clearRecentMealInMainActivity() {
        // Clear shared preferences for recent meal if you’re using them
        val sharedPrefs = getSharedPreferences("MealPrefs", MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
    }


    private fun clearRealtimeDatabaseLogs() {

        if (!isNetworkAvailable()) {
            showNoNetworkToast()
            return
        }

        val realtimeDbRef = realtimeDatabase.reference
        // Reset total meals count
        realtimeDbRef.child("totalMealsLogged").setValue(0)
        // Remove all recent moods entries
        realtimeDbRef.child("recentMoods").removeValue()
            .addOnSuccessListener {
                Log.d("RealtimeDB", "recentMoods cleared successfully")
            }
            .addOnFailureListener { e ->
                Log.e("RealtimeDB", "Failed to clear recentMoods: ${e.message}")
            }
    }

}
