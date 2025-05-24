import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.intellij.codeInsight.intention.IntentionManager

class DebugTestAction : AnAction("🐛 Debug Plugin Status") {

    private val logger = Logger.getInstance(DebugTestAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        logger.info("🐛 DebugTestAction triggered")
        println("🐛 DEBUG: DebugTestAction triggered")

        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)

        val debugInfo = buildString {
            appendLine("🐛 Plugin Debug Information")
            appendLine("=".repeat(40))
            appendLine()

            // Check if our intention is registered
            val intentionManager = IntentionManager.getInstance()
            val allIntentions = intentionManager.availableIntentions
            val ourIntentions = allIntentions.filter {
                it.javaClass.simpleName.contains("Inject") ||
                        it.familyName.contains("Unity") ||
                        it.familyName.contains("Injection")
            }

            appendLine("🔌 Plugin Registration Status:")
            appendLine("   Total intentions registered: ${allIntentions.size}")
            appendLine("   Our injection intentions found: ${ourIntentions.size}")
            ourIntentions.forEach { intention ->
                appendLine("   - ${intention.javaClass.simpleName}: '${intention.text}' (${intention.familyName})")
            }
            appendLine()

            // Project info
            appendLine("📁 Project: ${project?.name ?: "No project"}")
            appendLine("📝 Editor: ${if (editor != null) "Available" else "Not available"}")
            appendLine("📄 PSI File: ${psiFile?.name ?: "No file"}")
            appendLine()

            // Current file analysis
            if (psiFile != null) {
                appendLine("🔍 Current File Analysis:")
                appendLine("   Name: ${psiFile.name}")
                appendLine("   Type: ${psiFile.fileType.name}")
                appendLine("   Is C#: ${psiFile.name.endsWith(".cs")}")

                val fileText = psiFile.text
                appendLine("   Contains 'MonoBehaviour': ${fileText.contains("MonoBehaviour")}")
                appendLine("   Contains 'private': ${fileText.contains("private")}")
                appendLine("   Contains '_': ${fileText.contains("_")}")
                appendLine("   File size: ${fileText.length} chars")
                appendLine()

                // Check for sample dependency patterns
                val lines = fileText.lines()
                val dependencyLines = lines.filter { line ->
                    line.trim().startsWith("private") &&
                            (line.contains("_") || line.contains("m_")) &&
                            (line.contains("I") || line.contains("Service") || line.contains("Manager"))
                }

                appendLine("🎯 Potential dependency fields found: ${dependencyLines.size}")
                dependencyLines.forEach { line ->
                    appendLine("   - ${line.trim()}")
                }
                appendLine()

                // Sample lines
                val firstLines = fileText.lines().take(10)
                appendLine("📝 First 10 lines:")
                firstLines.forEachIndexed { index, line ->
                    appendLine("   ${index + 1}: ${line.take(80)}")
                }
                appendLine()
            }

            // Caret position
            if (editor != null) {
                val offset = editor.caretModel.offset
                val element = psiFile?.findElementAt(offset)
                appendLine("🎯 Caret Position:")
                appendLine("   Offset: $offset")
                appendLine("   Element: '${element?.text ?: "No element"}'")
                appendLine("   Element type: ${element?.javaClass?.simpleName ?: "Unknown"}")
                appendLine()

                // Test our intention manually
                if (element != null && project != null) {
                    try {
                        val injectIntention = InjectDependencyIntention()
                        val isAvailable = injectIntention.isAvailable(project, editor, element)
                        appendLine("🧪 Manual Intention Test:")
                        appendLine("   InjectDependencyIntention.isAvailable(): $isAvailable")
                        appendLine("   Intention text: '${injectIntention.text}'")
                        appendLine("   Intention family: '${injectIntention.familyName}'")
                    } catch (e: Exception) {
                        appendLine("❌ Error testing intention: ${e.message}")
                    }
                }
            }

            // Test instructions
            appendLine("🧪 Next Steps:")
            appendLine("1. Create/open a C# file")
            appendLine("2. Add this line: private IPlayerService _playerService;")
            appendLine("3. Put cursor exactly on '_playerService'")
            appendLine("4. Press Ctrl+Alt+D again to test intention")
            appendLine("5. Try Alt+Enter or right-click")
            appendLine()
            appendLine("If intention count is 0, check plugin.xml registration!")
        }

        logger.info("Debug info collected: ${debugInfo.length} chars")
        println("🐛 DEBUG INFO:\n$debugInfo")

        Messages.showInfoMessage(debugInfo, "Plugin Debug Info")
    }
}