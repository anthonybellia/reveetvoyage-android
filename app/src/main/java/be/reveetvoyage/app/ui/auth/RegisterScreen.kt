package be.reveetvoyage.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import be.reveetvoyage.app.ui.components.IOSButton
import be.reveetvoyage.app.ui.components.IOSButtonStyle
import be.reveetvoyage.app.ui.components.IOSTextField
import be.reveetvoyage.app.ui.components.IOSTopBar
import be.reveetvoyage.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    vm: AuthViewModel = hiltViewModel(),
) {
    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.errorMessage.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        IOSTopBar(title = "Créer mon compte", onBack = onBack)

        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IOSTextField(value = prenom,  onValueChange = { prenom = it },  placeholder = "Prénom",       icon = Icons.Default.Person)
            IOSTextField(value = nom,     onValueChange = { nom = it },     placeholder = "Nom",          icon = Icons.Default.Person)
            IOSTextField(value = email,   onValueChange = { email = it },   placeholder = "Email",        icon = Icons.Default.Email)
            IOSTextField(value = password,onValueChange = { password = it },placeholder = "Mot de passe", icon = Icons.Default.Lock, isPassword = true)
            IOSTextField(value = confirm, onValueChange = { confirm = it }, placeholder = "Confirmer",    icon = Icons.Default.Lock, isPassword = true)

            error?.let { Text(it, color = RevRed) }

            IOSButton(
                text = "S'inscrire",
                style = IOSButtonStyle.Primary,
                isLoading = isLoading,
                enabled = prenom.isNotBlank() && nom.isNotBlank()
                       && email.isNotBlank() && password.length >= 8 && password == confirm,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        if (vm.register(prenom, nom, email, password, confirm)) onRegistered()
                    }
                },
            )
        }
    }
}
