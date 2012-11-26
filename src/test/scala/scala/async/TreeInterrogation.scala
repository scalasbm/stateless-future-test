/*
 * Copyright (C) 2012 Typesafe Inc. <http://www.typesafe.com>
 */

package scala.async

import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.Test
import AsyncId._

@RunWith(classOf[JUnit4])
class TreeInterrogation {
  @Test
  def `a minimal set of vals are lifted to vars`() {
    val cm = reflect.runtime.currentMirror
    val tb = mkToolbox("-cp target/scala-2.10/classes")
    val tree = tb.parse(
      """| import _root_.scala.async.AsyncId._
        | async {
        |   val x = await(1)
        |   val y = x * 2
        |   val z = await(x * 3)
        |   z
        | }""".stripMargin)
    val tree1 = tb.typeCheck(tree)

    //println(cm.universe.show(tree1))

    import tb.mirror.universe._
    val functions = tree1.collect {
      case f: Function => f
      case t: Template => t
    }
    functions.size mustBe 1

    val varDefs = tree1.collect {
      case ValDef(mods, name, _, _) if mods.hasFlag(Flag.MUTABLE) => name
    }
    varDefs.map(_.decoded.trim).toSet mustBe (Set("state$async", "await$1", "await$2"))
  }
}

object TreeInterrogation extends App {
  def withDebug[T](t: => T) {
    AsyncUtils.trace = true
    AsyncUtils.verbose = true
    try t
    finally {
      AsyncUtils.trace = false
      AsyncUtils.verbose = false
    }
  }

  withDebug {
    val cm = reflect.runtime.currentMirror
    val tb = mkToolbox("-cp target/scala-2.10/classes -Xprint:all")
    val tree = tb.parse(
      """ import _root_.scala.async.AsyncId._
        | val state = 23
        | val result: Any = "result"
        | def resume(): Any = "resume"
        | val res = async {
        |   val f1 = async { state + 2 }
        |   val x  = await(f1)
        |   val y  = await(async { result })
        |   val z  = await(async { resume() })
        |   (x, y, z)
        | }
        | ()
        | """.stripMargin)
    println(tree)
    val tree1 = tb.typeCheck(tree.duplicate)
    println(cm.universe.show(tree1))
    println(tb.eval(tree))
  }
}
