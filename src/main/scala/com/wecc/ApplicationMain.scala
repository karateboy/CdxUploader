package com.wecc

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}
import com.wecc.Uploader.{Upload, UploadRange}
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

class MonitorActor(message1: TextArea) extends Actor {

  import Uploader._

  val uploader: ActorRef = context.actorOf(Uploader.props, "uploader")
  uploader ! Uploader.Upload

  val timer: Cancellable = {
    val uploadTime = LocalDateTime.now.plusHours(1).withMinute(7)
    val duration = Duration.between(LocalDateTime.now(), uploadTime)

    context.system.scheduler.schedule(scala.concurrent.duration.Duration(duration.getSeconds + 1, scala.concurrent.duration.SECONDS),
      scala.concurrent.duration.Duration(1, scala.concurrent.duration.HOURS), self, Uploader.ScheduledUpload)
  }

  override def receive: Receive = {
    case ScheduledUpload =>
      Platform.runLater({
        message1.appendText(s"上傳最近資料...\n")
      })
      uploader ! Upload
    case Upload =>
      Platform.runLater({
        message1.appendText(s"上傳最近資料...\n")
      })
      uploader ! Upload
    case uploadRange: UploadRange =>
      Platform.runLater({
        message1.appendText(s"上傳 從${uploadRange.start.format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm"))}->" +
          s"${uploadRange.end.format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm"))}\n")
      })
      uploader ! uploadRange
    case UploadResult(success, dateTime) =>
      Platform.runLater({
        if (success)
          message1.appendText(s"${dateTime.format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm"))}-上傳成功\n")
        else
          message1.appendText(s"${dateTime.format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm"))}-上傳失敗\n")
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
    }
  }
}

/*
object ApplicationMain extends App {
  DbHelper.start

  val system = ActorSystem("MyActorSystem")
  val uploader = system.actorOf(Uploader.props, "uploader")
  if (args.length == 0) {
    val timer = {
      import com.github.nscala_time.time.Imports._
      val nextHour = DateTime.now + 1.hour
      val uploadTime = nextHour.withMinuteOfHour(9)
      val duration = new Duration(DateTime.now(), uploadTime)

      system.scheduler.schedule(scala.concurrent.duration.Duration(duration.getStandardSeconds + 1, scala.concurrent.duration.SECONDS),
        scala.concurrent.duration.Duration(1, scala.concurrent.duration.HOURS), uploader, Uploader.Upload)

    }
    uploader ! Uploader.Upload
  }else if(args.length == 2){
    assert(args(0).equalsIgnoreCase("upload"))
    val dataTime = DateTime.parse(args(1), DateTimeFormat.forPattern("YYYY-MM-dd HH:mm"))
    uploader ! Uploader.UploadData(dataTime)
    Console.println(s"Uploading ${dataTime} data")    
  }else if(args.length == 3){
    assert(args(0).equalsIgnoreCase("upload"))
    val start = DateTime.parse(args(1), DateTimeFormat.forPattern("YYYY-MM-dd HH:mm"))
    val end = DateTime.parse(args(2), DateTimeFormat.forPattern("YYYY-MM-dd HH:mm"))
    
    uploader ! Uploader.UploadRange(start, end)
  }
  system.awaitTermination()
}

 */