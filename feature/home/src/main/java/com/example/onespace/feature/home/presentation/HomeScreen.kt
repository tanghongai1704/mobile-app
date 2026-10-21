package com.example.onespace.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.session.Section

@Composable
internal fun HomeScreen(onSection: (Section) -> Unit) {
    FeaturePage("Your everyday, connected", "One space.\nMore possibilities.", "Watch, listen, read and play. Pick your moment.") {
        item { Artwork("A LITTLE OF EVERYTHING YOU LOVE", Modifier.fillMaxWidth().height(180.dp)) }
        item { SectionTitle("What are you in the mood for?") }
        item { ContentCard("Watch something new", "Video · Stories worth a pause", Icons.Filled.SmartDisplay, { onSection(Section.VIDEO) }) }
        item { ContentCard("Set the mood", "Music · Your daily mix", Icons.Filled.MusicNote, { onSection(Section.MUSIC) }) }
        item { ContentCard("Stay curious", "News · Fresh perspectives", Icons.Filled.Newspaper, { onSection(Section.NEWS) }) }
        item { ContentCard("Take a little break", "Games · Offline favourites", Icons.Filled.SportsEsports, { onSection(Section.GAMES) }) }
        item { PrototypeNote("You are exploring as a guest · All content in this build is sample data.") }
    }
}
