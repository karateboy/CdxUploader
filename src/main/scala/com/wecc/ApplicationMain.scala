package com.wecc

import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.nscala_time.time.Imports._

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