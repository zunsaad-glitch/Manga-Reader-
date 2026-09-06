sed -i '/containerColor = ThemeBackground,/a \
        floatingActionButton = {\
            ExtendedFloatingActionButton(\
                onClick = { showCopilot = true; viewModel.clearCopilot() },\
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Oracle") },\
                text = { Text("Ask AI Oracle", fontWeight = FontWeight.Bold) },\
                containerColor = Color(0xFF673AB7),\
                contentColor = Color.White\
            )\
        },\
' app/src/main/java/com/example/ui/screens/ChapterListScreen.kt
