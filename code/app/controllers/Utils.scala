package controllers

import java.nio.file.attribute.PosixFilePermission._
import java.nio.file.{Files, Path}

import scala.jdk.CollectionConverters._

object Utils {
  
  def ensureImageServerReadPermissions( filePath:Path ): Unit = {
    Files.setPosixFilePermissions(filePath, Set(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE, OTHERS_READ).asJava)
  }
  
}
