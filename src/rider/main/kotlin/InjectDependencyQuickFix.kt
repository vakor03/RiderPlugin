import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class InjectDependencyQuickFix : LocalQuickFix {

    override fun getName(): String = "🔧 Inject this dependency"

    override fun getFamilyName(): String = "Unity Dependency Injection"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val file = element.containingFile ?: return
        val fileText = file.text

        // Find the field line containing this element
        val fieldInfo = extractFieldInfoFromFile(element, fileText)

        val resultCode = generateInjectMethod(fieldInfo)

        showQuickFixPreview(fieldInfo, resultCode)

        // TODO: Implement actual PSI modification
        // This would create or update the InjectDependencies method in the class
    }

    private fun extractFieldInfoFromFile(element: com.intellij.psi.PsiElement, fileText: String): FieldInfo {
        val lines = fileText.lines()

        // Find the line containing a field declaration near this element
        val elementOffset = element.textOffset
        val lineNumber = fileText.take(elementOffset).count { it == '\n' }

        // Look around the current line for field declarations
        val searchRange = maxOf(0, lineNumber - 2)..minOf(lines.size - 1, lineNumber + 2)

        for (i in searchRange) {
            val line = lines[i]
            if (isFieldDeclaration(line)) {
                val fieldName = extractFieldName(line)
                val fieldType = extractFieldType(line)
                val parameterName = generateParameterName(fieldName)

                return FieldInfo(fieldName, fieldType, parameterName)
            }
        }

        return FieldInfo("unknown", "unknown", "unknown")
    }

    private fun isFieldDeclaration(line: String): Boolean {
        val trimmedLine = line.trim()
        return trimmedLine.startsWith("private") &&
                (trimmedLine.contains("_") || trimmedLine.contains("m_"))
    }

    private fun extractFieldName(line: String): String {
        val regex = Regex("\\b([_m]*\\w+)\\s*[;=]")
        return regex.find(line)?.groupValues?.get(1) ?: "unknown"
    }

    private fun extractFieldType(line: String): String {
        val regex = Regex("private\\s+([A-Za-z][A-Za-z0-9<>\\[\\]]*)")
        return regex.find(line)?.groupValues?.get(1) ?: "unknown"
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

    private fun showQuickFixPreview(fieldInfo: FieldInfo, code: String) {
        val message = buildString {
            appendLine("⚡ Quick Fix: Inject Dependency")
            appendLine()
            appendLine("Field: ${fieldInfo.name}")
            appendLine("Type: ${fieldInfo.type}")
            appendLine("Parameter: ${fieldInfo.parameterName}")
            appendLine()
            appendLine("Will create InjectDependencies method:")
            appendLine()
            appendLine(code)
            appendLine()
            appendLine("✨ This enables dependency injection for this field!")
        }

        Messages.showInfoMessage(message, "Unity Dependency Injection")
    }

    data class FieldInfo(
        val name: String,
        val type: String,
        val parameterName: String
    )
}