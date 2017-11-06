package com.wecc

import akka.actor.{ Actor, ActorLogging, Props }
import javax.xml.ws.Holder
import com.github.nscala_time.time.Imports._
import java.io.FileOutputStream

object Uploader {
  val props = Props[Uploader]
  case object Upload
  case class UploadData(time: DateTime)
  case class UploadRange(start:DateTime, end:DateTime)
}

class Uploader extends Actor with ActorLogging {
  import Uploader._
  import com.github.nscala_time.time.Imports._

  def getPeriodHours(start:DateTime, end:DateTime)={
    var hours = Vector.empty[DateTime]
    var current = start
    while(current < end){
      hours = hours :+(current)
      current += 1.hour
    }
    hours
  }
  
  def receive = {
    case Upload =>
      try {
        val lastHour = DateTime.now().withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0) - 1.hour
        log.info(s"Upload ${lastHour.toString()}")
        val result = upload(lastHour, "AQX_S_00", "epbntcair", "wfuviFJf")
      } catch {
        case ex: Throwable =>
          log.error(ex, "upload failed")
      }
    case UploadData(time) =>
      try {
        log.info(s"Upload ${time.toString()}")
        val result = upload(time, "AQX_S_00", "epbntcair", "wfuviFJf")
      } catch {
        case ex: Throwable =>
          log.error(ex, "upload failed")
      }
    case UploadRange(start, end)=>
      try {       
        log.info(s"上傳自 ${start.toLocalDateTime.toString} to ${end.toLocalDateTime.toString}")
        for(time <- getPeriodHours(start,end))
          upload(time, "AQX_S_00", "epbntcair", "wfuviFJf")
          
        log.info("Done!")
      } catch {
        case ex: Throwable =>
          log.error(ex, "upload failed")
      }
  }

  def getBase64XmlStr(hour: DateTime) = {
    val xml = DbHelper.getXmlRecord(hour)

    scala.xml.XML.save("temp.xml", xml, "UTF-8", true)

    val xmlStr = scala.io.Source.fromFile("temp.xml")("UTF-8").mkString
    val encoder = java.util.Base64.getEncoder
    encoder.encode(xmlStr.getBytes("UTF-8"))
  }

  def getXmlStr(hour: DateTime) = {
    val xml = DbHelper.getXmlRecord(hour)

    scala.xml.XML.save("temp.xml", xml, "UTF-8", true)

    scala.io.Source.fromFile("temp.xml")("UTF-8").mkString
  }

  def saveCSV(hour: DateTime) = {
    val csv = DbHelper.getCsvRecord(hour)
    val bytes = csv.getBytes
    val out = new FileOutputStream("aqm.csv")
    out.write(bytes)
    out.close()

    import java.io.File
    import java.nio.file.Files
    import java.nio.file.StandardCopyOption
    val src = new File("aqm.csv")
    val dest = new File("C:/inetpub/wwwroot/aqm.csv")
    Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
  }

  def upload(hour: DateTime, serviceId: String, user: String, password: String) = {
    val xmlStr = getXmlStr(hour)
    saveCSV(hour)
    val fileName = s"${serviceId}_${hour.toString("MMdd")}${hour.getHourOfDay}_${user}.xml"
    val errMsgHolder = new Holder("")
    val resultHolder = new Holder(Integer.valueOf(0))
    val unknownHolder = new Holder(new java.lang.Boolean(true))
    CdxWebService.service.putFile(user, password, fileName, xmlStr.getBytes("UTF-8"), errMsgHolder, resultHolder, unknownHolder)
    if (resultHolder.value != 1) {
      log.error(s"errMsg:${errMsgHolder.value}")
      log.error(s"ret:${resultHolder.value.toString}")
      log.error(s"unknown:${unknownHolder.value.toString}")
    } else {
      log.info(s"Success upload ${hour.toString}")
    }
  }
}