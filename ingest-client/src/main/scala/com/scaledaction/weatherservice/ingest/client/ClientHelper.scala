package com.scaledaction.weatherservice.ingest.client

import java.io.{BufferedInputStream, FileInputStream, File => JFile}
import java.util.zip.GZIPInputStream
import scala.util.Try
import com.typesafe.config.{ Config, ConfigFactory }

private[client] trait ClientHelper {
    import Sources._
    
    private val config = ConfigFactory.load
    protected val DefaultPath = config.getString("weatherservice.data.load.path")
    protected val DefaultExtension = 
        config.getString("weatherservice.data.file.extension")

    protected val ingestData: Set[FileSource] = 
        new JFile(DefaultPath).list.collect {
        
        case name if name.endsWith(DefaultExtension) =>
            println("<1>ClientHelper.ingestData name: " + name)
            FileSource(new JFile(s"$DefaultPath/$name"))
    }.toSet
}

private[client] object Sources {

    case class FileSource(data: Array[String], name: String) {
        //def days: Seq[Day] = data.map(Day(_)).toSeq
    }
    object FileSource {
        def apply(file: JFile): FileSource = {
            println("<2>Sources FileSource.apply file: " + file)
            val src = file match {
                case f if f.getAbsolutePath endsWith ".gz" =>
                    scala.io.Source.fromInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))), "utf-8")
                case f =>
                    scala.io.Source.fromFile(file, "utf-8")
            }
            val read = src.getLines.toList
            Try(src.close())
            FileSource(read.toArray, file.getName)
        }
    }
}