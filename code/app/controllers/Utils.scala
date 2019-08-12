package controllers

import java.awt.Image
import java.nio.file.attribute.PosixFilePermission._
import java.nio.file.{Files, Path}

import scala.collection.JavaConversions._

object Utils {
  
  def ensureImageServerReadPermissions( filePath:Path ): Unit = {
    Files.setPosixFilePermissions(filePath, Set(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE, OTHERS_READ))
  }
  
//  def resizeImage( img: Image, width:Int, height: Int ): Image = {
//
//  }
//
}
