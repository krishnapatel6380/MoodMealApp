@file:Suppress("DEPRECATION")

package com.krishna.moodmeal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration


class MainActivity : BaseActivity()  {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var realtimeDb: FirebaseDatabase

    private lateinit var moodButtons: List<MaterialButton>
    private var selectedMood: String? = null

    private lateinit var mealSuggestionsList: LinearLayout
    private lateinit var buttonLogMeal: MaterialButton
    private lateinit var textTotalMealsLogged: TextView
    private lateinit var recentMoodsList: LinearLayout
    private lateinit var textMealSuggestionsLabel: TextView

    private var currentMealSuggestions: List<String> = emptyList()
    private var selectedMeal: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        firestore = FirebaseFirestore.getInstance()

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // This enables caching
            .build()
        firestore.firestoreSettings = settings
        realtimeDb = FirebaseDatabase.getInstance("https://moodmeal-158cf-default-rtdb.europe-west1.firebasedatabase.app/")

        val topAppBar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)

        moodButtons = listOf(
            findViewById(R.id.buttonHappy),
            findViewById(R.id.buttonSad),
            findViewById(R.id.buttonRelaxed),
            findViewById(R.id.buttonEnergetic),
            findViewById(R.id.buttonAngry),
            findViewById(R.id.buttonStressed)
        )
        mealSuggestionsList = findViewById(R.id.mealSuggestionsList)
        buttonLogMeal = findViewById(R.id.buttonLogMeal)
        textTotalMealsLogged = findViewById(R.id.textTotalMealsLogged)
        recentMoodsList = findViewById(R.id.recentMoodsList)
        textMealSuggestionsLabel = findViewById(R.id.textMealSuggestionsLabel)

        moodButtons.forEach { button ->
            button.setOnClickListener {
                selectMood(button.text.toString())
            }
        }

        buttonLogMeal.setOnClickListener {
            logSelectedMeal()
        }

        listenTotalMealsLogged()
        listenRecentMoods()
        seedDatabaseIfNeeded()
    }


    private fun selectMood(mood: String) {
        selectedMood = mood
        moodButtons.forEach { button ->
            val buttonMood = button.text.toString().lowercase()
            val isSelected = buttonMood == mood.lowercase()

            val bgColor = when (buttonMood) {
                "happy" -> if (isSelected) R.color.happy_selected else R.color.happy_unselected
                "sad" -> if (isSelected) R.color.sad_selected else R.color.sad_unselected
                "relaxed" -> if (isSelected) R.color.relaxed_selected else R.color.relaxed_unselected
                "angry" -> if (isSelected) R.color.angry_selected else R.color.angry_unselected
                "energetic" -> if (isSelected) R.color.energetic_selected else R.color.energetic_unselected
                "stressed" -> if (isSelected) R.color.stressed_selected else R.color.stressed_unselected
                else -> if (isSelected) R.color.primaryContainer else R.color.primaryContainer
            }

            button.backgroundTintList = ContextCompat.getColorStateList(this, bgColor)

            button.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isSelected) R.color.text_selected else R.color.text_unselected
                )
            )
        }

        textMealSuggestionsLabel.text = "Meal Suggestions for \"$mood\":"
        loadMealSuggestions(mood)
    }


    private var mealSuggestionsListener: ListenerRegistration? = null



    private fun loadMealSuggestions(mood: String) {
        // Remove previous listener if exists
        mealSuggestionsListener?.remove()

        mealSuggestionsList.removeAllViews()
        selectedMeal = null
        buttonLogMeal.isEnabled = false

        mealSuggestionsListener = firestore.collection("moodSuggestions")
            .document(mood)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load meals.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val suggestions = document.get("suggestions") as? List<*>
                    val meals = suggestions?.filterIsInstance<String>() ?: emptyList()
                    currentMealSuggestions = meals

                    mealSuggestionsList.removeAllViews()

                    if (meals.isEmpty()) {
                        val tv = TextView(this).apply {
                            text = "No suggestions available."
                            setPadding(16, 8, 16, 8)
                        }
                        mealSuggestionsList.addView(tv)
                        return@addSnapshotListener
                    }

                    meals.forEach { mealName ->
                        val cardView = MaterialCardView(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 4, 0, 4)
                            }
                            radius = 16f
                            strokeWidth = 3
                            strokeColor = ContextCompat.getColor(context, R.color.primaryContainer)
                            setCardBackgroundColor(ContextCompat.getColor(context, R.color.surfaceColor))
                            isClickable = true
                            isFocusable = true
                            foreground = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)
                            preventCornerOverlap = true

                            useCompatPadding = true
                        }

                        val textView = TextView(this).apply {
                            text = mealName
                            textSize = 16f
                            setPadding(24, 24, 24, 24)
                            setTextColor(ContextCompat.getColor(context, R.color.onSurfaceColor))
                        }

                        cardView.addView(textView)

                        cardView.setOnClickListener {
                            // Reset all cards to default style
                            for (i in 0 until mealSuggestionsList.childCount) {
                                val child = mealSuggestionsList.getChildAt(i) as MaterialCardView
                                child.strokeColor = ContextCompat.getColor(this, R.color.primaryContainer)
                                child.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surfaceColor))
                            }
                            // Highlight selected card
                            cardView.strokeColor = ContextCompat.getColor(this, R.color.primaryColor)
                            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primaryContainer))

                            selectedMeal = mealName
                            buttonLogMeal.isEnabled = true
                        }

                        mealSuggestionsList.addView(cardView)
                    }
                } else {
                    mealSuggestionsList.removeAllViews()
                    val tv = TextView(this).apply {
                        text = "No suggestions available."
                        setPadding(16, 8, 16, 8)
                    }
                    mealSuggestionsList.addView(tv)
                }
            }
    }

    private fun logSelectedMeal() {
        if (!isNetworkAvailable()) {
            showNoNetworkToast()
            return
        }

        val mood = selectedMood ?: return
        val meal = selectedMeal ?: return

        val logEntry = hashMapOf(
            "mealName" to meal,
            "mood" to mood,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("mealLogs")
            .add(logEntry)
            .addOnSuccessListener {
                Toast.makeText(this, "Meal logged!", Toast.LENGTH_SHORT).show()
                updateRealtimeDb(mood)
                selectedMeal = null
                buttonLogMeal.isEnabled = false
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to log meal.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRealtimeDb(mood: String) {
        if (!isNetworkAvailable()) {
            showNoNetworkToast()
            return
        }

        val totalMealsRef = realtimeDb.reference.child("totalMealsLogged")
        val recentMoodsRef = realtimeDb.reference.child("recentMoods")

        totalMealsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                var currentValue = currentData.getValue(Int::class.java)
                if (currentValue == null) currentValue = 0
                currentData.value = currentValue + 1
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("RealtimeDB", "Transaction failed: ${error.message}")
                }
            }
        })
        val newMoodEntryRef = recentMoodsRef.push()
        val moodData = mapOf("mood" to mood, "timestamp" to System.currentTimeMillis())
        newMoodEntryRef.setValue(moodData)
            .addOnSuccessListener {
                Log.d("RealtimeDB", "Mood entry added successfully")
            }
            .addOnFailureListener { e ->
                Log.e("RealtimeDB", "Failed to add mood entry: ${e.message}")
            }
    }

    private fun listenTotalMealsLogged() {
        realtimeDb.reference.child("totalMealsLogged")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val total = snapshot.getValue(Int::class.java) ?: 0
                    textTotalMealsLogged.text = "Total meals logged: $total"
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Log.e("RealtimeDB", "Load total meals cancelled: ${error.message}")
                }
            })
    }
    private fun listenRecentMoods() {
        realtimeDb.reference.child("recentMoods")
            .orderByChild("timestamp")
            .limitToLast(5)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    recentMoodsList.removeAllViews()

                    val moods = snapshot.children.mapNotNull {
                        val mood = it.child("mood").getValue(String::class.java)
                        val ts = it.child("timestamp").getValue(Long::class.java)
                        if (mood != null && ts != null) Pair(mood, ts) else null
                    }.sortedByDescending { it.second }

                    if (moods.isEmpty()) {
                        val tv = MaterialTextView(this@MainActivity).apply {
                            text = "No recent logout"
                            setPadding(24, 16, 24, 16)
                            setTextAppearance(R.style.TextBodyLarge)
                            setTextColor(
                                MaterialColors.getColor(
                                    this,
                                    com.google.android.material.R.attr.colorOnSurface,
                                    0
                                )
                            )
                            background = ContextCompat.getDrawable(context, R.drawable.bg_rounded_light)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 8, 0, 8)
                            }
                        }
                        recentMoodsList.addView(tv)
                        return
                    }

                    moods.forEach { (mood, ts) ->
                        val tv = MaterialTextView(this@MainActivity).apply {
                            text = "- $mood (${formatTimestamp(ts)})"
                            setPadding(24, 16, 24, 16)
                            setTextAppearance(R.style.TextBodyLarge)
                            setTextColor(
                                MaterialColors.getColor(
                                    this,
                                    com.google.android.material.R.attr.colorOnSurface,
                                    0
                                )
                            )
                            background = ContextCompat.getDrawable(context, R.drawable.bg_rounded_light)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 8, 0, 8)
                            }
                        }
                        recentMoodsList.addView(tv)
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Log.e("RealtimeDB", "Load recent moods cancelled: ${error.message}")
                }
            })
    }


    private fun formatTimestamp(ts: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(ts))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_history) {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivityForResult(intent, 1001)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == RESULT_OK && data?.getBooleanExtra("historyCleared", false) == true) {
            // Refresh UI after logs deleted
            recentMoodsList.removeAllViews()
            textTotalMealsLogged.text = "Total meals logged: 0"
            seedDatabaseIfNeeded()
        }
    }

    private fun seedDatabaseIfNeeded() {
        val totalMealsRef = realtimeDb.reference.child("totalMealsLogged")
        totalMealsRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                totalMealsRef.setValue(0)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        mealSuggestionsListener?.remove()
    }
}
