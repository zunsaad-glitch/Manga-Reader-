sed -i '/val similarMangas = MutableStateFlow/a \
    val copilotMessages = MutableStateFlow<List<CopilotMessage>>(emptyList())\
    val copilotIsLoading = MutableStateFlow(false)\
' app/src/main/java/com/example/viewmodel/MainViewModel.kt
