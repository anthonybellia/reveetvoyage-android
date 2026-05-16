package be.reveetvoyage.app.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.model.*
import be.reveetvoyage.app.data.repo.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repo: ExpenseRepository,
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<VoyageExpense>>(emptyList())
    val expenses: StateFlow<List<VoyageExpense>> = _expenses.asStateFlow()

    private val _participants = MutableStateFlow<List<VoyageParticipant>>(emptyList())
    val participants: StateFlow<List<VoyageParticipant>> = _participants.asStateFlow()

    private val _settlement = MutableStateFlow<SettlementResponse?>(null)
    val settlement: StateFlow<SettlementResponse?> = _settlement.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var loadedVoyageId: Int? = null

    fun load(voyageId: Int) {
        loadedVoyageId = voyageId
        viewModelScope.launch {
            _isLoading.value = true
            refreshInternal(voyageId)
            _isLoading.value = false
        }
    }

    fun refresh(voyageId: Int) {
        loadedVoyageId = voyageId
        viewModelScope.launch { refreshInternal(voyageId) }
    }

    private suspend fun refreshInternal(voyageId: Int) {
        runCatching { repo.participants(voyageId) }
            .onSuccess { _participants.value = it }
        runCatching { repo.expenses(voyageId) }
            .onSuccess { _expenses.value = it.sortedByDescending { e -> e.spent_at ?: "" } }
        runCatching { repo.settlement(voyageId) }
            .onSuccess { _settlement.value = it }
    }

    fun createExpense(
        voyageId: Int,
        title: String,
        amount: Double,
        currency: String = "EUR",
        paidByParticipantId: Int,
        spentAt: String? = null,
        description: String? = null,
        category: String? = null,
        splits: List<VoyageExpenseSplit>? = null,
        locationName: String? = null,
        locationLatitude: Double? = null,
        locationLongitude: Double? = null,
        onDone: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val req = CreateExpenseRequest(
                title = title,
                amount = amount,
                currency = currency,
                paid_by_participant_id = paidByParticipantId,
                spent_at = spentAt,
                description = description,
                category = category,
                splits = splits,
                location_name = locationName,
                location_latitude = locationLatitude,
                location_longitude = locationLongitude,
            )
            val ok = runCatching { repo.createExpense(voyageId, req) }.isSuccess
            if (ok) refreshInternal(voyageId)
            onDone(ok)
        }
    }

    fun updateExpense(
        voyageId: Int,
        expenseId: Int,
        title: String,
        amount: Double,
        currency: String = "EUR",
        paidByParticipantId: Int,
        spentAt: String? = null,
        description: String? = null,
        category: String? = null,
        splits: List<VoyageExpenseSplit>? = null,
        locationName: String? = null,
        locationLatitude: Double? = null,
        locationLongitude: Double? = null,
        onDone: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val req = CreateExpenseRequest(
                title = title,
                amount = amount,
                currency = currency,
                paid_by_participant_id = paidByParticipantId,
                spent_at = spentAt,
                description = description,
                category = category,
                splits = splits,
                location_name = locationName,
                location_latitude = locationLatitude,
                location_longitude = locationLongitude,
            )
            val ok = runCatching { repo.updateExpense(voyageId, expenseId, req) }.isSuccess
            if (ok) refreshInternal(voyageId)
            onDone(ok)
        }
    }

    suspend fun searchPlaces(q: String): List<Place> =
        runCatching { repo.searchPlaces(q) }.getOrDefault(emptyList())

    fun deleteExpense(voyageId: Int, expenseId: Int, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { repo.deleteExpense(voyageId, expenseId) }.isSuccess
            if (ok) refreshInternal(voyageId)
            onDone(ok)
        }
    }

    fun addGuest(voyageId: Int, displayName: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { repo.addGuest(voyageId, displayName) }.isSuccess
            if (ok) refreshInternal(voyageId)
            onDone(ok)
        }
    }

    fun inviteByEmail(voyageId: Int, email: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { repo.inviteByEmail(voyageId, email) }.isSuccess
            if (ok) refreshInternal(voyageId)
            onDone(ok)
        }
    }

    fun removeParticipant(voyageId: Int, participantId: Int, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { repo.removeParticipant(voyageId, participantId) }.isSuccess
            if (ok) refreshInternal(voyageId)
            onDone(ok)
        }
    }
}
