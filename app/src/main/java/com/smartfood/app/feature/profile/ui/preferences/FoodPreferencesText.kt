package com.smartfood.app.feature.profile.ui.preferences

import com.smartfood.app.R
import com.smartfood.app.feature.profile.domain.model.Cuisine
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.profile.domain.model.MealBudget
import com.smartfood.app.feature.profile.domain.model.SpiceLevel

/**
 * Nombre: FoodPreferencesText.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Único mapeo de los enums de preferencias a `strings_profile.xml` (spec 008, RNF-PER-02; Figma
 * `376:1260`). Los textos de los chips son los del frame.
 */

/** Texto de cada restricción alimentaria (CA-PER-03-01). */
val DietaryRestriction.titleRes: Int
    get() = when (this) {
        DietaryRestriction.VEGETARIAN -> R.string.preference_vegetarian
        DietaryRestriction.VEGAN -> R.string.preference_vegan
        DietaryRestriction.GLUTEN_FREE -> R.string.preference_gluten_free
        DietaryRestriction.LACTOSE_FREE -> R.string.preference_lactose_free
        DietaryRestriction.NUT_FREE -> R.string.preference_nut_free
    }

/** Texto de cada cocina favorita (CA-PER-03-01). */
val Cuisine.titleRes: Int
    get() = when (this) {
        Cuisine.COLOMBIAN -> R.string.preference_colombian
        Cuisine.ITALIAN -> R.string.preference_italian
        Cuisine.ASIAN -> R.string.preference_asian
        Cuisine.MEXICAN -> R.string.preference_mexican
        Cuisine.MEDITERRANEAN -> R.string.preference_mediterranean
    }

/** Texto de cada nivel de picante (CA-PER-03-01). */
val SpiceLevel.titleRes: Int
    get() = when (this) {
        SpiceLevel.MILD -> R.string.preference_spice_mild
        SpiceLevel.MEDIUM -> R.string.preference_spice_medium
        SpiceLevel.HOT -> R.string.preference_spice_hot
    }

/** Texto de cada presupuesto por comida (CA-PER-03-01). */
val MealBudget.titleRes: Int
    get() = when (this) {
        MealBudget.ECONOMIC -> R.string.preference_budget_economic
        MealBudget.MODERATE -> R.string.preference_budget_moderate
        MealBudget.UNLIMITED -> R.string.preference_budget_unlimited
    }
