package com.openminis.app.ui.settings

import android.content.Context
import com.openminis.app.data.model.LLMError
import com.openminis.app.data.model.LLMMessage
import com.openminis.app.data.model.ModelEntry
import com.openminis.app.data.model.ProviderCredential
import com.openminis.app.data.repository.ProviderRepository
import com.openminis.app.provider.ProviderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * ALTIV Smart Router 1.0.
 *
 * A user-initiated, sequential, bounded health probe for a Model Group.
 * It does not pretend that a model is free: users must explicitly add
 * models whose charges/quotas they have checked. Each probe consumes API
 * usage and must never run in the background or on app startup.
 *
 * Actual message failure recovery stays with the existing Model Group
 * fallback strategy. No API keys or server response bodies are logged.
 */
internal object SmartRouterProbe {
    const val MAX_PROBES = 12
    const val PROBE_TIMEOUT_MS = 15_000L

    enum class State { AVAILABLE, UNAVAILABLE, SKIPPED }

    data class Result(
        val entryId: String,
        val label: String,
        val state: State,
        val note: String,
    )

    suspend fun check(
        context: Context,
        repository: ProviderRepository,
        entry: ModelEntry,
    ): Result {
        val instance = repository.instance(entry.providerInstanceId)
            ?: return Result(entry.id, entry.model.displayName, State.SKIPPED, "Provedor não encontrado")
        val name = "${instance.label} · ${entry.model.displayName}"
        if (!instance.isEnabled || entry.isHidden || entry.isUnavailableFromProvider) {
            return Result(entry.id, name, State.SKIPPED, "Modelo desabilitado ou indisponível")
        }
        // Restrict the first version to user-supplied API keys. OAuth needs
        // token refresh/consent flows and must not be exercised by a probe.
        if (instance.credentialType != ProviderCredential.apiKey) {
            return Result(entry.id, name, State.SKIPPED, "Teste OAuth não suportado")
        }
        if (instance.azureMode) {
            return Result(entry.id, name, State.SKIPPED, "Teste Azure não suportado")
        }
        val key = repository.usableApiKey(instance)
            ?: return Result(entry.id, name, State.SKIPPED, "Configure a chave API")
        return try {
            val response = withTimeout(PROBE_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    val provider = ProviderFactory.create(
                        instance = instance,
                        apiKey = key,
                        model = entry.model,
                        context = context,
                        sessionId = ProviderFactory.oneOffSessionId(),
                        overrides = entry.overrides,
                    )
                    provider.sendMessage(
                        messages = listOf(LLMMessage(LLMMessage.Role.USER, "Responda somente OK.")),
                        systemPrompt = null,
                        maxTokens = 32,
                    )
                }
            }
            if (response.text.isNotBlank() || response.mediaAttachments.isNotEmpty()) {
                Result(entry.id, name, State.AVAILABLE, "Respondeu ao teste")
            } else {
                Result(entry.id, name, State.UNAVAILABLE, "Resposta vazia")
            }
        } catch (_: TimeoutCancellationException) {
            Result(entry.id, name, State.UNAVAILABLE, "Tempo limite do teste")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val note = when (failure) {
                is LLMError.RateLimited -> "Cota ou limite atingido (429)"
                is LLMError.InvalidApiKey -> "Chave inválida ou sem acesso"
                is LLMError.ProviderError -> when (failure.httpStatus) {
                    401, 403 -> "Autorização recusada"
                    402 -> "Saldo ou pagamento necessário"
                    404 -> "Modelo ou rota não encontrado"
                    429 -> "Cota ou limite atingido (429)"
                    in 500..599 -> "Servidor do provedor indisponível"
                    else -> "O provedor rejeitou o teste"
                }
                else -> "Falha de conexão ou resposta"
            }
            Result(entry.id, name, State.UNAVAILABLE, note)
        }
    }
}

/**
 * Stable ordering: available probes first, unknown/skipped/failing members
 * after them in their original order. Never removes a model from the group.
 */
internal object SmartRouterPlanner {
    fun prioritize(
        currentMemberIds: List<String>,
        results: List<SmartRouterProbe.Result>,
    ): List<String> {
        val good = results.filter { it.state == SmartRouterProbe.State.AVAILABLE }
            .map { it.entryId }.toSet()
        return currentMemberIds.filter { it in good } +
            currentMemberIds.filterNot { it in good }
    }
}
