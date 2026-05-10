package com.minimind.app.ui.training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.minimind.app.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class ToolDefinition(
    val name: String = "",
    val description: String = "",
    val parameters: List<ToolParameter> = emptyList()
)

data class ToolParameter(
    val name: String = "",
    val type: String = "string",
    val description: String = ""
)

data class RewardRule(
    val type: String = "format",
    val condition: String = "",
    val score: String = "1.0"
)

data class TestScenario(
    val userInput: String = "",
    val expectedToolCall: String = "",
    val expectedOutput: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentEnvEditorScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()

    var tools by remember { mutableStateOf(listOf(ToolDefinition())) }
    var rewardRules by remember { mutableStateOf(listOf(RewardRule())) }
    var testScenarios by remember { mutableStateOf(listOf(TestScenario())) }
    var generatedRewardCode by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agentic RL 环境编辑器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "工具定义",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "定义模型可以调用的工具。每个工具需要指定名称、描述和参数。模型会根据用户的提问判断是否需要调用工具，以及调用哪个工具。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            tools.forEachIndexed { index, tool ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = "工具 ${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (tools.size > 1) {
                                IconButton(
                                    onClick = {
                                        tools = tools.toMutableList().also { it.removeAt(index) }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFF666666))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tool.name,
                            onValueChange = { newName ->
                                tools = tools.toMutableList().also {
                                    it[index] = tool.copy(name = newName)
                                }
                            },
                            label = { Text("工具名称") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tool.description,
                            onValueChange = { newDesc ->
                                tools = tools.toMutableList().also {
                                    it[index] = tool.copy(description = newDesc)
                                }
                            },
                            label = { Text("工具描述") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "参数列表",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        tool.parameters.forEachIndexed { paramIndex, param ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = param.name,
                                    onValueChange = { newName ->
                                        val newParams = tool.parameters.toMutableList().also {
                                            it[paramIndex] = param.copy(name = newName)
                                        }
                                        tools = tools.toMutableList().also {
                                            it[index] = tool.copy(parameters = newParams)
                                        }
                                    },
                                    label = { Text("参数名") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                var paramTypeExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(0.7f)) {
                                    OutlinedTextField(
                                        value = param.type,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("类型") },
                                        trailingIcon = {
                                            IconButton(onClick = { paramTypeExpanded = true }) {
                                                Icon(Icons.Default.ArrowDropDown, null)
                                            }
                                        }
                                    )
                                    DropdownMenu(expanded = paramTypeExpanded, onDismissRequest = { paramTypeExpanded = false }) {
                                        listOf("string", "int", "float", "bool", "list").forEach { type ->
                                            DropdownMenuItem(text = { Text(type) }, onClick = {
                                                val newParams = tool.parameters.toMutableList().also {
                                                    it[paramIndex] = param.copy(type = type)
                                                }
                                                tools = tools.toMutableList().also {
                                                    it[index] = tool.copy(parameters = newParams)
                                                }
                                                paramTypeExpanded = false
                                            })
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = param.description,
                                    onValueChange = { newDesc ->
                                        val newParams = tool.parameters.toMutableList().also {
                                            it[paramIndex] = param.copy(description = newDesc)
                                        }
                                        tools = tools.toMutableList().also {
                                            it[index] = tool.copy(parameters = newParams)
                                        }
                                    },
                                    label = { Text("描述") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                if (tool.parameters.size > 1) {
                                    IconButton(onClick = {
                                        val newParams = tool.parameters.toMutableList().also { it.removeAt(paramIndex) }
                                        tools = tools.toMutableList().also {
                                            it[index] = tool.copy(parameters = newParams)
                                        }
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "删除参数", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        TextButton(onClick = {
                            val newParams = tool.parameters.toMutableList().also { it.add(ToolParameter()) }
                            tools = tools.toMutableList().also {
                                it[index] = tool.copy(parameters = newParams)
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加参数")
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { tools = tools + ToolDefinition() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加工具")
            }

            HorizontalDivider()

            Text(
                text = "奖励函数",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "定义如何评价模型的表现。你可以添加多条规则，每条规则指定一个评价维度和对应的奖惩分值。训练时系统会根据这些规则自动计算奖励。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            rewardRules.forEachIndexed { index, rule ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("规则 ${index + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (rewardRules.size > 1) {
                                IconButton(onClick = {
                                    rewardRules = rewardRules.toMutableList().also { it.removeAt(index) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFF666666))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        var ruleTypeExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedTextField(
                                value = when (rule.type) {
                                    "format" -> "格式正确性"
                                    "tool_call" -> "工具调用合法性"
                                    "result_match" -> "结果匹配"
                                    "custom" -> "自定义"
                                    else -> rule.type
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("规则类型") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { ruleTypeExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            )
                            DropdownMenu(expanded = ruleTypeExpanded, onDismissRequest = { ruleTypeExpanded = false }) {
                                listOf("format" to "格式正确性", "tool_call" to "工具调用合法性", "result_match" to "结果匹配", "custom" to "自定义").forEach { (value, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        rewardRules = rewardRules.toMutableList().also {
                                            it[index] = rule.copy(type = value)
                                        }
                                        ruleTypeExpanded = false
                                    })
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rule.condition,
                            onValueChange = { newCond ->
                                rewardRules = rewardRules.toMutableList().also {
                                    it[index] = rule.copy(condition = newCond)
                                }
                            },
                            label = { Text("规则条件") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rule.score,
                            onValueChange = { newScore ->
                                rewardRules = rewardRules.toMutableList().also {
                                    it[index] = rule.copy(score = newScore)
                                }
                            },
                            label = { Text("奖励分值 (可正可负)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { rewardRules = rewardRules + RewardRule() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加奖励规则")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    isGenerating = true
                    coroutineScope.launch {
                        try {
                            val gson = Gson()
                            val toolsConfig = gson.toJson(tools)
                            val rewardRulesJson = gson.toJson(rewardRules)
                            val scenarioDescription = testScenarios.joinToString("\n") { scenario ->
                                "用户输入: ${scenario.userInput}, 预期工具调用: ${scenario.expectedToolCall}, 预期输出: ${scenario.expectedOutput}"
                            }
                            val requestMap = mapOf(
                                "tools_config" to toolsConfig,
                                "reward_rules" to rewardRulesJson,
                                "scenario_description" to scenarioDescription
                            )
                            val requestBody = gson.toJson(requestMap)
                                .toRequestBody("application/json".toMediaType())
                            val result = withContext(Dispatchers.IO) {
                                val client = ApiClient.fetchOkHttpClient()
                                val request = Request.Builder()
                                    .url("${ApiClient.getBaseUrl()}/api/config/generate-reward-function")
                                    .post(requestBody)
                                    .build()
                                val response = client.newCall(request).execute()
                                if (response.isSuccessful) {
                                    val responseBody = response.body?.string() ?: ""
                                    val jsonResponse = gson.fromJson(responseBody, Map::class.java) as Map<String, Any>
                                    jsonResponse["code"] as? String ?: responseBody
                                } else {
                                    "生成失败: HTTP ${response.code}"
                                }
                            }
                            generatedRewardCode = result
                        } catch (e: Exception) {
                            generatedRewardCode = "生成失败: ${e.message}"
                        }
                        isGenerating = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 生成奖励函数")
            }

            if (generatedRewardCode.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "生成的奖励函数代码",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = generatedRewardCode,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "测试场景",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "定义用于验证模型行为的测试用例。每个场景包含一个用户输入、预期的工具调用和预期输出，帮助你评估模型是否学会了正确使用工具。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            testScenarios.forEachIndexed { index, scenario ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("场景 ${index + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (testScenarios.size > 1) {
                                IconButton(onClick = {
                                    testScenarios = testScenarios.toMutableList().also { it.removeAt(index) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFF666666))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = scenario.userInput,
                            onValueChange = { newInput ->
                                testScenarios = testScenarios.toMutableList().also {
                                    it[index] = scenario.copy(userInput = newInput)
                                }
                            },
                            label = { Text("用户输入") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = scenario.expectedToolCall,
                            onValueChange = { newCall ->
                                testScenarios = testScenarios.toMutableList().also {
                                    it[index] = scenario.copy(expectedToolCall = newCall)
                                }
                            },
                            label = { Text("预期工具调用") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = scenario.expectedOutput,
                            onValueChange = { newOutput ->
                                testScenarios = testScenarios.toMutableList().also {
                                    it[index] = scenario.copy(expectedOutput = newOutput)
                                }
                            },
                            label = { Text("预期输出") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { testScenarios = testScenarios + TestScenario() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加测试场景")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
