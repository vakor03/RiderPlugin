import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement

class InjectDependencyIntention : PsiElementBaseIntentionAction(), IntentionAction {

    // Add logger with initialization logging
    private val logger = Logger.getInstance(InjectDependencyIntention::class.java)

    init {
        logger.info("🚀 InjectDependencyIntention class initialized!")
        println("🚀 DEBUG: InjectDependencyIntention class initialized!")
    }

    override fun getText(): String {
        logger.info("📝 getText() called")
        return "Inject this dependency"
    }

    override fun getFamilyName(): String {
        logger.info("👨‍👩‍👧‍👦 getFamilyName() called")
        return "Unity Dependency Injection"
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        logger.info("🔍 ===== InjectDependencyIntention.isAvailable() called =====")
        println("🔍 DEBUG: InjectDependencyIntention.isAvailable() called")

        try {
            val file = element.containingFile
            if (file == null) {
                logger.info("❌ No containing file found")
                println("❌ DEBUG: No containing file found")
                return false
            }

            logger.info("📁 File: ${file.name}")
            println("📁 DEBUG: File: ${file.name}")

            // Simple check - only show for C# files that might contain MonoBehaviour
            if (!file.name.endsWith(".cs")) {
                logger.info("❌ Not a C# file")
                println("❌ DEBUG: Not a C# file")
                return false
            }

            val fileText = file.text
            if (!fileText.contains("MonoBehaviour")) {
                logger.info("❌ File doesn't contain MonoBehaviour")
                println("❌ DEBUG: File doesn't contain MonoBehaviour")
                return false
            }

            logger.info("✅ File contains MonoBehaviour")
            println("✅ DEBUG: File contains MonoBehaviour")

            // Check if cursor is on a private field that looks like a dependency
            val elementText = element.text ?: ""
            logger.info("🎯 Element text: '$elementText'")
            println("🎯 DEBUG: Element text: '$elementText'")

            val isOnDependency = isOnDependencyField(element, fileText)
            logger.info("🎯 Is on dependency field: $isOnDependency")
            println("🎯 DEBUG: Is on dependency field: $isOnDependency")

            return isOnDependency

        } catch (e: Exception) {
            logger.error("💥 Exception in isAvailable: ${e.message}", e)
            println("💥 DEBUG: Exception in isAvailable: ${e.message}")
            return false
        }
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        logger.info("🚀 ===== InjectDependencyIntention.invoke() called =====")
        println("🚀 DEBUG: InjectDependencyIntention.invoke() called")

        try {
            val file = element.containingFile ?: return
            val fileText = file.text

            // Extract field information from context
            val fieldInfo = extractFieldInfoFromContext(element, fileText)
            logger.info("📋 Field info: $fieldInfo")
            println("📋 DEBUG: Field info: $fieldInfo")

            val resultCode = generateInjectMethod(fieldInfo)

            showPreview(fieldInfo, resultCode)

        } catch (e: Exception) {
            logger.error("💥 Exception in invoke: ${e.message}", e)
            println("💥 DEBUG: Exception in invoke: ${e.message}")
        }
    }

    private fun isOnDependencyField(element: PsiElement, fileText: String): Boolean {
        val elementText = element.text ?: return false

        logger.info("🔍 Checking element text: '$elementText'")
        println("🔍 DEBUG: Checking element text: '$elementText'")

        // Look for private field patterns
        val lines = fileText.lines()
        val elementLine = findElementLine(element, lines) ?: return false

        logger.info("📝 Found element line: '$elementLine'")
        println("📝 DEBUG: Found element line: '$elementLine'")

        // Check if line contains private field declaration with dependency pattern
        val hasPrivate = elementLine.contains("private")
        val hasUnderscore = elementLine.contains("_") || elementLine.contains("m_")

        // For now, accept ANY private field with underscore (more permissive)
        val isDependency = true // We'll check dependency type later if needed

        logger.info("🔍 Analysis: private=$hasPrivate, underscore=$hasUnderscore, dependency=$isDependency")
        println("🔍 DEBUG: Analysis: private=$hasPrivate, underscore=$hasUnderscore, dependency=$isDependency")

        val result = hasPrivate && hasUnderscore && isDependency
        logger.info("🎯 Final result: $result")
        println("🎯 DEBUG: Final result: $result")

        return result
    }

