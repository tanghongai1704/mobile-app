package com.example.onespace.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Colours that Material 3 has no role for (media overlays, category art, rewards, game and
 * mascot artwork). They live next to the M3 schemes so feature code never declares a colour:
 * read them with `MaterialTheme.oneSpaceColors`.
 */
@Immutable
class GradientPair(val start: Color, val end: Color) {
    val colors: List<Color> get() = listOf(start, end)
    fun brush(): Brush = Brush.linearGradient(colors)
}

/** One gradient per content topic; [all] is the stable pool used to colour items by id. */
@Immutable
class TopicGradients(
    val nature: GradientPair,
    val life: GradientPair,
    val calm: GradientPair,
    val travel: GradientPair,
    val food: GradientPair,
    val sport: GradientPair,
    val tech: GradientPair,
    val neutral: GradientPair
) {
    val all: List<GradientPair> = listOf(tech, calm, nature, travel, sport, food, neutral, life)
}

/** Accent per entertainment area (wrapped report, shop tiles). */
@Immutable
class DomainAccents(
    val video: Color,
    val music: Color,
    val news: Color,
    val games: Color,
    val pet: Color
)

/** Avatar frame tiers unlocked by points. */
@Immutable
class FrameColors(val basic: Color, val bronze: Color, val silver: Color, val gold: Color)

/** Positive feedback (correct answer, success); M3 only ships an error role. */
@Immutable
class SuccessColors(val success: Color, val onSuccess: Color, val container: Color, val onContainer: Color)

/** Mascot artwork: egg, chick and parrot. Identical in light and dark. */
@Immutable
class PetPaletteColors(
    val eggShell: Color, val eggSpot: Color,
    val chickBody: Color, val chickBelly: Color, val chickFoot: Color, val chickBeak: Color,
    val cheek: Color, val eyeWhite: Color, val eyePupil: Color,
    val parrotBody: Color, val parrotWing: Color, val parrotHead: Color,
    val parrotTailBlue: Color, val parrotTailYellow: Color, val parrotFoot: Color,
    val parrotFace: Color, val parrotBeak: Color, val parrotCrest: Color,
    val groundShadow: Color
)

@Immutable
class OneSpaceExtendedColors(
    val topics: TopicGradients,
    val accents: DomainAccents,
    val frames: FrameColors,
    val feedback: SuccessColors,
    /** Resting colour of content cards: white on light, a step above the page on dark. */
    val card: Color,
    /** Falling-note lane colours for the rhythm game, one per lane. */
    val lanes: List<Color>,
    /** Content drawn on top of artwork or video (icons, duration text, play button). */
    val onMedia: Color,
    /** Scrim behind badges that sit on artwork or video. */
    val mediaScrim: Color,
    /** Letterbox behind a video surface before the first frame; dark in both themes. */
    val mediaBackdrop: Color,
    val pet: PetPaletteColors
)

internal val PetPalette = PetPaletteColors(
    eggShell = Color(0xFFFFF6E3), eggSpot = Color(0xFFF3E3B8),
    chickBody = Color(0xFFFFD75E), chickBelly = Color(0xFFFFF0C2),
    chickFoot = Color(0xFFF09A2D), chickBeak = Color(0xFFF57C00),
    cheek = Color(0xD9F8BBD0), eyeWhite = Color(0xFFFFFFFF), eyePupil = Color(0xFF33261A),
    parrotBody = Color(0xFF66BB6A), parrotWing = Color(0xFF3E8E41), parrotHead = Color(0xFFEF5350),
    parrotTailBlue = Color(0xFF42A5F5), parrotTailYellow = Color(0xFFFFCA28),
    parrotFoot = Color(0xFF8D6E63), parrotFace = Color(0xFFFFF3E0),
    parrotBeak = Color(0xFFB0BEC5), parrotCrest = Color(0xFFFFCA28),
    groundShadow = Color(0x1A000000)
)

