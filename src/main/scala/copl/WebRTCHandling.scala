package copl

import loci.communicator.experimental.webrtc.WebRTC
import loci.communicator.experimental.webrtc.WebRTC.ConnectorFactory
import loci.registry.Registry
import org.scalajs.dom.UIEvent
import scalatags.JsDom.all._
import scalatags.JsDom.tags2.section

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import loci.communicator.experimental.webrtc

import scala.concurrent.{Future, Promise}

case class WebRTCHandling(registry: Registry) {

  val codec: JsonValueCodec[webrtc.WebRTC.CompleteSession] = JsonCodecMaker.make

  def webrtcHandlingArea: Tag = {

    val renderedTa  = textarea().render
    val renderedPre = pre().render

    var pendingServer: Option[PendingConnection] = None

    def connected() = {
      renderedPre.textContent = ""
      renderedTa.value = ""
    }

    def showSession(s: WebRTC.CompleteSession) = {
      val message = writeToString(s)(codec)
      renderedPre.textContent = message
      org.scalajs.dom.window.getSelection().selectAllChildren(renderedPre)
    }

    val hb = button(
      "host",
      onclick := { uie: UIEvent =>
        val res = webrtcIntermediate(WebRTC.offer())
        res.session.foreach(showSession)
        pendingServer = Some(res)
        registry.connect(res.connector).foreach(_ => connected())
      }
    )

    val cb = button(
      "connect",
      onclick := { uie: UIEvent =>
        val cs = readFromString(renderedTa.value)(codec)
        val connector = pendingServer match {
          case None => // we are client
            val res = webrtcIntermediate(WebRTC.answer())
            res.session.foreach(showSession)
            registry.connect(res.connector).foreach(_ => connected())
            res.connector
          case Some(ss) => // we are server
            pendingServer = None
            ss.connector
        }
        connector.set(cs)
      }
    )

    section(hb, cb, renderedPre, renderedTa)
  }

  case class PendingConnection(connector: WebRTC.Connector, session: Future[WebRTC.CompleteSession])

  def webrtcIntermediate(cf: ConnectorFactory) = {
    val p      = Promise[WebRTC.CompleteSession]()
    val answer = cf complete p.success
    PendingConnection(answer, p.future)
  }

}