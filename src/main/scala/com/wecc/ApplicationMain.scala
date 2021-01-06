package com.wecc

import java.time.LocalDate

import akka.actor.ActorSystem
import scalafx.Includes._
import scalafx.application.{JFXApp, Platform}
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.ButtonBar.ButtonData
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{GridPane, HBox, VBox}
import scalafx.scene.paint.{LinearGradient, Stops}
import scalafx.scene.text.Text
import scalafx.scene.paint.Color._
import scala.concurrent.ExecutionContext.Implicits.global

object ApplicationMain extends JFXApp {

  DbHelper.start

  val system = ActorSystem("MyActorSystem")
  val uploader = system.actorOf(Uploader.props, "uploader")

  val timer = {
    import com.github.nscala_time.time.Imports._
    val nextHour = DateTime.now + 1.hour
    val uploadTime = nextHour.withMinuteOfHour(9)
    val duration = new Duration(DateTime.now(), uploadTime)

    system.scheduler.schedule(scala.concurrent.duration.Duration(duration.getStandardSeconds + 1, scala.concurrent.duration.SECONDS),
      scala.concurrent.duration.Duration(1, scala.concurrent.duration.HOURS), uploader, Uploader.Upload)
  }

  def nextUploadTime = {
    import com.github.nscala_time.time.Imports._
    val nextHour = DateTime.now + 1.hour
    val uploadTime = nextHour.withMinuteOfHour(9)
    uploadTime.toString("yyyy-MM-dd hh:mm")
  }

  val uploadMessage = new Text {
    text = ""
    style = "-fx-font-size: 16pt"
    fill = new LinearGradient(
      endX = 0,
      stops = Stops(PaleGreen, SeaGreen))
  }
  stage = new JFXApp.PrimaryStage {
    icons += new Image("/sfx.png")
    scene = new Scene(400, 150) {
      title = "CDX上傳工具"
      fill = Black
      content =
        new VBox {
          padding = Insets(10)
          spacing = 8
          children = Seq(
            new Text {
              text = s"下次上傳:${nextUploadTime}"
              style = "-fx-font-size: 16pt"
              fill = new LinearGradient(
                endX = 0,
                stops = Stops(PaleGreen, SeaGreen))
            },
            new VBox {
              spacing = 8
              //padding = Insets(10)
              children = Seq(
                new Button("上傳區間資料") {
                  onAction = handle {
                    onShowDateRangeDlg()
                  }
                },
                uploadMessage
              )
            }
          )
        }

    }
  }

  def onShowDateRangeDlg(): Unit = {

    case class Result(start: LocalDate, end: LocalDate)

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

    // Create the username and password labels and fields.
    val startDate = new DatePicker()
    val endDate = new DatePicker()

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
    uploadButton.disable = true

    // Do some validation (disable when username is empty).
    startDate.value.onChange{ (_, _, newValue) =>
      endDate.value = newValue}

    endDate.value.onChange{(_,_, newValue)=>
      uploadButton.disable = !newValue.isAfter(startDate.value())
    }
    dialog.dialogPane().content = grid

    // Request focus on the username field by default.
    Platform.runLater(startDate.requestFocus())

    // Convert the result to a username-password-pair when the login button is clicked.
    dialog.resultConverter = dialogButton =>
      if (dialogButton == uploadButtonType) Result(startDate.value(), endDate.value())
      else null

    val result = dialog.showAndWait()

    result match {
      case Some(Result(u, p)) => println("start=" + u + ", end=" + p)
      case None               => println("Dialog returned: None")
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