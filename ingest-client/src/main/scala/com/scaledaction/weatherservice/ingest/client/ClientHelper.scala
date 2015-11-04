package com.scaledaction.weatherservice.ingest.client

import java.io.{BufferedInputStream, FileInputStream, File => JFile}
import java.util.zip.GZIPInputStream
import scala.util.Try

object ClientHelper {

    case class FileSource(data: Array[String], name: String) {
        //def days: Seq[Day] = data.map(Day(_)).toSeq
    }
    object FileSource {
        def apply(file: JFile): FileSource = {
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