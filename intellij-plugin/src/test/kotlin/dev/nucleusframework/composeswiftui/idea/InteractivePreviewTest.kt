package dev.nucleusframework.composeswiftui.idea

import com.google.gson.JsonParser
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

/** The interactive panel against a fake preview host: frames in, touches out, in points. */
class InteractivePreviewTest : BasePlatformTestCase() {
    fun testStreamsFramesAndSendsTouchesInPoints() {
        val server = ServerSocket(0)
        val received = LinkedBlockingQueue<String>()
        val host = Thread {
            server.accept().use { socket ->
                val out = DataOutputStream(socket.getOutputStream())
                fun send(type: Int, payload: ByteArray) {
                    out.writeByte(type)
                    out.writeInt(payload.size)
                    out.write(payload)
                    out.flush()
                }
                send(2, """{"t":"info","width":100,"height":200,"touches":true}""".toByteArray())
                val jpeg = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(200, 400, BufferedImage.TYPE_INT_RGB), "jpg", it) }
                send(1, jpeg.toByteArray())
                socket.getInputStream().bufferedReader().lineSequence().forEach { received += it }
            }
        }.apply { isDaemon = true; start() }

        val module = PreviewModule("/tmp", "/tmp", ":")
        val panel = InteractivePreview("Preview", SourceNavigator(project, module)) {}
        try {
            panel.setSize(400, 600)
            panel.doLayout()
            panel.connect(server.localPort)
            val deadline = System.currentTimeMillis() + 10_000
            while (!panel.liveStatus.startsWith("Live ·") && System.currentTimeMillis() < deadline) {
                PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
                Thread.sleep(10)
            }
            assertTrue(panel.liveStatus, panel.liveStatus.startsWith("Live ·"))

            // The preview (100 × 200 points) fits the screen at 2 pixels per point, centered horizontally.
            val screen = panel.liveScreen
            val x0 = (screen.width - 200) / 2
            val y0 = com.intellij.util.ui.JBUI.scale(12)
            fun mouse(id: Int) = MouseEvent(screen, id, System.currentTimeMillis(), 0, x0 + 50, y0 + 100, 1, false, MouseEvent.BUTTON1)
            screen.dispatchEvent(mouse(MouseEvent.MOUSE_PRESSED))
            screen.dispatchEvent(mouse(MouseEvent.MOUSE_RELEASED))

            val down = JsonParser.parseString(received.poll(5, TimeUnit.SECONDS)!!).asJsonObject
            assertEquals("down", down.get("t").asString)
            assertEquals(25.0, down.get("x").asDouble, 0.6)
            assertEquals(50.0, down.get("y").asDouble, 0.6)
            assertEquals("up", JsonParser.parseString(received.poll(5, TimeUnit.SECONDS)!!).asJsonObject.get("t").asString)
        } finally {
            Disposer.dispose(panel)
            server.close()
            host.interrupt()
        }
    }
}
