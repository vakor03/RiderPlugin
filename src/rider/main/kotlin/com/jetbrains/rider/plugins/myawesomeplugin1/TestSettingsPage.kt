// Put this file in: src/rider/main/kotlin/com/jetbrains/rider/plugins/myawesomeplugin1/
package com.jetbrains.rider.plugins.myawesomeplugin1

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.FormBuilder
import javax.swing.DefaultListModel
import javax.swing.JComponent

class TestSettingsPage : Configurable {

    private lateinit var projectListModel: DefaultListModel<String>
    private lateinit var projectList: JBList<String>

    override fun getDisplayName(): String = "Always Indexed Projects"

    override fun createComponent(): JComponent {
        // Get current project (if any)
        val project = ProjectManager.getInstance().openProjects.firstOrNull()

        // Create a list of mock projects
        projectListModel = DefaultListModel<String>()
        projectListModel.addElement("Core.Domain")
        projectListModel.addElement("Core.Infrastructure")
        projectListModel.addElement("Shared.Utilities")
        projectListModel.addElement("Common.Models")

        projectList = JBList(projectListModel)

        // Create toolbar with + and - buttons
        val toolbarDecorator = ToolbarDecorator.createDecorator(projectList)
            .setAddAction { addProject() }
            .setRemoveAction { removeProject() }
            .disableUpDownActions()

        val projectListPanel = toolbarDecorator.createPanel()

        // Create form
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Always Indexed Projects:", projectListPanel)
            .addVerticalGap(10)
            .addComponent(JBLabel("Current solution: ${project?.name ?: "No project open"}"))
            .addVerticalGap(5)
            .addComponent(JBLabel("Use + to add projects and - to remove them"))
            .addVerticalGap(5)
            .addComponent(JBLabel("(Settings are not saved yet - just for testing UI)"))
            .addComponentFillVertically(javax.swing.JPanel(), 0)  // This pushes everything to the top
            .panel
    }

    private fun addProject() {
        val projectName = Messages.showInputDialog(
            "Enter project name:",
            "Add Project",
            Messages.getQuestionIcon(),
            "",
            object : InputValidator {
                override fun checkInput(inputString: String?): Boolean {
                    return !inputString.isNullOrBlank()
                }
                override fun canClose(inputString: String?): Boolean {
                    return checkInput(inputString)
                }
            }
        )

        if (!projectName.isNullOrBlank() && !projectListModel.contains(projectName)) {
            projectListModel.addElement(projectName)
        }
    }

    private fun removeProject() {
        val selectedIndex = projectList.selectedIndex
        if (selectedIndex >= 0) {
            projectListModel.removeElementAt(selectedIndex)
        }
    }

    override fun isModified(): Boolean = false

    override fun apply() {
        // Do nothing for now
    }

    override fun reset() {
        // Do nothing for now
    }
}