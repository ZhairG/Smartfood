package com.smartfood.app.feature.recipes.data.repository

import com.smartfood.app.core.database.CookingDao
import com.smartfood.app.core.database.CookingEntity
import com.smartfood.app.core.database.CurrentUid
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.repository.CookingHistoryRepository
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Marca la implementación de [CookingHistoryRepository] que habla con Firestore (spec 014, plan §3). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteHistory

/**
 * Nombre: CachedCookingHistoryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Historial de cocina con una copia en Room (spec 014, HU-LOC-01). Firestore sigue siendo la fuente de
 * verdad: cada snapshot reemplaza la copia del teléfono (CA-LOC-01-02), y lo que se emite sale de Room, así que
 * «Ahorrados este mes» se ve sin red y al abrir la app en frío (CA-LOC-01-01). Si Firestore falla, se queda con la
 * copia (CA-LOC-01-04). La escritura no cambia: va a Firestore con su `WriteOutcome` y, si no falla, también a Room
 * (CA-LOC-01-03).
 *
 * @param remote Historial en Firestore (`users/{uid}/cookings`).
 * @param dao Copia local.
 * @param currentUid Usuario con sesión; cada consulta filtra por él (RNF-LOC-02).
 */
class CachedCookingHistoryRepository @Inject constructor(
    @param:RemoteHistory private val remote: CookingHistoryRepository,
    private val dao: CookingDao,
    private val currentUid: CurrentUid,
) : CookingHistoryRepository {

    override fun observeHistory(): Flow<List<CookingRecord>> {
        val uid = currentUid.get() ?: return flowOf(emptyList())
        return channelFlow {
            launch {
                remote.observeHistory()
                    .catch { /* Sin red o sin permiso: se queda con la copia del teléfono (CA-LOC-01-04). */ }
                    .collect { records -> dao.replaceAll(uid, records.map { it.toEntity(uid) }) }
            }
            dao.observe(uid).map { rows -> rows.map { it.toRecord() } }.collect { send(it) }
        }
    }

    override fun newRecordId(): String = remote.newRecordId()

    override suspend fun saveRecord(record: CookingRecord): Result<WriteOutcome> {
        val outcome = remote.saveRecord(record)
        val uid = currentUid.get()
        if (outcome.isSuccess && uid != null) dao.upsert(record.toEntity(uid))
        return outcome
    }

    private fun CookingRecord.toEntity(uid: String) =
        CookingEntity(id, uid, recipeId, servings, cookedAt, rescuedValueCop)

    private fun CookingEntity.toRecord() =
        CookingRecord(id = id, recipeId = recipeId, servings = servings, cookedAt = cookedAt, rescuedValueCop = rescuedValueCop)
}
