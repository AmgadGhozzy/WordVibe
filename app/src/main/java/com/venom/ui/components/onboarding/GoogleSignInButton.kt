package com.venom.ui.components.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.venom.wordvibe.R
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.adp),
        shape = RoundedCornerShape(20.adp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(0.9f),
            contentColor = Color(0xFF1F1F1F)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.adp,
            pressedElevation = 0.adp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.adp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon_google),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.adp)
            )
            Spacer(modifier = Modifier.width(12.adp))
            Text(
                text = "Continue with Google",
                fontSize = 16.asp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

