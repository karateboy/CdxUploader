package com.wecc

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}
import com.wecc.Uploader.{Upload, UploadRange}
import org.slf4j.{Logger, LoggerFactory}
import scalafx.Includes._
import scalafx.application.{JFXApp, Platform}
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.ButtonBar.ButtonData
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{GridPane, HBox, VBox}
import scalafx.scene.paint.Color._
import scalafx.scene.paint.{LinearGradient, Stops}
import scalafx.scene.text.Text

import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{FiniteDuration, HOURS, SECONDS}

class MonitorActor(message1: TextArea) extends Actor {

  val logger = LoggerFactory.getLogger(classOf[MonitorActor])
  import Uploader._

  val uploader: ActorRef = context.actorOf(Uploader.props, "uploader")
  uploader ! Uploader.Upload

  val timer: Cancellable = {
    val uploadTime = LocalDateTime.now.plusHours(1).withMinute(9).withSecond(0)
    val duration = Duration.between(LocalDateTime.now(), uploadTime)

    context.system.scheduler.schedule(FiniteDuration(duration.getSeconds, SECONDS),
      FiniteDuration(1, HOURS), self, Uploader.ScheduledUpload)
  }

  override def receive: Receive = {
    case ScheduledUpload =>
      val msg = s"上傳排定資料..."
      logger.info(msg)
      Platform.runLater({
        message1.appendText(msg + "\n")
      })
      uploader ! Upload
    case Upload =>
      val msg = s"上傳最近資料..."
      logger.info(msg)
      Platform.runLater({
        message1.appendText(msg + "\n")
      })
      uploader ! Upload
    case uploadRange: UploadRange =>
      val msg = s"上傳 從${uploadRange.start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}->" +
        s"${uploadRange.end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}"
      logger.info(msg)
      Platform.runLater({
        message1.appendText(msg + "\n")
      })
      uploader ! uploadRange
    case UploadResult(success, dateTime) =>
      val msg = if (success)
        s"${dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}-上傳成功"
      else
        s"${dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}-上傳失敗"
      Platform.runLater({
        message1.appendText(msg + "\n")
      })
  }
}

object ApplicationMain extends JFXApp {
  DbHelper.start()

  val message1: TextArea = new TextArea()
  message1.editable = false

  val system: ActorSystem = ActorSystem("MyActorSystem")
  val monitor: ActorRef = system.actorOf(Props(classOf[MonitorActor], message1))


  val message2: Text = new Text {
    text = ""
    style = "-fx-font-size: 16pt"
    fill = new LinearGradient(
      endX = 0,
      stops = Stops(PaleGreen, SeaGreen))
  }

  stage = new JFXApp.PrimaryStage {
    icons += new Image("/sfx.png")
    scene = new Scene() {
      title = "CDX上傳工具"
      content =
        new VBox {
          padding = Insets(10)
          spacing = 8
          children = Seq(
            new HBox {
              spacing = 8
              //padding = Insets(10)
              children = Seq(
                new Button("上傳最新資料") {
                  onAction = handle {
                    monitor ! Upload
                  }
                },
                new Button("上傳區間資料") {
                  onAction = handle {
                    onShowDateRangeDlg(false)
                  }
                },
                new Button("上傳區間資料(所有狀態)") {
                  onAction = handle {
                    onShowDateRangeDlg(true)
                  }
                }
              )
            },
            message1
          )
        }

    }
  }

  def onShowDateRangeDlg(allStatus:Boolean): Unit = {

    case class Result(start: LocalDateTime, end: LocalDateTime)

    // Create the custom dialog.
    val dialog = new Dialog[Result]() {
      initOwner(stage)
      title = "上傳區間資料"
      headerText = "上傳區間資料"
      graphic = new ImageView(this.getClass.getResource("login_icon.png").toString)
    }

    // Set the button types.
    val uploadButtonType = new ButtonType("上傳", ButtonData.OKDone)
    dialog.dialogPane().buttonTypes = Seq(uploadButtonType, ButtonType.Cancel)

    import jfxtras.scene.control._
    // Create the username and password labels and fields.
    val startDate = new LocalDateTimeTextField(LocalDateTime.now().minusDays(1).withMinute(0).withSecond(0).withNano(0))
    val endDate = new LocalDateTimeTextField(startDate.getLocalDateTime.plusDays(1))

    startDate.setMinWidth(200)

    val grid = new GridPane() {
      hgap = 10
      vgap = 10
      padding = Insets(20, 100, 10, 10)

      add(new Label("起始日期:"), 0, 0)
      add(startDate, 1, 0)
      add(new Label("結束日期:"), 0, 1)
      add(endDate, 1, 1)
    }

    // Enable/Disable login button depending on whether a username was entered.
    val uploadButton = dialog.dialogPane().lookupButton(uploadButtonType)

    // Do some validation (disable when username is empty).
    startDate.localDateTimeProperty().onChange { (_, _, newValue) =>
      if (newValue.isAfter(endDate.getLocalDateTime))
        endDate.setLocalDateTime(newValue.plusHours(1))
    }

    endDate.localDateTimeProperty().onChange { (_, _, newValue) =>
      uploadButton.disable = !newValue.isAfter(startDate.getLocalDateTime)
    }
    dialog.dialogPane().content = grid

    // Request focus on the username field by default.
    Platform.runLater(startDate.requestFocus())
    // Convert the result to a username-password-pair when the login button is clicked.
    dialog.resultConverter = dialogButton =>

      if (dialogButton == uploadButtonType)
        Result(startDate.getLocalDateTime, endDate.getLocalDateTime)
      else null

    val result = dialog.showAndWait()

    result match {
      case Some(Result(start, end)) =>
        monitor ! UploadRange(start, end, allStatus)
      case None => println("Dialog returned: None")
      case _ => println("unexpected!")
    }
  }
}