package com.example

import java.sql.{Connection, PreparedStatement, ResultSet, Statement}

import scala.util.control.NonFatal

import scala.io.StdIn._

/**
  * Created by louis on 2016/11/30.
  */

object DisplayStudentsData {

  def passed(conn: Connection) = {
    var stmtS: PreparedStatement = null
    var rsS: ResultSet = null

    stmtS = conn.prepareStatement("select s.id, s.name, sr.exam1, sr.exam2, sr.exam3, sr.avg " +
      "from students s left join scores sr " +
      "on s.id = sr.student_id where sr.avg >= 60")
    rsS = stmtS.executeQuery()

    println("以下為及格名單")

    while (rsS.next()) {
      println(rsS.getInt("s.id") + "  " +
        "name: " + rsS.getString("s.name") + "  " +
        "exam1: " + rsS.getFloat("sr.exam1") + "  " +
        "exam2: " + rsS.getFloat("sr.exam2") + "  " +
        "exam3: " + rsS.getFloat("sr.exam3") + "  " +
        "avg: " + rsS.getFloat("sr.avg"))
    }
    close(rsS)
    close(stmtS)
  }

  def failed(conn: Connection) = {
    var stmtS: PreparedStatement = null
    var rsS: ResultSet = null

    stmtS = conn.prepareStatement("select s.id, s.name, sr.exam1, sr.exam2, sr.exam3, sr.avg " +
      "from students s left join scores sr " +
      "on s.id = sr.student_id where sr.avg < 60 or sr.avg is null")
    rsS = stmtS.executeQuery()

    println("以下為補考名單")
    while (rsS.next()) {
      if (rsS.getFloat("sr.avg") < 60) {
        if (rsS.getString("sr.avg") == null)
          println(rsS.getInt("s.id") + " name: " + rsS.getString("s.name") + "  缺考")
        else
          println(rsS.getInt("s.id") + "  " +
            "name: " + rsS.getString("s.name") + "  " +
            "exam1: " + rsS.getFloat("sr.exam1") + "  " +
            "exam2: " + rsS.getFloat("sr.exam2") + "  " +
            "exam3: " + rsS.getFloat("sr.exam3") + "  " +
            "avg: " + rsS.getFloat("sr.avg"))
      }
    }
    close(rsS)
    close(stmtS)
  }
}

object GradesSystem {

  def GetScoresInput(line: String): (Float, Float, Float, Float) = {

    val scores = line.split(" ")

    val exam1 = scores(0).toFloat
    val exam2 = scores(1).toFloat
    val exam3 = scores(2).toFloat
    val avg = (exam1 + exam2 + exam3) / 3

    (exam1, exam2, exam3, avg)
  }

  def ConstructData(conn: Connection, name: String, exam1: Float, exam2: Float, exam3: Float, avg: Float) = {
    var stmtPI: PreparedStatement = null
    var stmtSI: PreparedStatement = null

    stmtPI = conn.prepareStatement("insert into students (name) values(?)", Statement.RETURN_GENERATED_KEYS)
    stmtPI.setString(1, name)

    stmtPI.executeUpdate()
    val rsPK = stmtPI.getGeneratedKeys()

    if (rsPK.next()) {
      val PK = rsPK.getInt(1)
      stmtSI = conn.prepareStatement("insert into scores (student_id, exam1, exam2, exam3, avg) values(?, ?, ?, ?, ?)")
      stmtSI.setInt(1, PK)
      stmtSI.setFloat(2, exam1)
      stmtSI.setFloat(3, exam2)
      stmtSI.setFloat(4, exam3)
      stmtSI.setFloat(5, avg)

      stmtSI.executeUpdate()
      println(s"id: $PK")
    }
    close(rsPK)
    close(stmtPI)
    close(stmtSI)
  }

  def RenewData(conn: Connection, studentId: Int, exam1: Float, exam2: Float, exam3: Float, avg: Float) = {
    var stmtSS: PreparedStatement = null
    var stmtSU: PreparedStatement = null
    var stmtSI: PreparedStatement = null

    stmtSS = conn.prepareStatement("select avg from scores where student_id = ?")
    stmtSS.setInt(1, studentId)
    val rsSS = stmtSS.executeQuery()
    if (rsSS.next()) {
      stmtSU = conn.prepareStatement("update scores set exam1 = ?, exam2 = ?, exam3 = ?, avg =? where student_id = ?")

      stmtSU.setFloat(1, exam1)
      stmtSU.setFloat(2, exam2)
      stmtSU.setFloat(3, exam3)
      stmtSU.setFloat(4, avg)
      stmtSU.setInt(5, studentId)

      stmtSU.executeUpdate()
      close(stmtSU)
    }
    else {
      stmtSI = conn.prepareStatement("insert into scores(student_id, exam1, exam2, exam3, avg) values(?, ?, ?, ?, ?)")

      stmtSI.setInt(1, studentId)
      stmtSI.setFloat(2, exam1)
      stmtSI.setFloat(3, exam2)
      stmtSI.setFloat(4, exam3)
      stmtSI.setFloat(5, avg)

      stmtSI.executeUpdate()
    }
    close(rsSS)
    close(stmtSS)

    close(stmtSU)
    close(stmtSI)
  }

  def main(args: Array[String]): Unit = {

    initDriver()

    var conn: Connection = null

    var stmtP: PreparedStatement = null
    var rsP: ResultSet = null

    var nameString: String = ""

    try {
      conn = getConnection()

      println("輸入學生姓名")
      nameString = readLine()

      stmtP = conn.prepareStatement("select id, name from students where name = ?")
      stmtP.setString(1, nameString)
      rsP = stmtP.executeQuery()

      if (rsP.next()) {
        println("資料庫中有該學生資料，是否更新(Y/N)")
        if (readChar() == 'Y') {
          println("exam1  exam2  exam3")
          val scores = GetScoresInput(readLine())

          val index = rsP.getInt("id")

          RenewData(conn, index, scores._1, scores._2, scores._3, scores._4)
        }
      }
      else {
        println("資料庫中無該學生資料，是否新增(Y/N)")
        if (readChar() == 'Y') {
          println("exam1  exam2  exam3")
          val scores = GetScoresInput(readLine())

          ConstructData(conn, nameString, scores._1, scores._2, scores._3, scores._4)
        }
      }

      DisplayStudentsData.passed(conn)

      DisplayStudentsData.failed(conn)
    }
    catch {
      case NonFatal(ex) => println(ex)
    }
    finally {
      close(rsP)
      close(stmtP)

      close(conn)
    }

  }
}
