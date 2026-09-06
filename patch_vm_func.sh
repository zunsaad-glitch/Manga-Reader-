sed -i '$ d' app/src/main/java/com/example/viewmodel/MainViewModel.kt

cat << 'INNEREOF' >> app/src/main/java/com/example/viewmodel/MainViewModel.kt

    fun clearCopilot() {
        copilotMessages.value = emptyList()
    }

    fun sendCopilotMessage(manga: MangaData?, userMessage: String) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            copilotMessages.value = copilotMessages.value + CopilotMessage("user", userMessage) + CopilotMessage("model", "Please set your Gemini API key in the Secrets panel to use the AI Oracle.")
            return
        }

        copilotMessages.value = copilotMessages.value + CopilotMessage("user", userMessage)
        copilotIsLoading.value = true

        viewModelScope.launch {
            try {
                val sysPrompt = "You are an expert AI Manga Oracle. The user is asking about the manga titled '${manga?.attributes?.title?.values?.firstOrNull() ?: "Unknown"}'. Description: '${manga?.attributes?.description?.values?.firstOrNull() ?: ""}'. Be highly engaging, formatting with emojis and bold text. Provide amazing insights."
                
                val request = GenerateContentRequest(
                    contents = copilotMessages.value.map { msg ->
                        Content(parts = listOf(Part(text = msg.text)))
                    },
                    systemInstruction = Content(parts = listOf(Part(text = sysPrompt)))
                )
                
                val response = GeminiClient.api.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "The Oracle is currently clouded..."
                
                copilotMessages.value = copilotMessages.value + CopilotMessage("model", responseText)
            } catch (e: Exception) {
                copilotMessages.value = copilotMessages.value + CopilotMessage("model", "An error occurred communing with the Oracle: ${e.message}")
            } finally {
                copilotIsLoading.value = false
            }
        }
    }
}
INNEREOF
