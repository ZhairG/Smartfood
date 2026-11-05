package com.smartfood.app.feature.recipes.ui.own

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.recipes.domain.model.OwnRecipe
import com.smartfood.app.feature.recipes.domain.usecase.GetOwnRecipeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel del detalle propio (spec 010, T014). */
class OwnRecipeDetailViewModel @Inject constructor(private val get: GetOwnRecipeUseCase, savedState: SavedStateHandle) : ViewModel() {
    private val _recipe = MutableStateFlow<OwnRecipe?>(null)
    val recipe: StateFlow<OwnRecipe?> = _recipe.asStateFlow()
    init { savedState.get<String>("recipeId")?.let { id -> viewModelScope.launch { get(id).onSuccess { _recipe.value = it } } } }
}
