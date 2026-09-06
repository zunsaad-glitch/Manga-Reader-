sed -i '/val isLoading by viewModel.isLoading.collectAsState()/a \
    val copilotMessages by viewModel.copilotMessages.collectAsState()\
    val copilotIsLoading by viewModel.copilotIsLoading.collectAsState()\
    var showCopilot by remember { mutableStateOf(false) }\
' app/src/main/java/com/example/ui/screens/ChapterListScreen.kt
