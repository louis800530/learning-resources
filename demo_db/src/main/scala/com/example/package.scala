package com

import java.sql.{Connection, DriverManager}

import scala.util.control.NonFatal

/**
  * Created by kigi on 11/28/16.
  */
package object example {

  def initDriver()  {
    Class.forName("com.mysql.jdbc.Driver").newInstance()
  }

  def getConnection(): Connection = {
    // jdbc:mysql://database_host/database_name?user=account&password=password
    //DriverManager.getConnection("jdbc:mysql://192.168.1.53/test2?user=e7life&password=e7life17")
    DriverManager.getConnection("jdbc:mysql://localhost/grades?user=louis&password=d3210805466")
  }


  def close(resource: AutoCloseable): Unit = {
    if (resource != null) {
      try { resource.close() }
      catch { case NonFatal(ex) => }
    }
  }


}
