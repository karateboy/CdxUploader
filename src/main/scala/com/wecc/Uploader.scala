package com.wecc

import akka.actor.{Actor, ActorLogging, Props}

import java.io.FileOutputStream
import java.nio.file.Path
import javax.xml.ws.Holder
import scala.collection.immutable
import com.typesafe.config._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Uploader {
  val props: Props = Props[Uploader]

  case object ScheduledUpload

  case object Upload

  case class UploadData(time: LocalDateTime)

  case class UploadRange(start: LocalDateTime, end: LocalDateTime, allStatus:Boolean)

  case class UploadResult(success: Boolean, dateTime: LocalDateTime)

}

class Uploader extends Actor with ActorLogging {

  import Uploader._

  val dryRun = ConfigFactory.load().getBoolean("dryRun")
  log.info(s"dryRun=$dryRun")

  def getPeriodHours(start: LocalDateTime, end: LocalDateTime): immutable.Seq[LocalDateTime] = {
    var hours = Vector.empty[LocalDateTime]
    var current = start.withMinute(0).withSecond(0).withNano(0)
    while (current.isBefore(end)) {
      hours = hours :+ current
      current = current.plusHours(1)
    }
    hours
  }

  def receive: Receive = {
    case Upload =>
      try {
        val lastHour = LocalDateTime.now().withMinute(0).withSecond(0).minusHours(1).withNano(0)
        log.info(s"Upload ${lastHour.toString()}")
        upload(lastHour, "AQX_S_00", "epbntcair", "wfuviFJf")
      } catch {
        case ex: Throwable =>
          log.error(ex, "upload failed")
      }
    case UploadData(time) =>
      try {
        log.info(s"Upload ${time.toString()}")
        upload(time, "AQX_S_00", "epbntcair", "wfuviFJf")
      } catch {
        case ex: Throwable =>
          log.error(ex, "upload failed")
      }
    case UploadRange(start, end, allStatus) =>
      try {
        log.info(s"上傳從 ${start.toString} 至 ${end.toString}")
        for (time <- getPeriodHours(start, end))
          upload(time, "AQX_S_00", "epbntcair", "wfuviFJf", allStatus)

        log.info("Done!")
      } catch {
        case ex: Throwable =>
          log.error(ex, "upload failed")
      }
  }

  def getXmlStr(hour: LocalDateTime, allStatus:Boolean): String = {
    val xml = DbHelper.getXmlRecord(hour, allStatus)

    scala.xml.XML.save("temp.xml", xml, "UTF-8", xmlDecl = true)

    scala.io.Source.fromFile("temp.xml")("UTF-8").mkString
  }

  def saveCSV(hour: LocalDateTime, allStatus:Boolean) {
    import java.nio.charset.Charset
    val csv = DbHelper.getCsvRecord(hour, allStatus)
    val bytes = csv.getBytes(Charset.forName("UTF-8"))
    val out = new FileOutputStream("aqm.csv")
    out.write(bytes)
    out.close()

    import java.io.File
    import java.nio.file.{Files, StandardCopyOption}
    val src = new File("aqm.csv")
    val dest = new File("C:/inetpub/wwwroot/aqm.csv")
    if (!dryRun)
      Files.copy(src.toPath, dest.toPath, StandardCopyOption.REPLACE_EXISTING)
  }

  def upload(hour: LocalDateTime, serviceId: String, user: String, password: String, allStatus:Boolean = false): Unit = {
    val xmlStr = getXmlStr(hour, allStatus)
    saveCSV(hour, allStatus)
    val fileName = s"${serviceId}_${hour.format(DateTimeFormatter.ofPattern("MMdd"))}${hour.getHour}_$user.xml"
    val errMsgHolder = new Holder("")
    val resultHolder = new Holder(Integer.valueOf(0))
    val unknownHolder = new Holder(new java.lang.Boolean(true))

    if (!dryRun) {
      CdxWebService.service.putFile(user, password, fileName, xmlStr.getBytes("UTF-8"), errMsgHolder, resultHolder, unknownHolder)
      if (resultHolder.value != 1) {
        log.error(s"errMsg:${errMsgHolder.value}")
        log.error(s"ret:${resultHolder.value.toString}")
        log.error(s"unknown:${unknownHolder.value.toString}")
        sender ! UploadResult(false, hour)
      } else {
        log.info(s"Success upload ${hour.toString}")
        sender ! UploadResult(true, hour)
      }
    } else {
      sender ! UploadResult(true, hour)
    }
  }
}