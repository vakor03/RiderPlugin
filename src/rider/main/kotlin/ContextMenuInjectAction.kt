import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange

class ContextMenuInjectAction : AnAction("🔧 Inject This Field") {

    private val logger = Logger.getInstance(ContextMenuInjectAction::class.java)

    init {
        logger.info("🎯 ContextMenuInjectAction initialized")
        println("🎯 DEBUG: ContextMenuInjectAction initialized")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)

        val shouldShow = psiFile != null &&
                editor != null &&
                psiFile.name.endsWith(".cs") &&
                psiFile.text.contains("MonoBehaviour") &&
                isOnPrivateField(editor, psiFile)

        e.presentation.isEnabledAndVisible = shouldShow

        if (shouldShow) {
            logger.info("🎯 Context menu action available")
            println("🎯 DEBUG: Context menu action available")
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val project = e.project ?: return

        logger.info("🎯 Context menu action triggered!")
        println("🎯 DEBUG: Context menu action triggered!")

        val currentLine = getCurrentLine(editor, psiFile)
        logger.info("📝 Current line: '$currentLine'")
        println("📝 DEBUG: Current line: '$currentLine'")

        if (currentLine == null) {
            logger.info("❌ Current line is null")
            println("❌ DEBUG: Current line is null")
            return
        }

        val isPrivateField = isPrivateFieldLine(currentLine)
        logger.info("🔍 Is private field line: $isPrivateField")
        println("🔍 DEBUG: Is private field line: $isPrivateField")

        if (!isPrivateField) {
            logger.info("❌ Not a private field line")
            println("❌ DEBUG: Not a private field line")
            Messages.showInfoMessage(
                project,
                "Current line is not a private field with underscore:\n'$currentLine'\n\nExpected format: private IType _fieldName;",
                "Not a Dependency Field"
            )
            return
        }

        val fieldName = extractFieldName(currentLine)
        val fieldType = extractFieldType(currentLine)
        val parameterName = generateParameterName(fieldName)

        logger.info("📝 Processing field: name='$fieldName', type='$fieldType', param='$parameterName'")
        println("📝 DEBUG: Processing field: name='$fieldName', type='$fieldType', param='$parameterName'")

        if (fieldName == "unknown" || fieldType == "unknown") {
            logger.info("❌ Could not extract field info")
            println("❌ DEBUG: Could not extract field info")
            Messages.showInfoMessage(
                project,
                "Could not extract field information from line:\n'$currentLine'\n\nFieldName: $fieldName\nFieldType: $fieldType",
                "Extraction Failed"
            )
            return
        }

        logger.info("🚀 Starting write command action")
        println("🚀 DEBUG: Starting write command action")

        // Automatically modify the code without confirmation
        WriteCommandAction.runWriteCommandAction(project, "Inject Dependency", null, {
            try {
                logger.info("📄 File text length: ${psiFile.text.length}")
                println("📄 DEBUG: File text length: ${psiFile.text.length}")

                val isUpdate = createOrUpdateInjectMethod(editor.document, psiFile.text, fieldName, fieldType, parameterName)

                val action = if (isUpdate) "Updated" else "Created"
                logger.info("✅ $action injection method for '$fieldName'")
                println("✅ DEBUG: $action injection method for '$fieldName'")

            } catch (e: Exception) {
                logger.error("❌ Error creating injection method", e)
                println("❌ DEBUG: Error creating injection method: ${e.message}")
                e.printStackTrace()
            }
        })

        // Show success message OUTSIDE the write action to avoid AWT error
//        val action = if (hasExistingInjectMethod(psiFile.text.lines())) "Updated" else "Created"
//        Messages.showInfoMessage(
//            project,
//            "$action injection method for '$fieldName'!\n\nField: $fieldName\nType: $fieldType\nParameter: $parameterName",
//            "Injection Complete"
//        )
    }

    private fun createOrUpdateInjectMethod(document: Document, fileText: String, fieldName: String, fieldType: String, parameterName: String): Boolean {
        val lines = fileText.lines().toMutableList()

        return if (hasExistingInjectMethod(lines)) {
            logger.info("🔄 Updating existing inject method")
            println("🔄 DEBUG: Updating existing inject method")
            updateExistingInjectMethod(document, lines, fieldName, fieldType, parameterName)
            true
        } else {
            logger.info("➕ Creating new inject method")
            println("➕ DEBUG: Creating new inject method")
            createNewInjectMethod(document, lines, fieldName, fieldType, parameterName)
            false
        }
    }

    private fun hasExistingInjectMethod(lines: List<String>): Boolean {
        return lines.any { it.trim() == "[Inject]" } &&
                lines.any { it.contains("InjectDependencies") }
    }

