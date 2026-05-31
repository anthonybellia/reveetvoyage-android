package be.reveetvoyage.app.ui.screens.packing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.model.*
import be.reveetvoyage.app.data.repo.PackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PackingViewModel @Inject constructor(
    private val repo: PackingRepository,
) : ViewModel() {

    private val _categories = MutableStateFlow<List<PackingCategory>>(emptyList())
    val categories: StateFlow<List<PackingCategory>> = _categories.asStateFlow()

    private val _items = MutableStateFlow<List<VoyagePackingItem>>(emptyList())
    val items: StateFlow<List<VoyagePackingItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** checked / total — recomputed from _items */
    val progress: Pair<Int, Int>
        get() {
            val all = _items.value
            return Pair(all.count { it.is_checked }, all.size)
        }

    /** Items grouped by category_id (null = uncategorised) */
    fun grouped(categories: List<PackingCategory>): List<Pair<PackingCategory?, List<VoyagePackingItem>>> {
        val items = _items.value
        val byCategory = items.groupBy { it.category_id }
        val result = mutableListOf<Pair<PackingCategory?, List<VoyagePackingItem>>>()

        // First: categories that have items, in API order
        categories.forEach { cat ->
            val catItems = byCategory[cat.id]
            if (!catItems.isNullOrEmpty()) {
                result.add(Pair(cat, catItems.sortedBy { it.sort_order }))
            }
        }
        // Then: uncategorised items
        val uncategorised = byCategory[null]
        if (!uncategorised.isNullOrEmpty()) {
            result.add(Pair(null, uncategorised.sortedBy { it.sort_order }))
        }
        return result
    }

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
        runCatching { repo.categories() }.onSuccess { _categories.value = it }
        runCatching { repo.voyagePacking(voyageId) }.onSuccess { _items.value = it.sortedBy { i -> i.sort_order } }
    }

    /** Optimistic toggle: flip locally, then sync. Revert on failure. */
    fun toggle(voyageId: Int, item: VoyagePackingItem) {
        val newChecked = !item.is_checked
        // optimistic update
        _items.value = _items.value.map { if (it.id == item.id) it.copy(is_checked = newChecked) else it }
        viewModelScope.launch {
            val ok = runCatching {
                repo.updateVoyageItem(voyageId, item.id, VoyagePackingUpdateRequest(is_checked = newChecked))
            }.isSuccess
            if (!ok) {
                // revert
                _items.value = _items.value.map { if (it.id == item.id) it.copy(is_checked = !newChecked) else it }
            }
        }
    }

    fun addItem(
        voyageId: Int,
        label: String,
        categoryId: Int?,
        keepForNext: Boolean,
        onDone: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val req = VoyagePackingCreateRequest(
                category_id = categoryId,
                label = label,
                keep_for_next = keepForNext,
            )
            val ok = runCatching { repo.addVoyageItem(voyageId, req) }.isSuccess
            if (ok) refreshInternal(voyageId)
            onDone(ok)
        }
    }

    fun deleteItem(voyageId: Int, itemId: Int, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { repo.deleteVoyageItem(voyageId, itemId) }.isSuccess
            if (ok) refreshInternal(voyageId)
            onDone(ok)
        }
    }
}