    private fun findElementLine(element: PsiElement, lines: List<String>): String? {
        val elementText = element.text ?: return null

        logger.info("🔍 Searching for line containing: '$elementText'")
        println("🔍 DEBUG: Searching for line containing: '$elementText'")

        // Try different strategies to find the line

        // Strategy 1: Direct text match
        var foundLine = lines.find { line ->
            line.contains(elementText) &&
                    (line.contains("private") || line.contains("_") || line.contains("m_"))
        }

        if (foundLine != null) {
            logger.info("✅ Found line via direct match: '$foundLine'")
            println("✅ DEBUG: Found line via direct match: '$foundLine'")
            return foundLine
        }

        // Strategy 2: Look for lines with private fields around the element
        foundLine = lines.find { line ->
            line.trim().startsWith("private") && line.contains("_")
        }

        if (foundLine != null) {
            logger.info("✅ Found line via private field search: '$foundLine'")
            println("✅ DEBUG: Found line via private field search: '$foundLine'")
            return foundLine
        }

        logger.info("❌ No suitable line found")
        println("❌ DEBUG: No suitable line found")
        return null
    }

    private fun isDependencyType(line: String): Boolean {
        val hasInterface = line.contains(Regex("\\bI[A-Z]\\w+"))
        val hasService = line.contains("Service") ||
                line.contains("Manager") ||
                line.contains("Handler") ||
                line.contains("Controller") ||
                line.contains("Repository") ||
                line.contains("Provider")

        logger.info("🔍 Dependency check: interface=$hasInterface, service=$hasService")
        println("🔍 DEBUG: Dependency check: interface=$hasInterface, service=$hasService")

        return hasInterface || hasService
    }

    private fun extractFieldInfoFromContext(element: PsiElement, fileText: String): FieldInfo {
        val lines = fileText.lines()
        val elementLine = findElementLine(element, lines) ?: return FieldInfo("unknown", "unknown", "unknown")

        // Parse field declaration line
        val fieldName = extractFieldName(elementLine)
        val fieldType = extractFieldType(elementLine)
        val parameterName = generateParameterName(fieldName)

        logger.info("📋 Extracted: name='$fieldName', type='$fieldType', param='$parameterName'")
        println("📋 DEBUG: Extracted: name='$fieldName', type='$fieldType', param='$parameterName'")

        return FieldInfo(fieldName, fieldType, parameterName)
    }

    private fun extractFieldName(line: String): String {
        val regex = Regex("\\b([_m]*\\w+)\\s*[;=]")
        val result = regex.find(line)?.groupValues?.get(1) ?: "unknown"
        logger.info("📝 Extracted field name: '$result' from line: '$line'")
        println("📝 DEBUG: Extracted field name: '$result' from line: '$line'")
        return result
    }

    private fun extractFieldType(line: String): String {
        val regex = Regex("private\\s+([A-Za-z][A-Za-z0-9<>\\[\\]]*)")
        val result = regex.find(line)?.groupValues?.get(1) ?: "unknown"
        logger.info("📝 Extracted field type: '$result' from line: '$line'")
        println("📝 DEBUG: Extracted field type: '$result' from line: '$line'")
        return result
    }

    private fun generateParameterName(fieldName: String): String {
        return when {
            fieldName.startsWith("_") -> fieldName.substring(1).replaceFirstChar { it.lowercase() }
            fieldName.startsWith("m_") -> fieldName.substring(2).replaceFirstChar { it.lowercase() }
            else -> fieldName.replaceFirstChar { it.lowercase() }
        }
    }

    private fun generateInjectMethod(fieldInfo: FieldInfo): String {
        return """
        [Inject]
        private void InjectDependencies(${fieldInfo.type} ${fieldInfo.parameterName})
        {
            ${fieldInfo.name} = ${fieldInfo.parameterName};
        }
        """.trimIndent()
    }

    private fun showPreview(fieldInfo: FieldInfo, code: String) {
        val message = buildString {
            appendLine("🔧 Unity Dependency Injection")
            appendLine()
            appendLine("Field: ${fieldInfo.name}")
            appendLine("Type: ${fieldInfo.type}")
            appendLine("Parameter: ${fieldInfo.parameterName}")
            appendLine()
            appendLine("Will create InjectDependencies method:")
            appendLine()
            appendLine(code)
            appendLine()
            appendLine("Note: This is a demonstration of the injection pattern.")
            appendLine("In a full implementation, this would modify your code.")
        }

        logger.info("📋 Showing preview for: ${fieldInfo.name}")
        println("📋 DEBUG: Showing preview for: ${fieldInfo.name}")
        Messages.showInfoMessage(message, "Inject Dependency")
    }

    data class FieldInfo(
        val name: String,
        val type: String,
        val parameterName: String
    )
}