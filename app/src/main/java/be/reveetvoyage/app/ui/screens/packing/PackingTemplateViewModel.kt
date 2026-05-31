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
class PackingTemplateViewModel @Inject constructor(
    private val repo: PackingRepository,
) : ViewModel() {

    private val _categories = MutableStateFlow<List<PackingCategory>>(emptyList())
    val categories: StateFlow<List<PackingCategory>> = _categories.asStateFlow()

    private val _items = MutableStateFlow<List<PackingTemplateItem>>(emptyList())
    val items: StateFlow<List<PackingTemplateItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            refreshInternal()
            _isLoading.value = false
        }
    }

    private suspend fun refreshInternal() {
        runCatching { repo.categories() }.onSuccess { _categories.value = it }
        runCatching { repo.template() }.onSuccess { _items.value = it.sortedBy { i -> i.sort_order } }
    }

    /** Items grouped by category_id */
    fun grouped(categories: List<PackingCategory>): List<Pair<PackingCategory?, List<PackingTemplateItem>>> {
        val items = _items.value
        val byCategory = items.groupBy { it.category_id }
        val result = mutableListOf<Pair<PackingCategory?, List<PackingTemplateItem>>>()

        categories.forEach { cat ->
            val catItems = byCategory[cat.id]
            if (!catItems.isNullOrEmpty()) {
                result.add(Pair(cat, catItems.sortedBy { it.sort_order }))
            }
        }
        val uncategorised = byCategory[null]
        if (!uncategorised.isNullOrEmpty()) {
            result.add(Pair(null, uncategorised.sortedBy { it.sort_order }))
        }
        return result
    }

    fun addItem(label: String, categoryId: Int?, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val req = PackingTemplateItemRequest(category_id = categoryId, label = label)
            val ok = runCatching { repo.addTemplate(req) }.isSuccess
            if (ok) refreshInternal()
            onDone(ok)
        }
    }

    fun updateItem(id: Int, label: String, categoryId: Int?, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val req = PackingTemplateItemRequest(category_id = categoryId, label = label)
            val ok = runCatching { repo.updateTemplate(id, req) }.isSuccess
            if (ok) refreshInternal()
            onDone(ok)
        }
    }

    fun deleteItem(id: Int, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { repo.deleteTemplate(id) }.isSuccess
            if (ok) refreshInternal()
            onDone(ok)
        }
    }
}
