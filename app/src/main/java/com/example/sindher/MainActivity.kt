package com.example.sindher

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.rememberCoroutineScope
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.sindher.ui.theme.SindherTheme
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.io.File

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SindherTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }

        // Initialize Firebase
        val database = Firebase.database
        val storage = Firebase.storage
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var showDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Numbers", "Family", "Colors", "Phrases")

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo_n),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sindher")
                    }
                }
            )
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
            when (selectedTab) {
                0 -> NumbersScreen()
                1 -> FamilyScreen()
                2 -> ColorsScreen()
                3 -> PhrasesScreen()
            }
        }

        // Floating action button
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp, 50.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
        }

        if (showDialog) {
            AddItemDialog(onDismiss = { showDialog = false })
        }
    }
}

@Composable
fun AddItemDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val categories = listOf("Numbers", "Colors", "Family", "Phrases")
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var sindhiTranslation by remember { mutableStateOf("") }
    var englishTranslation by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Firebase references
    val database: DatabaseReference = Firebase.database.reference
    val storage = Firebase.storage

    // Launcher for selecting an image from gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        uri?.let {
            val imageFile = File(context.filesDir, "images/${uri.lastPathSegment}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    // Launcher for recording audio
    val audioRecorderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            val uri = result.data?.data
            audioUri = uri
            uri?.let {
                val audioFile = File(context.filesDir, "audio/${uri.lastPathSegment}")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    audioFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Item") },
        text = {
            Column {
                Box {
                    Text(
                        text = selectedCategory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                            .background(Color.LightGray)
                            .padding(16.dp)
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                },
                                text = { Text(text = category) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = sindhiTranslation,
                    onValueChange = { sindhiTranslation = it },
                    label = { Text("Sindhi Translation") }
                )

                OutlinedTextField(
                    value = englishTranslation,
                    onValueChange = { englishTranslation = it },
                    label = { Text("English Translation") }
                )

                if (selectedCategory != "Phrases") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        imagePickerLauncher.launch("image/*")
                    }) {
                        Text(text = "Pick Image")
                    }
                    imageUri?.let { uri ->
                        Image(painter = rememberAsyncImagePainter(uri), contentDescription = null)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "audio/*"
                        }
                        audioRecorderLauncher.launch(intent)
                    }) {
                        Text(text = "Record Audio")
                    }
                    audioUri?.let { uri ->
                        BasicText(text = uri.toString())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                coroutineScope.launch {
                    val key = database.child("items").push().key ?: return@launch
                    val itemData = mapOf(
                        "category" to selectedCategory,
                        "sindhiTranslation" to sindhiTranslation,
                        "englishTranslation" to englishTranslation
                    )
                    database.child("items").child(key).setValue(itemData)

                    imageUri?.let { uri ->
                        val imageRef = storage.reference.child("images/${uri.lastPathSegment}")
                        imageRef.putFile(uri).await()
                    }

                    audioUri?.let { uri ->
                        val audioRef = storage.reference.child("audio/${uri.lastPathSegment}")
                        audioRef.putFile(uri).await()
                    }

                    onDismiss()
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ColorsScreen() {
    val colors = listOf(
        Triple("naasi", "brown", R.raw.brown),
        Triple("accho", "white", R.raw.white),
        Triple("haydro", "yellow", R.raw.yellow),
        Triple("karro", "black", R.raw.black),
        Triple("saoo", "green", R.raw.green),
        Triple("gray", "gray", R.raw.gray),
        Triple("wangraai", "purple", R.raw.purple),
        Triple("garho", "red", R.raw.red),
        Triple("neero", "blue", R.raw.blue),
        Triple("narangi", "orange", R.raw.orange)
    )
    val colorIcons = listOf(
        R.drawable.ic_brown,
        R.drawable.ic_white,
        R.drawable.ic_yellow,
        R.drawable.black,
        R.drawable.ic_green,
        R.drawable.ic_gray,
        R.drawable.ic_purple,
        R.drawable.ic_red,
        R.drawable.ic_blue,
        R.drawable.ic_orange
    )

    LazyColumn {
        items(colors.zip(colorIcons)) { (color, icon) ->
            WordItem(
                word = color.first,
                translation = color.second,
                audioResId = color.third,
                iconResId = icon
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NumbersScreen() {
    val words = listOf(
        Triple("hik", "One", R.raw.one),
        Triple("baa", "Two", R.raw.two),
        Triple("tey", "Three", R.raw.three),
        Triple("chaar", "Four", R.raw.four),
        Triple("panj", "Five", R.raw.five),
        Triple("chay", "Six", R.raw.six),
        Triple("sat", "Seven", R.raw.seven),
        Triple("ath", "Eight", R.raw.eight),
        Triple("nau", "Nine", R.raw.nine),
        Triple("dah", "Ten", R.raw.ten)
    )
    val icons = listOf(
        R.drawable.ic_one,
        R.drawable.ic_two,
        R.drawable.ic_three,
        R.drawable.ic_four,
        R.drawable.ic_five,
        R.drawable.ic_six,
        R.drawable.ic_sev,
        R.drawable.ic_eight,
        R.drawable.ic_nine,
        R.drawable.ten
    )

    LazyColumn {
        items(words.zip(icons)) { (word, icon) ->
            WordItem(
                word = word.first,
                translation = word.second,
                audioResId = word.third,
                iconResId = icon
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PhrasesScreen() {
    val phrases = listOf(
        Triple("tawah jo naalo cha ahye?", "What is your name?", R.raw.what_is_your_name),
        Triple("muhjo naalo aahye...", "My name is...?", R.raw.my_name_is),
        Triple("tawah kaady pya wanjo?", "Where are you going?", R.raw.where_are_you_going),
        Triple("aaun school pyo wanja.", "I'm going to the school", R.raw.im_going_to_the_school),
        Triple("he mahjo dost ahye", "He is my friend", R.raw.friend),
        Triple("tawah g tabiyat kiyein ahye?", "How are you feeling?", R.raw.how_are_you_feeling),
        Triple("maan behtreen ahyan", "I'm better", R.raw.im_better),
        Triple(
            "ha app Shafique Ahmed thaee aahye.",
            "This app is made by Shafique Ahmed.",
            R.raw.this_app_is_made_by_shafique_ahmed
        ),
        Triple("cha tawah acho tha?", "Are you coming?", R.raw.are_you_coming),
        Triple("haa maan achan tho", "Yes, I'm coming.", R.raw.yes_im_coming)
    )

    LazyColumn {
        items(phrases) { (phrase, translation, audioResId) ->
            PhraseItem(
                phrase = phrase,
                translation = translation,
                audioResId = audioResId
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PhraseItem(phrase: String, translation: String, audioResId: Int) {
    val context = LocalContext.current
    // Remember a single MediaPlayer instance
    val mediaPlayer = remember { MediaPlayer() }
    // Update the current audio resource ID
    val currentAudioResId by rememberUpdatedState(newValue = audioResId)

    // Release MediaPlayer when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color = Color(0xFF26A69A)) // Set background color to teal
            .clickable {
                // Reset and set data source for MediaPlayer
                mediaPlayer.reset()
                context.resources
                    .openRawResourceFd(currentAudioResId)
                    .apply {
                        mediaPlayer.setDataSource(fileDescriptor, startOffset, length)
                        close()
                    }
                mediaPlayer.prepare()
                mediaPlayer.start()
            }
            .padding(24.dp), // Inner padding for the content
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = phrase,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = translation,
                style = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color.White)
            )
        }
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
    }

    DisposableEffect(mediaPlayer) {
        onDispose {
            mediaPlayer.release()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FamilyScreen() {
    val familyWords = listOf(
        Triple("amma", "Mother", R.raw.ama),
        Triple("baba", "Father", R.raw.dad),
        Triple("putr", "Son", R.raw.son),
        Triple("dhee", "Daughter", R.raw.daughter),
        Triple("bha", "Brother", R.raw.bro),
        Triple("bherr", "Sister", R.raw.sis),
        Triple("chacho", "Uncle", R.raw.uncle),
        Triple("phuphi", "Aunt", R.raw.aunt)
    )
    val familyIcons = listOf(
        R.drawable.mom,
        R.drawable.dad,
        R.drawable.son,
        R.drawable.daughter,
        R.drawable.bro,
        R.drawable.sis,
        R.drawable.uncle,
        R.drawable.aunt
    )

    LazyColumn {
        items(familyWords.zip(familyIcons)) { (word, icon) ->
            WordItem(
                word = word.first,
                translation = word.second,
                audioResId = word.third,
                iconResId = icon
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WordItem(word: String, translation: String, audioResId: Int, iconResId: Int) {
    val context = LocalContext.current
    // Remember a single MediaPlayer instance
    val mediaPlayer = remember { MediaPlayer() }
    // Update the current audio resource ID
    val currentAudioResId by rememberUpdatedState(newValue = audioResId)

    // Release MediaPlayer when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color = Color(0xFFFFA726)) // Set background color to orange
            .clickable {
                // Reset and set data source for MediaPlayer
                mediaPlayer.reset()
                context.resources
                    .openRawResourceFd(currentAudioResId)
                    .apply {
                        mediaPlayer.setDataSource(fileDescriptor, startOffset, length)
                        close()
                    }
                mediaPlayer.prepare()
                mediaPlayer.start()
            }
            .padding(24.dp), // Inner padding for the content
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(androidx.compose.foundation.shape.CircleShape) // Set size for the icon
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = word,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = translation,
                style = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color.White)
            )
        }
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
    }

    DisposableEffect(mediaPlayer) {
        onDispose {
            mediaPlayer.release()
        }
    }
}