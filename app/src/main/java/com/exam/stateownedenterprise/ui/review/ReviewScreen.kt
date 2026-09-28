package com.exam.stateownedenterprise.ui.review

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.exam.stateownedenterprise.data.local.AnswerRecordWithQuestion
import com.exam.stateownedenterprise.data.local.correctOptionsList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when {
                        uiState.selectedRecord != null -> {
                            val qNum = uiState.selectedRecord?.question?.questionNumber ?: 0
                            val idx = (uiState.selectedRecordIndex ?: 0) + 1
                            "第 $qNum 題 ($idx / ${uiState.records.size})"
                        }
                        uiState.records.isNotEmpty() -> "錯題檢討 (${uiState.records.size} 題)"
                        else -> "錯題檢討"
                    }
                    Text(text = titleText)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.isDetailMode) {
                                viewModel.selectRecord(null)
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.isDetailMode) {
                DetailBottomBar(
                    hasPrevious = uiState.hasPrevious,
                    hasNext = uiState.hasNext,
                    onPrevious = viewModel::previousRecord,
                    onNext = viewModel::nextRecord,
                    onBackToList = { viewModel.selectRecord(null) }
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
                uiState.records.isEmpty() -> {
                    EmptyMistakesView(
                        onNavigateBack = onNavigateBack,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
                uiState.selectedRecord != null -> {
                    val selectedRecord = uiState.selectedRecord!!
                    MistakeDetailContent(
                        record = selectedRecord,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    MistakeListView(
                        records = uiState.records,
                        onSelectRecord = { index -> viewModel.selectRecord(index) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMistakesView(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "「本次全部答對！沒有錯題 🎉」",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("返回")
        }
    }
}

@Composable
private fun MistakeListView(
    records: List<AnswerRecordWithQuestion>,
    onSelectRecord: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(records) { index, item ->
            MistakeListItemCard(
                item = item,
                onClick = { onSelectRecord(index) }
            )
        }
    }
}

@Composable
private fun MistakeListItemCard(
    item: AnswerRecordWithQuestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "第 ${item.question.questionNumber} 題. ${item.question.stem}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "你的答案：${item.answerRecord.selectedOption ?: "未作答"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC62828) // Dark Red
                )

                Text(
                    text = "正確答案：${item.question.correctOptionsList().joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D32) // Dark Green
                )
            }
        }
    }
}

@Composable
private fun MistakeDetailContent(
    record: AnswerRecordWithQuestion,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val question = record.question
    val selectedOption = record.answerRecord.selectedOption
    val correctOptions = question.correctOptionsList()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Question stem
        Text(
            text = "${question.questionNumber}. ${question.stem}",
            style = MaterialTheme.typography.titleMedium
        )

        // Options A, B, C, D (read-only)
        val options = listOf(
            "A" to question.optionA,
            "B" to question.optionB,
            "C" to question.optionC,
            "D" to question.optionD
        )

        options.forEach { (optionKey, optionText) ->
            val isUserSelected = selectedOption == optionKey
            val isCorrectOption = correctOptions.contains(optionKey)

            val containerColor = when {
                isCorrectOption -> Color(0xFFE8F5E9) // Light Green
                isUserSelected -> Color(0xFFFFEBEE)  // Light Red
                else -> MaterialTheme.colorScheme.surface
            }

            val borderColor = when {
                isCorrectOption -> Color(0xFF2E7D32) // Dark Green
                isUserSelected -> Color(0xFFC62828)  // Dark Red
                else -> MaterialTheme.colorScheme.outlineVariant
            }

            val labelColor = when {
                isCorrectOption -> Color(0xFF2E7D32)
                isUserSelected -> Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.onSurface
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = BorderStroke(1.5.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "($optionKey)",
                        style = MaterialTheme.typography.titleMedium,
                        color = labelColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = optionText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = labelColor
                    )
                }
            }
        }

        // Feedback / Explanation Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "錯題詳情",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFC62828)
                )

                Text(
                    text = "你的答案：${selectedOption ?: "未作答"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFC62828)
                )

                Text(
                    text = "正確答案：${correctOptions.joinToString(", ")}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2E7D32)
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
}

@Composable
private fun DetailBottomBar(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBackToList: () -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = hasPrevious,
                modifier = Modifier.weight(1f)
            ) {
                Text("上一題")
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onBackToList,
                modifier = Modifier.weight(1f)
            ) {
                Text("返回列表")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onNext,
                enabled = hasNext,
                modifier = Modifier.weight(1f)
            ) {
                Text("下一題")
            }
        }
    }
}
