package com.exam.stateownedenterprise.ui.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exam.stateownedenterprise.data.local.Question
import com.exam.stateownedenterprise.data.local.correctOptionsList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel,
    onNavigateBack: () -> Unit,
    onPracticeFinished: (attemptId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        val titleText = if (uiState.questions.isNotEmpty()) {
                            "第 ${uiState.currentIndex + 1} / ${uiState.questions.size} 題"
                        } else {
                            "刷題練習"
                        }
                        Text(text = titleText)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Text("←", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                )
                // Running progress summary header
                if (uiState.questions.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.runningProgressText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (uiState.questions.isNotEmpty()) {
                BottomBarNavigation(
                    hasPrevious = uiState.hasPrevious,
                    hasNext = uiState.hasNext,
                    isLastQuestion = uiState.isLastQuestion,
                    isCurrentQuestionAnswered = uiState.isCurrentQuestionAnswered,
                    onPrevious = viewModel::previousQuestion,
                    onNext = viewModel::nextQuestion,
                    onFinish = { viewModel.finishPractice(onPracticeFinished) }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.questions.isEmpty() -> {
                    Text(
                        text = "目前尚無題目",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    val question = uiState.currentQuestion
                    if (question != null) {
                        QuestionContent(
                            question = question,
                            selectedOption = uiState.currentSelectedOption,
                            isCorrect = uiState.isCurrentAnswerCorrect(),
                            onSelectOption = viewModel::selectOption,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionContent(
    question: Question,
    selectedOption: String?,
    isCorrect: Boolean?,
    onSelectOption: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bonus question badge
        if (question.isBonus) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "【送分題】本題一律送分",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Question stem
        Text(
            text = "${question.questionNumber}. ${question.stem}",
            style = MaterialTheme.typography.titleMedium
        )

        // Options
        val options = listOf(
            "A" to question.optionA,
            "B" to question.optionB,
            "C" to question.optionC,
            "D" to question.optionD
        )

        options.forEach { (optionKey, optionText) ->
            OptionCard(
                optionKey = optionKey,
                optionText = optionText,
                isSelected = selectedOption == optionKey,
                onClick = { onSelectOption(optionKey) }
            )
        }

        // Answer Feedback Card (shown after an option is selected)
        if (selectedOption != null && isCorrect != null) {
            FeedbackCard(
                question = question,
                isCorrect = isCorrect
            )
        }
    }
}

@Composable
private fun OptionCard(
    optionKey: String,
    optionText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "($optionKey)",
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    question: Question,
    isCorrect: Boolean,
    modifier: Modifier = Modifier
) {
    val feedbackContainerColor = if (isCorrect) {
        Color(0xFFE8F5E9) // Light green
    } else {
        Color(0xFFFFEBEE) // Light red
    }

    val feedbackHeaderColor = if (isCorrect) {
        Color(0xFF2E7D32) // Dark green
    } else {
        Color(0xFFC62828) // Dark red
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = feedbackContainerColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isCorrect) "答對了！" else "答錯了！",
                style = MaterialTheme.typography.titleLarge,
                color = feedbackHeaderColor
            )

            val acceptedAnswersText = if (question.isBonus) {
                "本題一律送分，選擇任意選項皆給分"
            } else {
                "正確答案：${question.correctOptionsList().joinToString(", ")}"
            }

            Text(
                text = acceptedAnswersText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            val explanation = question.explanation
            if (!explanation.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "解析：",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BottomBarNavigation(
    hasPrevious: Boolean,
    hasNext: Boolean,
    isLastQuestion: Boolean,
    isCurrentQuestionAnswered: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = hasPrevious,
                modifier = Modifier.weight(1f)
            ) {
                Text("上一題")
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (!isLastQuestion) {
                Button(
                    onClick = onNext,
                    enabled = hasNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("下一題")
                }
            } else {
                Button(
                    onClick = onFinish,
                    enabled = isCurrentQuestionAnswered,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("完成練習")
                }
            }
        }
    }
}
