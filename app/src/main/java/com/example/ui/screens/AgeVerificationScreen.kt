package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.Calendar

@Composable
fun AgeVerificationScreen(
    onVerified: () -> Unit
) {
    var yearText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
            border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "AGE VERIFICATION", 
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = ThemePrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Confirm Your Age",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "This app contains mature content. Please enter your birth year to proceed:",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = yearText,
                    onValueChange = { 
                        yearText = it
                        if (errorText.isNotEmpty()) errorText = ""
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("Birth Year (e.g. 2000)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("birth_year_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ThemeSurfaceVariant,
                        unfocusedContainerColor = ThemeSurfaceVariant,
                        focusedBorderColor = ThemePrimary,
                        unfocusedBorderColor = ThemeOutline,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (errorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorText, color = MatureBadgeBg, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val year = yearText.toIntOrNull()
                        if (year != null) {
                            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                            val age = currentYear - year
                            if (age >= 18) {
                                onVerified()
                            } else {
                                errorText = "You must be 18 or older to access this app."
                            }
                        } else {
                            errorText = "Please enter a valid 4-digit birth year."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("verify_age_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemePrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "Confirm & Enter", 
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