internal val LightExtendedColors = OneSpaceExtendedColors(
    topics = TopicGradients(
        nature = GradientPair(Color(0xFF1E6121), Color(0xFF61A55C)),
        life = GradientPair(Color(0xFF4F555F), Color(0xFF9096A1)),
        calm = GradientPair(Color(0xFF644496), Color(0xFFA885DD)),
        travel = GradientPair(Color(0xFF8A3F01), Color(0xFFDB7E40)),
        food = GradientPair(Color(0xFF754D00), Color(0xFFC8891D)),
        sport = GradientPair(Color(0xFF006056), Color(0xFF00A898)),
        tech = GradientPair(Color(0xFF2F519E), Color(0xFF7494E5)),
        neutral = GradientPair(Color(0xFF4C565D), Color(0xFF8D979F))
    ),
    accents = DomainAccents(
        video = Color(0xFFA04B00),
        music = Color(0xFF7848C0),
        news = Color(0xFF0067A5),
        games = Color(0xFF007419),
        pet = Color(0xFFA8387B)
    ),
    frames = FrameColors(
        basic = Color(0xFF9E9392),
        bronze = Color(0xFFB46115),
        silver = Color(0xFF8F96A0),
        gold = Color(0xFFE0A700)
    ),
    feedback = SuccessColors(
        success = Color(0xFF006E17),
        onSuccess = Color(0xFFFFFFFF),
        container = Color(0xFFB8F1AD),
        onContainer = Color(0xFF002D05)
    ),
    card = LightColors.surfaceContainerLowest,
    lanes = listOf(Color(0xFFB9483E), Color(0xFF8C6700), Color(0xFF007A74), Color(0xFF7958C7)),
    onMedia = Color(0xFFFFFFFF),
    mediaScrim = Color(0x8C000000),
    mediaBackdrop = Color(0xFF0B0A0A),
    pet = PetPalette
)

internal val DarkExtendedColors = OneSpaceExtendedColors(
    topics = TopicGradients(
        nature = GradientPair(Color(0xFF0B5314), Color(0xFF488B45)),
        life = GradientPair(Color(0xFF414750), Color(0xFF767C87)),
        calm = GradientPair(Color(0xFF563587), Color(0xFF8D6CC1)),
        travel = GradientPair(Color(0xFF753400), Color(0xFFBB6529)),
        food = GradientPair(Color(0xFF624000), Color(0xFFA97100)),
        sport = GradientPair(Color(0xFF005048), Color(0xFF008C7E)),
        tech = GradientPair(Color(0xFF1D438F), Color(0xFF5979C9)),
        neutral = GradientPair(Color(0xFF3E484F), Color(0xFF747D85))
    ),
    accents = DomainAccents(
        video = Color(0xFFFFA76F),
        music = Color(0xFFCDACFF),
        news = Color(0xFF81C1FF),
        games = Color(0xFF6ED16A),
        pet = Color(0xFFFF9DCF)
    ),
    frames = FrameColors(
        basic = Color(0xFFB4A9A7),
        bronze = Color(0xFFF49446),
        silver = Color(0xFFBBC1CC),
        gold = Color(0xFFFCBC00)
    ),
    feedback = SuccessColors(
        success = Color(0xFF8ED885),
        onSuccess = Color(0xFF002D05),
        container = Color(0xFF00460B),
        onContainer = Color(0xFFB8F1AD)
    ),
    card = DarkColors.surfaceContainer,
    lanes = listOf(Color(0xFFFF897D), Color(0xFFDAA200), Color(0xFF00BFB5), Color(0xFFB89BFF)),
    onMedia = Color(0xFFFFFFFF),
    mediaScrim = Color(0x8C000000),
    mediaBackdrop = Color(0xFF0B0A0A),
    pet = PetPalette
)

internal val LocalOneSpaceColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.oneSpaceColors: OneSpaceExtendedColors
    @Composable get() = LocalOneSpaceColors.current
