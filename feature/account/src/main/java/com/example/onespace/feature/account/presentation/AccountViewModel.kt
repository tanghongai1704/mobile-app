package com.example.onespace.feature.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import com.example.onespace.domain.session.usecase.FeedPet
import com.example.onespace.domain.session.usecase.FeedResult
import com.example.onespace.domain.session.usecase.ObservePet
import com.example.onespace.domain.session.usecase.ObserveShop
import com.example.onespace.domain.session.usecase.ObserveStreak
import com.example.onespace.domain.session.usecase.ObserveTotalPoints
import com.example.onespace.domain.session.usecase.PurchaseItem
import com.example.onespace.domain.session.usecase.PurchaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Aggregates the gamification state the profile page renders (F-COM-03). */
@HiltViewModel
internal class AccountViewModel @Inject constructor(
    observeTotalPoints: ObserveTotalPoints,
    observeStreak: ObserveStreak,
    observePet: ObservePet,
    observeShop: ObserveShop,
    private val feedPet: FeedPet,
    private val purchaseItem: PurchaseItem,
    preferences: SessionPreferencesRepository
) : ViewModel() {
    val authEmail: StateFlow<String?> = preferences.preferences.map { it.authEmail }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val gamification: StateFlow<AccountGamificationState> = combine(
        observeTotalPoints(), observeStreak(), observePet(), observeShop()
    ) { points, streak, pet, shop ->
        AccountGamificationState(points, streak, pet, shop)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountGamificationState())

    /** One-shot outcome message for feed/purchase actions. */
    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    fun consumeFeedback() { _feedback.value = null }

    fun feed() {
        viewModelScope.launch {
            _feedback.value = when (feedPet()) {
                FeedResult.SUCCESS -> "Đã cho thú ăn (−30 điểm)"
                FeedResult.NOT_ENOUGH_POINTS -> "Không đủ 30 điểm để cho ăn"
                FeedResult.LIMIT_REACHED -> "Hôm nay đã cho ăn rồi"
            }
        }
    }

    fun purchase(item: com.example.onespace.domain.session.model.ShopItem) {
        viewModelScope.launch {
            _feedback.value = when (purchaseItem(item)) {
                PurchaseResult.SUCCESS -> "Đã đổi " + item.name
                PurchaseResult.NOT_ENOUGH_POINTS -> "Không đủ điểm để đổi " + item.name
                PurchaseResult.ALREADY_OWNED -> "Bạn đã có " + item.name
            }
        }
    }
}
