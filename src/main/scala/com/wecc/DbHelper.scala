package com.wecc
import scalikejdbc._
import scalikejdbc.config._
import scala.language.implicitConversions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.github.nscala_time.time.Imports._

case class HourRecord(station: Int, dateTime: DateTime, itemId: Int, value: Option[Float]){
  import scala.xml._
    /*
     * 測站代碼(SiteId)、測站名稱(SiteName)、縣市(County)、測項代碼(ItemId)、測項名稱(ItemName)、測項英文名稱(ItemEngName)、測項單位(ItemUnit)、監測日期(MonitorDate)、數值(Concentration)。
     */

  def toXML = {
    val map = DbHelper.itemIdMap(itemId)
    val dateTimeStr = dateTime.toString("YYYY-MM-dd HH:mm") 

    <HourRecord>
			<SiteId>{station}</SiteId>
			<SiteName>三峽測站</SiteName>
			<County>新北市</County>	
			<ItemId>{map.epaId}</ItemId>
			<ItemEngName>{map.itemCode}</ItemEngName>
			<ItemUnit>{map.unit}</ItemUnit>
			<MonitorDate>{dateTimeStr}</MonitorDate>
			{
			  if(value.isDefined){
			    <Concentration>{value.get}</Concentration>
			  }else{
			    <Concentration>-</Concentration>
			  }
			}
		</HourRecord>
  }
}
case class ItemIdMap(epaId: Int, itemName: String, itemCode: String, unit: String)

object DbHelper {
  val logger = LoggerFactory.getLogger(DbHelper.getClass)

  implicit def getSqlTimestamp(t: DateTime) = {
    new java.sql.Timestamp(t.getMillis)
  }

  implicit def getDateTime(st: java.sql.Timestamp) = {
    new DateTime(st)
  }

  def start = {
    DBs.setupAll()

    //val hour = DateTime.parse("2016-6-29 10:00")
    //getHourRecord(hour)
  }

  def getHourRecord(hour: DateTime)(implicit session: DBSession = AutoSession) = {
    import java.sql.Time
    val hourTime: java.sql.Timestamp = hour

    sql"""
      Select *
      From hour_data
      Where MStation = 90 and MDate = $hourTime
      """.map { rs =>
      val dateTime = rs.timestamp("MDate")
      val mStatus = rs.string("MStatus")
      val mValue = if (mStatus == "N")
        Some(rs.float("MValue"))
      else
        None

      HourRecord(rs.int("MStation"), dateTime, rs.int("MItem"), mValue)
    }.list.apply
  }

  val itemIdMap = Map(
    1 -> ItemIdMap(1, "二氧化硫", "SO2", "ppb"),
    2 -> ItemIdMap(5, "氮氧化物", "	NOx", "ppb"),
    3 -> ItemIdMap(7, "一氧化氮", "NO", "ppb"),
    4 -> ItemIdMap(6, "二氧化氮", "NO2", "ppb"),
    5 -> ItemIdMap(2, "一氧化碳", "CO", "ppm"),
    6 -> ItemIdMap(3, "臭氧", "O3", "ppb"),
    7 -> ItemIdMap(31, "甲烷", "CH4", "ppm"),
    8 -> ItemIdMap(9, "非甲烷", "NMHC", "ppm"),
    9 -> ItemIdMap(8, "總碳氫", "THC", "ppm"),
    10 -> ItemIdMap(4, "懸浮微粒", "PM10", "ug/m3"),
    11 -> ItemIdMap(33, "細懸浮微粒", "PM2.5", "ug/m3"),
    12 -> ItemIdMap(10, "風速", "WS", "m/2"),
    13 -> ItemIdMap(11, "風向", "WD", "Deg"),
    14 -> ItemIdMap(14, "溫度", "TEM", "Deg"),
    15 -> ItemIdMap(38, "溼度", "HUM", "%"),
    16 -> ItemIdMap(15, "大氣壓力", "Press", "atm"),
    17 -> ItemIdMap(23, "雨量", "RF", "mm"),
    18 -> ItemIdMap(14, "室內溫度", "TEM", "deg"))

  def getCsvRecord(hour:DateTime):String={
    val hrList = DbHelper.getHourRecord(hour)
    getCsvRecord(hrList)
  }
  
  def getXmlRecord(hour:DateTime)={
    val hrList = DbHelper.getHourRecord(hour)
    getXml(hrList)
  }
  
  def getCsvRecord(hrList: List[HourRecord]) = {
    /*
     * 測站代碼(SiteId)、測站名稱(SiteName)、縣市(County)、測項代碼(ItemId)、測項名稱(ItemName)、測項英文名稱(ItemEngName)、測項單位(ItemUnit)、監測日期(MonitorDate)、數值(Concentration)。
     */
    val header = "測站代碼(SiteId),測站名稱(SiteName),縣市(County),測項代碼(ItemId),測項名稱(ItemName),測項英文名稱(ItemEngName),測項單位(ItemUnit),監測日期(MonitorDate),數值(Concentration),\r"

    val csvList = hrList map { hr =>
      val map = itemIdMap(hr.itemId)
      val dateTimeStr = hr.dateTime.toString("YYYY-MM-dd HH:mm") 
      s"90,三峽測站,新北市,${map.epaId},${map.itemName},${map.itemCode},${map.unit},${dateTimeStr},${hr.value.getOrElse("-")},"
    }
    
    header + csvList.mkString("\r")    
  }
  
  def getXml(hrList:List[HourRecord]) = {
    import scala.xml._
    val xmlList = hrList.map { _.toXML }
    
    <AQX_P_274_Data><Data>{xmlList}</Data></AQX_P_274_Data>
  }
}