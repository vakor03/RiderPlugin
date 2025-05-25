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
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JComboBox
import javax.swing.JButton
import java.awt.FlowLayout
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class TestSettingsPage : Configurable {

    private lateinit var projectListModel: DefaultListModel<String>
    private lateinit var projectList: JBList<String>
    private lateinit var projectDropdown: JComboBox<String>
    private lateinit var filterField: JBTextField
    private var allAvailableProjects: List<String> = emptyList()

    override fun getDisplayName(): String = "Always Indexed Projects"

    override fun createComponent(): JComponent {
        // Get current project (if any)
        val project = ProjectManager.getInstance().openProjects.firstOrNull()

        // Create a list of already selected projects
        projectListModel = DefaultListModel<String>()
        projectListModel.addElement("Core.Domain")
        projectListModel.addElement("Core.Infrastructure")

        projectList = JBList(projectListModel)

        // Create toolbar with + and - buttons
        val toolbarDecorator = ToolbarDecorator.createDecorator(projectList)
            .setAddAction { addProject() }
            .setRemoveAction { removeProject() }
            .disableUpDownActions()

        val projectListPanel = toolbarDecorator.createPanel()

        // Create filter field
        filterField = JBTextField(20)
        filterField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = filterProjects()
            override fun removeUpdate(e: DocumentEvent?) = filterProjects()
            override fun changedUpdate(e: DocumentEvent?) = filterProjects()
        })

        // Create dropdown with available projects
        projectDropdown = JComboBox<String>()
        loadAvailableProjects()

        // Create panel for filter and dropdown
        val filterPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        filterPanel.add(JBLabel("Filter:"))
        filterPanel.add(filterField)

        val dropdownPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        dropdownPanel.add(JBLabel("Available projects:"))
        dropdownPanel.add(projectDropdown)
        val addFromDropdownButton = JButton("Add Selected")
        addFromDropdownButton.addActionListener { addProjectFromDropdown() }
        dropdownPanel.add(addFromDropdownButton)

        // Create form
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Always Indexed Projects:", projectListPanel)
            .addVerticalGap(15)
            .addComponent(JBLabel("Add projects from solution:"))
            .addVerticalGap(5)
            .addComponent(filterPanel)
            .addVerticalGap(5)
            .addComponent(dropdownPanel)
            .addVerticalGap(10)
            .addComponent(JBLabel("Current solution: ${project?.name ?: "No project open"}"))
            .addVerticalGap(5)
            .addComponent(JBLabel("Use + to add projects manually, or filter and select from dropdown"))
            .addVerticalGap(5)
            .addComponent(JBLabel("(Settings are not saved yet - just for testing UI)"))
            .addComponentFillVertically(javax.swing.JPanel(), 0)
            .panel
    }

    private fun loadAvailableProjects() {
        // Mock project names that would typically be in a solution (more projects for better filtering demo)
        allAvailableProjects = listOf(
            "Web.API",
            "Web.Client",
            "Web.Admin",
            "Business.Logic",
            "Business.Rules",
            "Data.Access",
            "Data.Repository",
            "Core.Models",
            "Core.Services",
            "Core.Entities",
            "Shared.Utils",
            "Shared.Contracts",
            "Shared.Events",
            "Integration.Tests",
            "Integration.External",
            "Unit.Tests",
            "Unit.Mocks",
            "Common.Contracts",
            "Common.Extensions",
            "Common.Logging",
            "Auth.Service",
            "Auth.Models",
            "Payment.Service",
            "Payment.Gateway",
            "Notification.Service",
            "Notification.Email"
        )

        filterProjects()
    }

    private fun filterProjects() {
        projectDropdown.removeAllItems()

        val filterText = filterField.text.lowercase()
        val alreadySelectedProjects = (0 until projectListModel.size()).map {
            projectListModel.getElementAt(it)
        }.toSet()

        allAvailableProjects
            .filter { project ->
                project.lowercase().contains(filterText) &&
                        !alreadySelectedProjects.contains(project)
            }
            .forEach { project ->
                projectDropdown.addItem(project)
            }

        // Update button state
        updateAddButtonState()
    }

    private fun updateAddButtonState() {
        // You could disable the add button if no items in dropdown, but for now we'll keep it simple
    }

    private fun addProjectFromDropdown() {
        val selectedProject = projectDropdown.selectedItem as? String
        if (!selectedProject.isNullOrBlank() && !projectListModel.contains(selectedProject)) {
            projectListModel.addElement(selectedProject)
            filterProjects() // Refresh the dropdown to remove the added project
        }
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
            filterProjects() // Refresh the dropdown
        }
    }

    private fun removeProject() {
        val selectedIndex = projectList.selectedIndex
        if (selectedIndex >= 0) {
            projectListModel.removeElementAt(selectedIndex)
            filterProjects() // Refresh the dropdown to add the project back
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