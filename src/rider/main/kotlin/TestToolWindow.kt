import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import java.awt.BorderLayout
import javax.swing.JOptionPane

class TestToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())

        val label = JLabel("Your plugin is loaded!", JLabel.CENTER)
        val button = JButton("Click me!")

        button.addActionListener {
            JOptionPane.showMessageDialog(
                panel,
                "Button clicked! Plugin is working!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            )
        }

        panel.add(label, BorderLayout.CENTER)
        panel.add(button, BorderLayout.SOUTH)

        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}