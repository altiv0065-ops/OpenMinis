package com.openminis.app.data.model

/**
 * OpenAI-compatible API shortcuts. Saved instances keep using ProviderType.openAI
 * and the existing encrypted key store, model discovery and request handling.
 * Free tiers and available models can change independently of these shortcuts.
 */
data class ApiProviderTemplate(
    val name: String,
    val description: String,
    val baseURL: String,
    val appendV1: Boolean = false,
) {
    companion object {
        val all: List<ApiProviderTemplate> = listOf(
            ApiProviderTemplate("NVIDIA NIM", "Modelos NVIDIA e parceiros via API", "https://integrate.api.nvidia.com/v1"),
            ApiProviderTemplate("GroqCloud", "Inferência rápida; plano gratuito limitado", "https://api.groq.com/openai/v1"),
            ApiProviderTemplate("Cerebras", "Inferência rápida; créditos de teste", "https://api.cerebras.ai/v1"),
            ApiProviderTemplate("Mistral AI", "Modelos Mistral; modo gratuito limitado", "https://api.mistral.ai/v1"),
            ApiProviderTemplate("Hugging Face", "Diversos modelos; pequenos créditos mensais", "https://router.huggingface.co/v1"),
            ApiProviderTemplate("OpenCode Zen", "Modelos de programação e opções gratuitas", "https://opencode.ai/zen/v1"),
            ApiProviderTemplate("DeepSeek", "Modelos DeepSeek; verifique saldo e preços", "https://api.deepseek.com"),
            ApiProviderTemplate("Together AI", "Modelos abertos e comerciais; verifique créditos", "https://api.together.ai/v1"),
            ApiProviderTemplate("Fireworks AI", "Inferência de modelos abertos; verifique créditos", "https://api.fireworks.ai/inference/v1"),
            ApiProviderTemplate("SiliconFlow", "Modelos de IA; disponibilidade por região", "https://api.siliconflow.com/v1"),
            ApiProviderTemplate("DeepInfra", "Modelos hospedados; verifique créditos", "https://api.deepinfra.com/v1/openai"),
        )
    }
}
