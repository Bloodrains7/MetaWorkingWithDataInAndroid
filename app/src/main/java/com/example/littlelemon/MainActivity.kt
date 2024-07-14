package com.example.littlelemon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.littlelemon.ui.theme.LittleLemonTheme
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true // This helps when the JSON contains fields that are not in your data class
        })
    }
}


class MainActivity : ComponentActivity() {
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(contentType = ContentType("text", "plain"))
        }
    }

    private val database by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database").build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LittleLemonTheme {
                // add databaseMenuItems code here
                val menuItemDao = database.menuItemDao()
                val databaseMenuItems: LiveData<List<MenuItemRoom>> = menuItemDao.getAll()
                val databaseMenuItemsState by databaseMenuItems.observeAsState(initial = emptyList())

                // add orderMenuItems variable here
                var orderMenuItems by remember { mutableStateOf(false) }

                // add menuItems variable here
                var menuItems by remember { mutableStateOf(databaseMenuItemsState) }

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "logo",
                        modifier = Modifier.padding(50.dp)
                    )

                    // add Button code here
                    Button(onClick = { orderMenuItems = true }) {
                        BasicText("Tap to Order by Name")
                    }

                    // add searchPhrase variable here
                    var searchPhrase by remember { mutableStateOf("") }

                    LaunchedEffect(databaseMenuItemsState, orderMenuItems, searchPhrase) {
                        menuItems = if (orderMenuItems) {
                            databaseMenuItemsState.sortedBy { it.title }
                        } else {
                            databaseMenuItemsState
                        }

                        if (searchPhrase.isNotEmpty()) {
                            menuItems = menuItems.filter { item ->
                                item.title.contains(searchPhrase, ignoreCase = true)
                            }
                        }
                    }

                    val filteredMenuItems = remember(menuItems, searchPhrase) {
                        menuItems.filter { item ->
                            item.title.contains(searchPhrase, ignoreCase = true)
                        }
                    }

                    // Add OutlinedTextField
                    OutlinedTextField(
                        value = searchPhrase,
                        onValueChange = { newValue ->
                            searchPhrase = newValue
                        },
                        label = { BasicText("Search") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 50.dp, end = 50.dp)
                    )

                    // add is not empty check here
                    if (filteredMenuItems.isNotEmpty()) {
                        MenuItemsList(filteredMenuItems)
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            if (database.menuItemDao().isEmpty()) {
                val menuItems = fetchMenu()
                saveMenuToDatabase(menuItems)
            }
        }
    }

    private suspend fun fetchMenu(): List<MenuItemNetwork> {
        val response: HttpResponse = httpClient.get("https://raw.githubusercontent.com/Meta-Mobile-Developer-PC/Working-With-Data-API/main/littleLemonSimpleMenu.json")
        val menuNetwork: MenuNetwork = response.body()
        return menuNetwork.menu
    }

    private fun saveMenuToDatabase(menuItemsNetwork: List<MenuItemNetwork>) {
        val menuItemsRoom = menuItemsNetwork.map { it.toMenuItemRoom() }
        database.menuItemDao().insertAll(*menuItemsRoom.toTypedArray())
    }
}

@Composable
private fun MenuItemsList(items: List<MenuItemRoom>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = 20.dp)
    ) {
        items(
            items = items,
            itemContent = { menuItem ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(menuItem.title)
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                        textAlign = TextAlign.Right,
                        text = "%.2f".format(menuItem.price)
                    )
                }
            }
        )
    }
}
