// Minimal Kotlin settings page that just appears
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class AlwaysIndexedProjectsConfigurable : Configurable {

    private lateinit var enableCheckbox: JBCheckBox
    private lateinit var projectsTextField: JBTextField
    private lateinit var panel: JPanel

    override fun getDisplayName(): String = "Always Indexed Projects"

    override fun createComponent(): JComponent {
        enableCheckbox = JBCheckBox("Enable always-indexed projects", false)
        projectsTextField = JBTextField("Core.Domain, Shared.Utils", 30)

        panel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Configure projects that should always be indexed:"))
            .addVerticalGap(10)
            .addComponent(enableCheckbox)
            .addVerticalGap(10)
            .addLabeledComponent("Project names (comma-separated):", projectsTextField)
            .addVerticalGap(10)
            .addComponent(JBLabel("Example: Core.Domain, Shared.Utils, Common.Models"))
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return panel
    }

    override fun isModified(): Boolean {
        // For now, always return false (no changes tracked)
        return false
    }

    override fun apply() {
        // TODO: Save settings
        println("Apply called - Enable: ${enableCheckbox.isSelected}, Projects: ${projectsTextField.text}")
    }

    override fun reset() {
        // TODO: Load settings
        enableCheckbox.isSelected = false
        projectsTextField.text = "Core.Domain, Shared.Utils"
    }
}