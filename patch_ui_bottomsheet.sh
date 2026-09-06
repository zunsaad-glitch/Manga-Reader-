sed -i '$ d' app/src/main/java/com/example/ui/screens/ChapterListScreen.kt
sed -i '$ d' app/src/main/java/com/example/ui/screens/ChapterListScreen.kt

cat << 'INNEREOF' >> app/src/main/java/com/example/ui/screens/ChapterListScreen.kt

    if (showCopilot) {
        ModalBottomSheet(
            onDismissRequest = { showCopilot = false },
            containerColor = ThemeSurface,
            modifier = Modifier.fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "✨ AI Manga Oracle",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF673AB7),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (copilotMessages.isEmpty() && !copilotIsLoading) {
                        item {
                            Text(
                                "I am the AI Oracle. Ask me for a spoiler-free summary, character list, or what chapter the anime ends on!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeOnSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                SuggestionChip(onClick = { viewModel.sendCopilotMessage(manga, "Give me a spoiler-free pitch!") }, label = { Text("Pitch this to me") })
                                SuggestionChip(onClick = { viewModel.sendCopilotMessage(manga, "Who are the main characters?") }, label = { Text("Characters") })
                            }
                        }
                    }
                    items(copilotMessages) { msg ->
                        val isUser = msg.role == "user"
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isUser) ThemePrimary else Color(0xFF2C2C2E),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    if (copilotIsLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF673AB7))
                            }
                        }
                    }
                }
                
                var inputText by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask the Oracle...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF673AB7)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendCopilotMessage(manga, inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.background(Color(0xFF673AB7), CircleShape)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
INNEREOF
