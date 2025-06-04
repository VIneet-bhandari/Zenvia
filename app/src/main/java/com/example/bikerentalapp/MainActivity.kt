package com.example.bikerentalapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bikerentalapp.ui.theme.BikeRentalTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : ComponentActivity() {
    private var viewModel: AuthViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        setContent {
            viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            BikeRentalTheme {
                AppContent()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            viewModel?.handleGoogleAccountSelection(data)
        }
    }
}

@Composable
private fun AppContent(
    viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var showSplash by remember { mutableStateOf(true) }
    
    if (showSplash) {
        SplashScreen(onSplashComplete = { showSplash = false })
    } else {
        AppNavigation(viewModel = viewModel)
    }
}

@Composable
private fun AppNavigation(
    viewModel: AuthViewModel
) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onSignUpClick = { navController.navigate("signup") },
                onLoginSuccess = { navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }},
                viewModel = viewModel
            )
        }
        composable("signup") {
            SignUpScreen(
                onNavigateBack = { navController.navigateUp() },
                onSignUpComplete = { navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }},
                viewModel = viewModel
            )
        }
        composable("home") {
            HomeScreen(
                onSignOut = { navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }},
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onSignUpClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    val authState by viewModel.authState.collectAsState()
    val selectedEmail by viewModel.selectedEmail.collectAsState()
    
    // Update email when Google account is selected
    LaunchedEffect(selectedEmail) {
        selectedEmail?.let {
            email = it
        }
    }

    // Handle authentication state
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                showError = null
                onLoginSuccess()
            }
            is AuthState.Error -> {
                showError = (authState as AuthState.Error).message
            }
            else -> {
                showError = null
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorResource(id = R.color.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Login with Zenvia",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.zenvia_yellow),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Show error message if any
            if (showError != null) {
                Text(
                    text = showError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    showError = null
                    viewModel.clearError()
                },
                label = { Text("Email", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = colorResource(id = R.color.zenvia_yellow),
                    focusedBorderColor = colorResource(id = R.color.zenvia_yellow),
                    cursorColor = colorResource(id = R.color.zenvia_yellow),
                    unfocusedTextColor = Color.Black,
                    focusedTextColor = Color.Black
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Icon",
                        tint = colorResource(id = R.color.zenvia_yellow)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                isError = showError != null && showError!!.contains("email", ignoreCase = true)
            )

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    showError = null
                    viewModel.clearError()
                },
                label = { Text("Password", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = colorResource(id = R.color.zenvia_yellow),
                    focusedBorderColor = colorResource(id = R.color.zenvia_yellow),
                    cursorColor = colorResource(id = R.color.zenvia_yellow),
                    unfocusedTextColor = Color.Black,
                    focusedTextColor = Color.Black
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password Icon",
                        tint = colorResource(id = R.color.zenvia_yellow)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = colorResource(id = R.color.zenvia_yellow)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                isError = showError != null && showError!!.contains("password", ignoreCase = true)
            )

            // Login Button
            Button(
                onClick = { 
                    viewModel.signInWithEmailAndPassword(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.zenvia_yellow),
                    contentColor = Color.White
                ),
                enabled = !authState.isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (authState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Login",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // OR Divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    color = Color.Gray.copy(alpha = 0.5f)
                )
                Text(
                    text = "OR",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    color = Color.Gray.copy(alpha = 0.5f)
                )
            }

            // Google Sign In Button
            OutlinedButton(
                onClick = {
                    viewModel.initiateGoogleSignIn(context)
                    val signInIntent = viewModel.getGoogleSignInClient(context).signInIntent
                    (context as Activity).startActivityForResult(signInIntent, RC_SIGN_IN)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = SolidColor(colorResource(id = R.color.zenvia_yellow))
                ),
                enabled = !authState.isLoading
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Icon",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Google Account",
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                }
            }

            // Sign Up Link
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = Color.Gray
                )
                TextButton(
                    onClick = onSignUpClick,
                    enabled = !authState.isLoading
                ) {
                    Text(
                        text = "Sign Up",
                        color = colorResource(id = R.color.zenvia_yellow),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private const val RC_SIGN_IN = 9001 