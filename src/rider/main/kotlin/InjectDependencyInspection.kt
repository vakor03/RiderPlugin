import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

class InjectDependencyInspection : LocalInspectionTool() {

    private val logger = Logger.getInstance(InjectDependencyInspection::class.java)

    init {
        logger.info("🔍 InjectDependencyInspection class initialized!")
        println("🔍 DEBUG: InjectDependencyInspection class initialized!")
    }

    override fun getDisplayName(): String = "Injectable dependency field"

    override fun getShortName(): String = "InjectableDependencyField"

    override fun getGroupDisplayName(): String = "Unity"

    override fun isEnabledByDefault(): Boolean = true

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        logger.info("🔍 Building visitor for inspection")
        println("🔍 DEBUG: Building visitor for inspection")

        return object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                super.visitFile(file)

                logger.info("🔍 Visiting file: ${file.name}")
                println("🔍 DEBUG: Visiting file: ${file.name}")

                // Only inspect C# files
                if (!file.name.endsWith(".cs")) {
                    logger.info("❌ Skipping non-C# file")
                    return
                }

                val fileText = file.text
                if (!fileText.contains("MonoBehaviour")) {
                    logger.info("❌ File doesn't contain MonoBehaviour")
                    return
                }

                logger.info("✅ Inspecting MonoBehaviour C# file")
                println("✅ DEBUG: Inspecting MonoBehaviour C# file")

                inspectCSharpFile(file, fileText, holder)
            }
        }
    }

    private fun inspectCSharpFile(file: PsiFile, fileText: String, holder: ProblemsHolder) {
        val lines = fileText.lines()

        logger.info("🔍 Analyzing ${lines.size} lines for injectable fields")
        println("🔍 DEBUG: Analyzing ${lines.size} lines for injectable fields")

        lines.forEachIndexed { index, line ->
            logger.info("📝 Line ${index + 1}: ${line.trim()}")

            if (isInjectableField(line)) {
                val fieldName = extractFieldName(line)
                logger.info("✅ Found injectable field: $fieldName")
                println("✅ DEBUG: Found injectable field: $fieldName")

                if (!isAlreadyInjected(fileText, line)) {
                    // Find a PsiElement on this line to register the problem
                    val lineStartOffset = calculateLineOffset(fileText, index)
                    val privateKeywordOffset = line.indexOf("private")

                    if (privateKeywordOffset >= 0) {
                        val elementOffset = lineStartOffset + privateKeywordOffset
                        val element = file.findElementAt(elementOffset)

                        if (element != null) {
                            logger.info("🎯 Registering problem for field: $fieldName at offset $elementOffset")
                            println("🎯 DEBUG: Registering problem for field: $fieldName")

                            holder.registerProblem(
                                element,
                                "Field '$fieldName' can be injected as dependency",
                                InjectDependencyQuickFix()
                            )
                        } else {
                            logger.info("❌ Could not find PSI element at offset $elementOffset")
                        }
                    }
                } else {
                    logger.info("⏭️ Field already injected: $fieldName")
                }
            }
        }
    }

    private fun calculateLineOffset(fileText: String, lineIndex: Int): Int {
        return fileText.lines().take(lineIndex).sumOf { it.length + 1 }
    }

    private fun isInjectableField(line: String): Boolean {
        val trimmedLine = line.trim()

        logger.info("🔍 Checking if injectable: '$trimmedLine'")

        // Must be private field
        val isPrivate = trimmedLine.startsWith("private")
        if (!isPrivate) {
            logger.info("❌ Not private field")
            return false
        }

        // Must have dependency naming pattern (any private field starting with _ or m_)
        val hasProperNaming = trimmedLine.contains("_") || trimmedLine.contains("m_")
        if (!hasProperNaming) {
            logger.info("❌ No dependency naming pattern (_ or m_)")
            return false
        }

        // For now, accept ALL private fields with _ or m_ prefix
        // Later we can add type checking for interfaces/services
        val isDependencyType = true // Accept all for now

        logger.info("🔍 Analysis: private=$isPrivate, naming=$hasProperNaming, dependency=$isDependencyType")
        println("🔍 DEBUG: Field analysis: private=$isPrivate, naming=$hasProperNaming, dependency=$isDependencyType")

        return isPrivate && hasProperNaming && isDependencyType
    }

    private fun isDependencyType(line: String): Boolean {
        // More lenient checking - accept common dependency patterns
        val hasInterface = line.contains(Regex("\\bI[A-Z]\\w+"))
        val hasServiceSuffix = line.contains("Service") ||
                line.contains("Manager") ||
                line.contains("Handler") ||
                line.contains("Controller") ||
                line.contains("Repository") ||
                line.contains("Provider")

        // Also accept common Unity types that might be injected
        val hasUnityDependency = line.contains("Camera") ||
                line.contains("Rigidbody") ||
                line.contains("Transform") ||
                line.contains("Component")

        // For debugging, let's be very permissive
        val isGenericType = line.contains("<") && line.contains(">")

        return hasInterface || hasServiceSuffix || hasUnityDependency || isGenericType
    }

    private fun isAlreadyInjected(fileText: String, fieldLine: String): Boolean {
        val fieldName = extractFieldName(fieldLine)

        // Simple check if there's already an InjectDependencies method mentioning this field
        val hasInjectMethod = fileText.contains("InjectDependencies")
        val fieldIsAssigned = fileText.contains("$fieldName =") || fileText.contains("$fieldName=")

        val alreadyInjected = hasInjectMethod && fieldIsAssigned

        logger.info("🔍 Already injected check for $fieldName: hasInjectMethod=$hasInjectMethod, fieldIsAssigned=$fieldIsAssigned, result=$alreadyInjected")

        return alreadyInjected
    }

    private fun extractFieldName(line: String): String {
        // Extract field name from patterns like:
        // private Type _fieldName;
        // private Type m_fieldName;
        val regex = Regex("\\b([_m]*\\w+)\\s*[;=]")
        val result = regex.find(line)?.groupValues?.get(1) ?: "unknown"

        logger.info("📝 Extracted field name: '$result' from line: '$line'")

        return result
    }
}