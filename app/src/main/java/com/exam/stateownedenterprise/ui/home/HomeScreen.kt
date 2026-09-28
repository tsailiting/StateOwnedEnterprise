package com.exam.stateownedenterprise.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartPractice: (examSubjectId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val years by viewModel.years.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val subjectOptions by viewModel.subjectOptions.collectAsStateWithLifecycle()

    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedExamSubjectId by viewModel.selectedExamSubjectId.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "國營事業考古題") }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "選擇練習考卷",
                style = MaterialTheme.typography.titleLarge
            )

            // Year Selector
            DropdownSelector(
                label = "年度",
                selectedValueText = selectedYear?.let { "${it} 年" } ?: "",
                placeholder = "請選擇年度",
                options = years,
                optionToText = { "${it} 年" },
                onOptionSelected = { year -> viewModel.selectYear(year) },
                enabled = years.isNotEmpty()
            )

            // Category Selector
            DropdownSelector(
                label = "類別",
                selectedValueText = selectedCategory ?: "",
                placeholder = if (selectedYear == null) "請先選擇年度" else "請選擇類別",
                options = categories,
                optionToText = { it },
                onOptionSelected = { cat -> viewModel.selectCategory(cat) },
                enabled = selectedYear != null && categories.isNotEmpty()
            )

            // Subject Selector
            val selectedSubjectName = subjectOptions.find { it.examSubjectId == selectedExamSubjectId }?.subjectName ?: ""
            DropdownSelector(
                label = "科目",
                selectedValueText = selectedSubjectName,
                placeholder = if (selectedCategory == null) "請先選擇類別" else "請選擇科目",
                options = subjectOptions,
                optionToText = { it.subjectName },
                onOptionSelected = { option -> viewModel.selectExamSubject(option.examSubjectId) },
                enabled = selectedCategory != null && subjectOptions.isNotEmpty()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    selectedExamSubjectId?.let { onStartPractice(it) }
                },
                enabled = selectedExamSubjectId != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "開始刷題",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSelector(
    label: String,
    selectedValueText: String,
    placeholder: String,
    options: List<T>,
    optionToText: (T) -> String,
    onOptionSelected: (T) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = if (selectedValueText.isNotEmpty()) selectedValueText else "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionToText(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