    private fun updateExistingInjectMethod(document: Document, lines: MutableList<String>, fieldName: String, fieldType: String, parameterName: String) {
        val injectMethodStart = findInjectMethodStart(lines)
        if (injectMethodStart == -1) {
            createNewInjectMethod(document, lines, fieldName, fieldType, parameterName)
            return
        }

        val methodSignatureLine = findMethodSignatureLine(lines, injectMethodStart)
        val methodBodyStart = findMethodBodyStart(lines, injectMethodStart)
        val methodBodyEnd = findMethodBodyEnd(lines, methodBodyStart)

        if (methodSignatureLine != -1 && methodBodyStart != -1 && methodBodyEnd != -1) {
            // Add parameter to signature if not already present
            val currentSignature = lines[methodSignatureLine]
            if (!currentSignature.contains(parameterName)) {
                lines[methodSignatureLine] = addParameterToSignature(currentSignature, fieldType, parameterName)
            }

            // Add assignment if not already present
            val hasAssignment = (methodBodyStart until methodBodyEnd).any { i ->
                lines[i].contains("$fieldName =")
            }

            if (!hasAssignment) {
                val indentation = getIndentation(lines[methodBodyStart + 1])
                lines.add(methodBodyEnd - 1, "$indentation$fieldName = $parameterName;")
            }

            document.setText(lines.joinToString("\n"))
        }
    }

    private fun createNewInjectMethod(document: Document, lines: MutableList<String>, fieldName: String, fieldType: String, parameterName: String) {
        logger.info("🏗️ Creating new inject method - starting analysis")
        println("🏗️ DEBUG: Creating new inject method - starting analysis")

        // Debug: print all lines
        logger.info("📋 File has ${lines.size} lines:")
        println("📋 DEBUG: File has ${lines.size} lines:")
        lines.forEachIndexed { index, line ->
            logger.info("  Line $index: '$line'")
            println("  DEBUG Line $index: '$line'")
        }

        // Find the current field line to determine which class it belongs to
        val fieldLineIndex = findFieldLineIndex(lines, fieldName)
        logger.info("📍 Field line index: $fieldLineIndex")
        println("📍 DEBUG: Field line index: $fieldLineIndex")

        if (fieldLineIndex == -1) {
            logger.info("❌ Could not find field line")
            println("❌ DEBUG: Could not find field line")
            return
        }

        val classEndIndex = findClassEndIndexForField(lines, fieldLineIndex)
        logger.info("🏁 Class end index: $classEndIndex")
        println("🏁 DEBUG: Class end index: $classEndIndex")

        if (classEndIndex == -1) {
            logger.info("❌ Could not find class end")
            println("❌ DEBUG: Could not find class end")
            return
        }

        val indentation = getClassIndentation(lines, fieldLineIndex)
        logger.info("📏 Using indentation: '$indentation' (length: ${indentation.length})")
        println("📏 DEBUG: Using indentation: '$indentation' (length: ${indentation.length})")

        val injectMethod = listOf(
            "",
            "$indentation[Inject]",
            "${indentation}private void InjectDependencies($fieldType $parameterName)",
            "$indentation{",
            "$indentation    $fieldName = $parameterName;",
            "$indentation}"
        )

        logger.info("📝 Generated method:")
        println("📝 DEBUG: Generated method:")
        injectMethod.forEach { line ->
            logger.info("  '$line'")
            println("  DEBUG: '$line'")
        }

        // Insert the method before the class closing brace
        logger.info("📍 Inserting at index $classEndIndex")
        println("📍 DEBUG: Inserting at index $classEndIndex")

        lines.addAll(classEndIndex, injectMethod)

        logger.info("📄 Setting document text (${lines.size} lines total)")
        println("📄 DEBUG: Setting document text (${lines.size} lines total)")

        val newText = lines.joinToString("\n")
        logger.info("📄 New text length: ${newText.length}")
        println("📄 DEBUG: New text length: ${newText.length}")

        document.setText(newText)

        logger.info("✅ Document updated successfully")
        println("✅ DEBUG: Document updated successfully")
    }

    private fun findFieldLineIndex(lines: List<String>, fieldName: String): Int {
        for (i in lines.indices) {
            if (lines[i].contains(fieldName) && lines[i].contains("private")) {
                return i
            }
        }
        return -1
    }

