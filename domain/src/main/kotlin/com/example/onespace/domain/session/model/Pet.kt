package com.example.onespace.domain.session.model

enum class PetStage(val displayName: String, val requiredPoints: Int) {
    EGG("Trứng", 0),
    BABY("Bé cưng", 500),
    TEEN("Thiếu niên", 2000),
    ADULT("Trưởng thành", 5000);

    companion object {
        fun fromPoints(totalPoints: Int): PetStage =
            entries.lastOrNull { totalPoints >= it.requiredPoints } ?: EGG
    }
}

data class PetState(
    val stage: PetStage = PetStage.EGG,
    val totalPoints: Int = 0,
    val missionsCompleted: Int = 0,
    val hasHatched: Boolean = false
) {
    val nextStage: PetStage? get() = PetStage.entries.getOrNull(stage.ordinal + 1)
    val pointsToNextStage: Int? get() = nextStage?.let { it.requiredPoints - totalPoints }
    val canFeed: Boolean get() = hasHatched && stage != PetStage.EGG
}
