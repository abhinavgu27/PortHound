package com.example.a1st.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hiddencameradetector.R
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val color: Color,
    val icon: ImageVector
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onFinished: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            stringResource(R.string.net_headline),
            stringResource(R.string.net_desc),
            Color(0xFF00FFFF),
            Icons.Default.Wifi
        ),
        OnboardingPage(
            stringResource(R.string.emf_headline),
            stringResource(R.string.emf_desc),
            Color(0xFFFF0055),
            Icons.Default.Radar
        ),
        OnboardingPage(
            "PHYSICAL SWEEP",
            "Use the IR Vision tool to reveal camera lenses that are invisible to the naked eye.",
            Color(0xFFAA00FF),
            Icons.Default.Security
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050B18), Color(0xFF1A0B2E))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { position ->
                OnboardingPageView(pages[position])
            }

            Row(
                Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) pages[iteration].color else Color.Gray
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(10.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 32.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = pages[pagerState.currentPage].color),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.size - 1) stringResource(R.string.btn_get_started) else stringResource(R.string.btn_next),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun OnboardingPageView(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = page.color
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = page.title,
            color = page.color,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = page.description,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
    }
}
