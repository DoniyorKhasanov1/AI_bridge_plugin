package ext.ide.bridges

import com.intellij.openapi.project.Project
import com.intellij.icons.AllIcons
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Path
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit

class CodexToolWindowFactory : ToolWindowFactory {

    private var chatHistory = """
        <html>
        <body style="font-family: 'JetBrains Mono', monospace; padding: 10px; background-color: #000000; color: #bbbbbb;">
            <div id="content">
                <div style="color: #555555; margin-bottom: 10px; font-size: 11px;">[System]: Connection established...</div>
            </div>
        </body>
        </html>
    """.trimIndent()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        panel.background = Color.BLACK

        val chatDisplay = JEditorPane().apply {
            isEditable = false
            contentType = "text/html"
            editorKit = HTMLEditorKit()
            text = chatHistory
            background = Color.BLACK
        }

        fun updateChat(role: String, message: String, isThinking: Boolean = false) {
            val idAttr = if (isThinking) "id='thinking-block'" else ""
            val newMessage = """
            <div $idAttr style="margin-bottom: 12px; padding: 10px; background-color: #1a1a1a; border: 1px solid #2b2b2b; border-radius: 5px;">
                <b style="color: #888888; font-size: 10px; text-transform: uppercase;">$role</b>
                <div style="margin-top: 5px; color: #dddddd; font-family: 'JetBrains Mono', monospace;">${message.replace("\n", "<br>")}</div>
            </div>
        """.trimIndent()

            if (isThinking) {
                chatHistory = chatHistory.replace("</div>\n        </body>", "$newMessage</div>\n        </body>")
            } else {
                if (chatHistory.contains("id='thinking-block'")) {
                    val thinkingPattern = Regex("<div id='thinking-block'.*?</div>.*?</div>", RegexOption.DOT_MATCHES_ALL)
                    chatHistory = chatHistory.replace(thinkingPattern, "")
                }
                chatHistory = chatHistory.replace("</div>\n        </body>", "$newMessage</div>\n        </body>")
            }

            chatDisplay.text = chatHistory
            SwingUtilities.invokeLater {
                chatDisplay.caretPosition = chatDisplay.document.length
            }
        }

        val textField = JBTextField().apply {
            emptyText.text = "Prompt..."
            background = Color(45, 45, 45)
            foreground = Color.WHITE
            caretColor = Color.WHITE
        }

        val sendButton = JButton(AllIcons.Actions.Execute).apply {
            isContentAreaFilled = false
            isFocusable = false
            border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
            preferredSize = Dimension(40, 40)
            toolTipText = "Send to Codex"
        }

        val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            background = Color.BLACK
            border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
        }

        val applyBtn = JButton("Apply", AllIcons.Actions.Commit).apply {
            isContentAreaFilled = false
            isFocusable = false
            foreground = Color.GREEN
            toolTipText = "Apply changes"
            addActionListener {
                Thread {
                    val response = talkToUnixSocket("CMD:APPLY")
                    SwingUtilities.invokeLater { updateChat("SYSTEM", response) }
                }.start()
            }
        }

        val rollbackBtn = JButton("Rollback", AllIcons.Actions.Rollback).apply {
            isContentAreaFilled = false
            isFocusable = false
            foreground = Color.ORANGE
            toolTipText = "Declining changes"
            addActionListener {
                val confirm = JOptionPane.showConfirmDialog(panel, "Are you sure you want to roll the changes back?", "Rollback", JOptionPane.YES_NO_OPTION)
                if (confirm == JOptionPane.YES_OPTION) {
                    Thread {
                        val response = talkToUnixSocket("CMD:ROLLBACK")
                        SwingUtilities.invokeLater { updateChat("SYSTEM", response) }
                    }.start()
                }
            }
        }

        controlPanel.add(applyBtn)
        controlPanel.add(rollbackBtn)

        sendButton.addActionListener {
            val requestText = textField.text.trim()
            if (requestText.isNotEmpty()) {
                updateChat("YOU", requestText)
                textField.text = ""
                sendButton.isEnabled = false

                updateChat("CODEX", "<i>Thinking...</i>", isThinking = true)

                Thread {
                    val response = talkToUnixSocket(requestText)
                    SwingUtilities.invokeLater {
                        updateChat("CODEX", response)
                        sendButton.isEnabled = true
                    }
                }.start()
            }
        }

        panel.add(JBScrollPane(chatDisplay), BorderLayout.CENTER)

        val inputPanel = JPanel(BorderLayout()).apply {
            background = Color.BLACK
            add(textField, BorderLayout.CENTER)
            add(sendButton, BorderLayout.EAST)
        }

        val bottomContainer = JPanel(BorderLayout()).apply {
            background = Color.BLACK
            add(controlPanel, BorderLayout.NORTH)
            add(inputPanel, BorderLayout.SOUTH)
        }

        panel.add(bottomContainer, BorderLayout.SOUTH)

        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun talkToUnixSocket(message: String): String {
        val socketPath = Path.of("/tmp/ide_bridge.sock")
        return try {
            val address = UnixDomainSocketAddress.of(socketPath)
            SocketChannel.open(StandardProtocolFamily.UNIX).use { channel ->
                channel.connect(address)
                channel.write(ByteBuffer.wrap(message.toByteArray()))
                val readBuffer = ByteBuffer.allocate(8192)
                val bytesRead = channel.read(readBuffer)
                if (bytesRead > 0) String(readBuffer.array(), 0, bytesRead).trim() else "Empty response."
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }
}