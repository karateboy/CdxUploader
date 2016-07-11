package com.wecc

import akka.actor.{ Actor, ActorLogging, Props }
import javax.xml.ws.Holder

object Uploader {
  val props = Props[Uploader]
  case object Upload
}

class Uploader extends Actor with ActorLogging {
  import Uploader._
  import com.github.nscala_time.time.Imports._

  def receive = {
    case Upload =>
      try {
        val lastHour = DateTime.now().withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0) - 1.hour
        log.info(s"Upload ${lastHour.toString()}")
        //val testHour = DateTime.parse("2016-06-29 0:0", DateTimeFormat.forPattern("YYYY-MM-dd HH:mm"))
        val result = upload(lastHour, "AQX_P_274", "epbntcair", "wfuviFJf")
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

  def upload(hour: DateTime, serviceId: String, user: String, password: String) = {
    val base64 = getBase64XmlStr(hour)
    val fileName = s"${serviceId}_${hour.toString("MMdd")}_${hour.hour}.xml"
    val errMsgHolder = new Holder("")
    val resultHolder = new Holder(Integer.valueOf(0))
    val unknownHolder = new Holder(new java.lang.Boolean(true))
    CdxWebService.service.putFile(user, password, fileName, base64, errMsgHolder, resultHolder, unknownHolder)
    if (resultHolder.value != 1) {
      log.error(s"errMsg:${errMsgHolder.value}")
      log.error(s"ret:${resultHolder.value.toString}")
      log.error(s"unknown:${unknownHolder.value.toString}")
    } else {
      log.info(s"Success upload ${hour.toString}")
    }
  }
}