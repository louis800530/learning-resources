package com.example

import com.sun.javafx.scene.control.skin.EmbeddedTextContextMenuContent


sealed abstract class MaxHeapTree {
  def value: Option[Double]

  def isEmpty: Boolean

  def height: Int

  def +(value: Double): MaxHeapTree = this match {
    case Empty => Node(value, Empty, Empty)
    case Node(value, left, right) => MaxHeapTree(this, value)
  }
}


case object Empty extends MaxHeapTree {
  override def value: Option[Double] = None

  override def isEmpty: Boolean = true
  override def height: Int = 0
}

case class Node(nodeValue: Double, left: MaxHeapTree, right:MaxHeapTree) extends MaxHeapTree{
  override def value = Some(nodeValue)

  override def isEmpty: Boolean = false

  override def height = 1 + Math.max(left.height, right.height)
}

object MaxHeapTree {
  def travel(tree: MaxHeapTree): Unit = tree match {
    case Empty => println("Done!!")
    case Node(value, left, right) => {
      println(value)
      travel(left)
      travel(right)
    }
  }

  def apply(tree: MaxHeapTree, value: Double) = addPoint(tree, value)

  def addPoint(tree: MaxHeapTree, value: Double): MaxHeapTree = tree match {
    case Empty => Node(value, Empty, Empty)
    case Node(nodeValue, left, right) => {
      if (value > nodeValue) {
        if (right.height <= left.height) Node(value, left, addPoint(right, nodeValue))
        else Node(value, addPoint(left, nodeValue), right)
      }
      else {
        if (right.height <= left.height) Node(nodeValue, left, addPoint(right, value))
        else Node(nodeValue, addPoint(left, value), right)
      }
    }
  }

  def pop2(tree: MaxHeapTree): MaxHeapTree = tree match {
    case Empty => Empty

    case Node(value, left, right) =>
      (for (leftValue <- left.value;
           rightValue <- right.value) yield {

        if (leftValue >= rightValue) Node(leftValue, pop2(left), right)
        else Node(rightValue, left, pop2(right))

      }) getOrElse {

        if (left.isEmpty && right.isEmpty) {
          Empty
        } else if (left.isEmpty) {
          Node(right.value.get, Empty, pop2(right))
        } else {
          Node(left.value.get, pop2(left), Empty)
        }
      }

  }

  def pop(tree: MaxHeapTree): (Option[Double], MaxHeapTree) = tree match {
    case Empty => (None, Empty)
    case Node(value, left, right) => {
      (Option(value), pop2(tree))
    }
  }
}


/**
  * Created by louis on 2016/11/29.
  */
object TestTree {
  def main(args: Array[String]): Unit = {

  }
}
