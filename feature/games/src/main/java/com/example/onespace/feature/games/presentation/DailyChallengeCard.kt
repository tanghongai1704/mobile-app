package com.example.onespace.feature.games.presentation

import com.example.onespace.core.designsystem.t
import com.example.onespace.core.designsystem.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import com.example.onespace.core.designsystem.AppCard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.LocalEnglish
import com.example.onespace.core.designsystem.Translations
import com.example.onespace.core.designsystem.oneSpaceColors
import com.example.onespace.domain.games.challenge.ChallengeReward
import com.example.onespace.domain.games.challenge.DailyChallenge

/** Today's challenge: one game, one goal, a reward that grows with the day streak. */
@Composable
internal fun DailyChallengeCard(
    challenge: DailyChallenge,
    streakDays: Int,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val done = challenge.completed
    val container = if (done) MaterialTheme.oneSpaceColors.feedback.container
    else MaterialTheme.oneSpaceColors.card
    val content = if (done) MaterialTheme.oneSpaceColors.feedback.onContainer
    else MaterialTheme.colorScheme.onSurface
    AppCard(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (done) Icons.Filled.CheckCircle else Icons.Filled.EmojiEvents,
                    contentDescription = null, modifier = Modifier.size(22.dp)
                )
                Text(tr("Thử thách hôm nay", "Today's challenge"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Text(challenge.template.goal.t(), style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { challenge.fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                color = if (done) content else MaterialTheme.colorScheme.primary,
                trackColor = content.copy(alpha = 0.18f)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${challenge.progress.coerceAtMost(challenge.template.target)}/${challenge.template.target}",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(tr("Chuỗi $streakDays ngày", "$streakDays-day streak"), style = MaterialTheme.typography.labelMedium)
                }
            }
            if (done) {
                Text(
                    tr("Hoàn thành! Bạn nhận ${challenge.rewardPoints.takeIf { it > 0 } ?: ChallengeReward.points(streakDays)} điểm ", "Done! You earned ${challenge.rewardPoints.takeIf { it > 0 } ?: ChallengeReward.points(streakDays)} points ") +
                        tr("và một thức ăn thưởng cho thú cưng.", "and a treat for your pet."),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    tr("Thưởng ${ChallengeReward.points(streakDays)} điểm", "Reward: ${ChallengeReward.points(streakDays)} points") +
                        (if (streakDays > 0) tr(" (gồm +${ChallengeReward.streakBonus(streakDays)} nhờ chuỗi ngày)", " (includes +${ChallengeReward.streakBonus(streakDays)} for your streak)") else "") +
                        tr(" và một thức ăn thưởng cho thú cưng.", " and a treat for your pet."),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(2.dp))
                Button(onClick = onPlay) { Text(tr("Chơi ${challenge.template.gameTitle}", "Play ${challenge.template.gameTitle.t()}")) }
            }
        }
    }
}

/** Shown over a game the moment the daily challenge is completed. */
@Composable
internal fun ChallengeCompletedBanner(completion: com.example.onespace.domain.games.usecase.ChallengeCompletion, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.oneSpaceColors.feedback
    AppCard(
        modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.success, contentColor = colors.onSuccess)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(28.dp))
            Column {
                Text(tr("Hoàn thành thử thách hôm nay!", "Today's challenge complete!"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    tr("+${completion.points} điểm", "+${completion.points} points") +
                        (if (completion.streakBonus > 0) tr(" (gồm +${completion.streakBonus} chuỗi ngày)", " (includes +${completion.streakBonus} for your streak)") else "") +
                        tr(". Thú cưng nhận một thức ăn thưởng.", ". Your pet got a treat."),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Shown over a game when a badge is unlocked. */
@Composable
internal fun BadgeUnlockedBanner(badges: List<com.example.onespace.domain.games.badge.GameBadge>, modifier: Modifier = Modifier) {
    AppCard(
        modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(28.dp))
            Column {
                Text(
                    if (badges.size == 1) tr("Mở khóa huy hiệu mới!", "New badge unlocked!") else tr("Mở khóa ${badges.size} huy hiệu mới!", "${badges.size} new badges unlocked!"),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold
                )
                Text(
                    badges.map { it.name }.map { if (LocalEnglish.current) Translations.english(it) else it }.joinToString(", ") + tr(". Xem trong Tài khoản, mục Huy hiệu.", ". See them under Account, Badges."),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