    private fun findClassEndIndexForField(lines: List<String>, fieldLineIndex: Int): Int {
        if (fieldLineIndex == -1) {
            logger.info("❌ Field line index is -1")
            println("❌ DEBUG: Field line index is -1")
            return -1
        }

        logger.info("🔍 Looking for class end starting from field line $fieldLineIndex")
        println("🔍 DEBUG: Looking for class end starting from field line $fieldLineIndex")

        // Find the class that contains this field by looking for opening and closing braces
        var braceCount = 0
        var classStartFound = false

        // Go backwards to find the class opening
        for (i in fieldLineIndex downTo 0) {
            val line = lines[i].trim()
            logger.info("  Checking backwards line $i: '$line'")
            println("  DEBUG: Checking backwards line $i: '$line'")

            if (line.contains("class") && line.contains(":") && line.contains("MonoBehaviour")) {
                logger.info("  ✅ Found class declaration at line $i")
                println("  ✅ DEBUG: Found class declaration at line $i")
                classStartFound = true
                break
            }
        }

        if (!classStartFound) {
            logger.info("❌ Could not find class declaration")
            println("❌ DEBUG: Could not find class declaration")
            return -1
        }

        // Now find the opening brace for this class
        var openBraceFound = false
        for (i in fieldLineIndex downTo 0) {
            val line = lines[i].trim()
            if (line == "{") {
                logger.info("  ✅ Found opening brace at line $i")
                println("  ✅ DEBUG: Found opening brace at line $i")
                braceCount = 1
                openBraceFound = true
                break
            }
        }

        if (!openBraceFound) {
            logger.info("❌ Could not find opening brace")
            println("❌ DEBUG: Could not find opening brace")
            return -1
        }

        // Go forwards to find the matching closing brace
        for (i in (fieldLineIndex + 1) until lines.size) {
            val line = lines[i]
            val openBraces = line.count { it == '{' }
            val closeBraces = line.count { it == '}' }

            braceCount += openBraces
            braceCount -= closeBraces

            logger.info("  Line $i: '$line' -> braces: +$openBraces -$closeBraces = $braceCount")
            println("  DEBUG: Line $i: '$line' -> braces: +$openBraces -$closeBraces = $braceCount")

            if (braceCount == 0) {
                logger.info("🎯 Found class end at line $i: '${line.trim()}'")
                println("🎯 DEBUG: Found class end at line $i: '${line.trim()}'")
                return i
            }
        }

        logger.info("❌ Could not find matching closing brace")
        println("❌ DEBUG: Could not find matching closing brace")
        return -1
    }

    private fun getClassIndentation(lines: List<String>, fieldLineIndex: Int): String {
        if (fieldLineIndex != -1 && fieldLineIndex < lines.size) {
            // Use the same indentation as the field
            val fieldIndentation = getIndentation(lines[fieldLineIndex])
            logger.info("📏 Using field indentation: '$fieldIndentation'")
            println("📏 DEBUG: Using field indentation: '$fieldIndentation'")
            return fieldIndentation
        }

        // Fallback: find any method or field indentation
        for (line in lines) {
            val trimmed = line.trim()
            if ((trimmed.startsWith("private") || trimmed.startsWith("public")) && line.isNotEmpty()) {
                return getIndentation(line)
            }
        }

        return "    " // Default 4-space indentation
    }

    private fun findInjectMethodStart(lines: List<String>): Int {
        for (i in lines.indices) {
            if (lines[i].trim() == "[Inject]" && i + 1 < lines.size &&
                lines[i + 1].contains("InjectDependencies")) {
                return i
            }
        }
        return -1
    }

    private fun findMethodSignatureLine(lines: List<String>, injectMethodStart: Int): Int {
        for (i in injectMethodStart until minOf(injectMethodStart + 3, lines.size)) {
            if (lines[i].contains("InjectDependencies")) {
                return i
            }
        }
        return -1
    }

    private fun findMethodBodyStart(lines: List<String>, injectMethodStart: Int): Int {
        for (i in injectMethodStart until lines.size) {
            if (lines[i].trim() == "{") {
                return i
            }
        }
        return -1
    }

    private fun findMethodBodyEnd(lines: List<String>, methodBodyStart: Int): Int {
        var braceCount = 1
        for (i in (methodBodyStart + 1) until lines.size) {
            val line = lines[i].trim()
            braceCount += line.count { it == '{' }
            braceCount -= line.count { it == '}' }
            if (braceCount == 0) {
                return i + 1
            }
        }
        return lines.size
    }

    private fun addParameterToSignature(signature: String, fieldType: String, parameterName: String): String {
        val openParen = signature.indexOf('(')
        val closeParen = signature.lastIndexOf(')')

        if (openParen != -1 && closeParen != -1) {
            val currentParams = signature.substring(openParen + 1, closeParen).trim()
            val newParam = "$fieldType $parameterName"

            val updatedParams = if (currentParams.isEmpty()) {
                newParam
            } else {
                "$currentParams, $newParam"
            }

            return signature.substring(0, openParen + 1) + updatedParams + signature.substring(closeParen)
        }

        return signature
    }

    private fun getIndentation(line: String): String {
        return line.takeWhile { it.isWhitespace() }
    }

    private fun isOnPrivateField(editor: com.intellij.openapi.editor.Editor, psiFile: com.intellij.psi.PsiFile): Boolean {
        val currentLine = getCurrentLine(editor, psiFile) ?: return false
        return isPrivateFieldLine(currentLine)
    }

    private fun getCurrentLine(editor: com.intellij.openapi.editor.Editor, psiFile: com.intellij.psi.PsiFile): String? {
        val offset = editor.caretModel.offset
        val document = editor.document
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)

        return document.getText(TextRange(lineStart, lineEnd))
    }

    private fun isPrivateFieldLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("private") && (trimmed.contains("_") || trimmed.contains("m_"))
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
}