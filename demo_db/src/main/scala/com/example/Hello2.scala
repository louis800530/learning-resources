package com.example

import java.sql.{Connection, PreparedStatement, ResultSet, Statement}

import scala.util.control.NonFatal


/**
  * Created by kigi on 11/28/16.
  */
object Hello2 {

  def main(args: Array[String]): Unit = {

    initDriver()


    var conn: Connection = null
    var stmt: PreparedStatement = null
    var rs: ResultSet = null

    var stmt2: PreparedStatement = null
    var stmt3: PreparedStatement = null
    var rs3: ResultSet = null

    var stmt4: PreparedStatement = null

    var stmt5: PreparedStatement = null
    var rs5: ResultSet = null

    try {
      conn = getConnection()
      stmt = conn.prepareStatement("select id, name, created, updated from test2 where id = ?")
      stmt.setInt(1, 2)

      rs = stmt.executeQuery()


      while(rs.next()) {
        println(rs.getInt("id"))
        println(rs.getString("name"))
        println(rs.getTimestamp("created"))
        println(rs.getTimestamp("updated"))
      }

      stmt2 = conn.prepareStatement("update test2 set name = ? where id = ?")

      stmt2.setString(1, "test1111");
      stmt2.setInt(2, 3)

      val updated = stmt2.executeUpdate()

      println("update = " + updated)

      println("----------------------------")
      stmt3 = conn.prepareStatement("insert into test2 (name) values(?)", Statement.RETURN_GENERATED_KEYS)

      stmt3.setString(1, "aaaaa")
      val inserted = stmt3.executeUpdate()
      println("inserted = " + inserted)
      val rs3 = stmt3.getGeneratedKeys()

      if (rs3.next()) {
        println(s"id: ${rs3.getInt(1)}")
      }
      println("----------------------------")

      stmt5 = conn.prepareStatement("select id, name, created, updated from test2")
      rs5 = stmt5.executeQuery()

      while(rs5.next()) {
        println(rs5.getInt("id"))
        println(rs5.getString("name"))
        println(rs5.getTimestamp("created"))
        println(rs5.getTimestamp("updated"))
      }
      println("----------------------------")
      stmt4 = conn.prepareStatement("delete from test2 where name = ?")
      stmt4.setString(1, "aaaaa")
      val deleted = stmt4.executeUpdate()

      println("deleted = " + deleted)

    }
    catch {
      case NonFatal(ex) => println(ex)
    }
    finally {
      close(rs)
      close(stmt)

      close(stmt2)

      close(rs3)
      close(stmt3)

      close(stmt4)

      close(rs5)
      close(stmt5)

      close(conn)
    }

  }

}
