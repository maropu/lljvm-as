/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package maropu.lljvm

import java.lang.{Double => jDouble, Float => jFloat, Integer => jInt, Long => jLong}
import java.lang.reflect.InvocationTargetException

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import maropu.lljvm.unsafe.Platform
import maropu.lljvm.util.PyArrayHolder


class PyFuncSuite extends FunSuite with BeforeAndAfterAll {

  private val basePath = "pyfunc"

  test("add") {
    TestUtils.doTest2(
      bitcode = s"$basePath/add_test-cfunc-int32.bc",
      source = s"$basePath/add_test.py",
      argTypes = Seq(jInt.TYPE, jInt.TYPE),
      arguments = Seq(new jInt(4), new jInt(5)),
      expected = Some(9)
    )
    TestUtils.doTest2(
      bitcode = s"$basePath/add_test-cfunc-int64.bc",
      source = s"$basePath/add_test.py",
      argTypes = Seq(jLong.TYPE, jLong.TYPE),
      arguments = Seq(new jLong(3), new jLong(2)),
      expected = Some(5)
    )
    TestUtils.doTest2(
      bitcode = s"$basePath/add_test-cfunc-float32.bc",
      source = s"$basePath/add_test.py",
      argTypes = Seq(jFloat.TYPE, jFloat.TYPE),
      arguments = Seq(new jFloat(1.0f), new jFloat(7.0f)),
      expected = Some(8.0f)
    )
    TestUtils.doTest2(
      bitcode = s"$basePath/add_test-cfunc-float64.bc",
      source = s"$basePath/add_test.py",
      argTypes = Seq(jDouble.TYPE, jDouble.TYPE),
      arguments = Seq(new jDouble(2.0), new jDouble(2.0)),
      expected = Some(4.0)
    )
  }

  test("pow") {
    TestUtils.doTest2(
      bitcode = s"$basePath/math_pow_test-cfunc-float32.bc",
      source = s"$basePath/math_pow_test.py",
      argTypes = Seq(jFloat.TYPE, jFloat.TYPE),
      arguments = Seq(new jFloat(1.0f), new jFloat(4.0f)),
      expected = Some(1.0f)
    )
    TestUtils.doTest2(
      bitcode = s"$basePath/math_pow_test-cfunc-float64.bc",
      source = s"$basePath/math_pow_test.py",
      argTypes = Seq(jDouble.TYPE, jDouble.TYPE),
      arguments = Seq(new jDouble(3.0), new jDouble(2.0)),
      expected = Some(9.0)
    )
  }

  test("log10") {
    TestUtils.doTest2(
      bitcode = s"$basePath/math_log10_test-cfunc-float32.bc",
      source = s"$basePath/math_log10_test.py",
      argTypes = Seq(jFloat.TYPE, jFloat.TYPE),
      arguments = Seq(new jFloat(100.0f), new jFloat(1.0f)),
      expected = Some(4.0f)
    )
    TestUtils.doTest2(
      bitcode = s"$basePath/math_log10_test-cfunc-float64.bc",
      source = s"$basePath/math_log10_test.py",
      argTypes = Seq(jDouble.TYPE, jDouble.TYPE),
      arguments = Seq(new jDouble(1000.0), new jDouble(3.0)),
      expected = Some(9.0)
    )
  }

  test("if") {
    TestUtils.doTest2(
      bitcode = s"$basePath/if1_test-cfunc-int32.bc",
      source = s"$basePath/if1_test.py",
      argTypes = Seq(jInt.TYPE),
      arguments = Seq(new jInt(1)),
      expected = Some(0)
    )
    TestUtils.doTest2(
      bitcode = s"$basePath/if2_test-cfunc-int32.bc",
      source = s"$basePath/if2_test.py",
      argTypes = Seq(jInt.TYPE),
      arguments = Seq(new jInt(0)),
      expected = Some(1)
    )
  }

  private var pyArray1: PyArrayHolder = _
  private var pyArray2: PyArrayHolder = _
  private var pyArray3: PyArrayHolder = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    pyArray1 = new PyArrayHolder()
    pyArray2 = new PyArrayHolder()
    pyArray3 = new PyArrayHolder()
  }

  override def afterAll(): Unit = {
    pyArray1.close()
    pyArray2.close()
    pyArray3.close()
    super.afterAll()
  }

  test("loop") {
    val floatArray1 = Array(1.0, 8.0, 2.0, 3.0, 4.0, 1.0, 1.0, 2.0).map(_.toFloat)
    TestUtils.doTest2(
      bitcode = s"$basePath/for1_test-cfunc-float32.bc",
      source = s"$basePath/for1_test.py",
      argTypes = Seq(jLong.TYPE, jInt.TYPE),
      arguments = Seq(new jLong(pyArray1.`with`(floatArray1).addr()), new jInt(floatArray1.size)),
      expected = Some(22.0)
    )
    val doubleArray1 = Array(2.0, 1.0, 5.0, 13.0, 4.0)
    TestUtils.doTest2(
      bitcode = s"$basePath/for1_test-cfunc-float64.bc",
      source = s"$basePath/for1_test.py",
      argTypes = Seq(jLong.TYPE, jInt.TYPE),
      arguments = Seq(new jLong(pyArray1.`with`(doubleArray1).addr()), new jInt(doubleArray1.size)),
      expected = Some(25.0)
    )
    val floatArray2 = Array(4.0, -5.0, 2.0, 8.0).map(_.toFloat)
    TestUtils.doTest2(
      bitcode = s"$basePath/for2_test-cfunc-float32.bc",
      source = s"$basePath/for2_test.py",
      argTypes = Seq(jLong.TYPE),
      arguments = Seq(new jLong(pyArray1.`with`(floatArray2).addr())),
      expected = Some(9.0)
    )
    val doubleArray2 = Array(5.0, 3.0, -9.0, 5.0, 1.0, 2.0, 2.0, 9.0, 1.0, 3.0)
    TestUtils.doTest2(
      bitcode = s"$basePath/for2_test-cfunc-float64.bc",
      source = s"$basePath/for2_test.py",
      argTypes = Seq(jLong.TYPE),
      arguments = Seq(new jLong(pyArray1.`with`(doubleArray2).addr())),
      expected = Some(22.0)
    )
  }

  test("numpy power") {
    val floatX = pyArray1.`with`(Array(1.0f, 2.0f, 3.0f, 4.0f))
    val floatY = pyArray2.`with`(Array(1.0f, 2.0f, 3.0f, 4.0f))
    val result1 = TestUtils.doTest2[Long](
      bitcode = s"$basePath/numpy_power_test-cfunc-float32.bc",
      source = s"$basePath/numpy_power_test.py",
      argTypes = Seq(jLong.TYPE, jLong.TYPE),
      arguments = Seq(new jLong(floatX.addr()), new jLong(floatY.addr()))
    )
    val resultArray1 = new PyArrayHolder(result1)
    assert(resultArray1.floatArray() === Seq(1.0f, 8.0f, 27.0f, 64.0f))

    val doubleX = pyArray1.`with`(Array(1.0, 2.0, 3.0, 4.0))
    val doubleY = pyArray2.`with`(Array(1.0, 2.0, 3.0, 4.0))
    val result2 = TestUtils.doTest2[Long](
      bitcode = s"$basePath/numpy_power_test-cfunc-float64.bc",
      source = s"$basePath/numpy_power_test.py",
      argTypes = Seq(jLong.TYPE, jLong.TYPE),
      arguments = Seq(new jLong(doubleX.addr()), new jLong(doubleY.addr()))
    )
    val resultArray2 = new PyArrayHolder(result2)
    assert(resultArray2.doubleArray() === Seq(1.0, 8.0, 27.0, 64.0))
  }

  test("numpy dot - vv") {
    // Vector * Vector case
    val floatX = pyArray1.`with`(Array(1.0f, 2.0f, 3.0f, 4.0f)).reshape(4, 1)
    val floatY = pyArray2.`with`(Array(1.0f, 2.0f, 3.0f, 4.0f)).reshape(4, 1)
    TestUtils.doTest2[Float](
      bitcode = s"$basePath/numpy_dot_test-cfunc-vv-float32.bc",
      source = s"$basePath/numpy_dot_test.py",
      argTypes = Seq(jLong.TYPE, jLong.TYPE),
      arguments = Seq(new jLong(floatX.addr()), new jLong(floatY.addr())),
      expected = Some(30.0f)
    )

    val doubleX = pyArray1.`with`(Array(1.0, 2.0, 3.0, 4.0)).reshape(4, 1)
    val doubleY = pyArray2.`with`(Array(1.0, 2.0, 3.0, 4.0)).reshape(4, 1)
    TestUtils.doTest2[Double](
      bitcode = s"$basePath/numpy_dot_test-cfunc-vv-float64.bc",
      source = s"$basePath/numpy_dot_test.py",
      argTypes = Seq(jLong.TYPE, jLong.TYPE),
      arguments = Seq(new jLong(doubleX.addr()), new jLong(doubleY.addr())),
      expected = Some(30.0)
    )
  }

  test("numpy dot - mv") {
    // Matrix * Vector case
    val floatX = pyArray1.`with`(Array(1.0f, 2.0f, 3.0f, 4.0f)).reshape(2, 2)
    val floatY = pyArray2.`with`(Array(1.0f, 2.0f)).reshape(2, 1)
    val result1 = TestUtils.doTest2[Long](
      bitcode = s"$basePath/numpy_dot_test-cfunc-mv-float32.bc",
      source = s"$basePath/numpy_dot_test.py",
      argTypes = Seq(jLong.TYPE, jLong.TYPE),
      arguments = Seq(new jLong(floatX.addr()), new jLong(floatY.addr()))
    )
    val resultArray1 = new PyArrayHolder(result1)
    assert(resultArray1.floatArray() === Seq(5.0f, 11.0f))

    val doubleX = pyArray1.`with`(Array(1.0, 2.0, 3.0, 4.0)).reshape(2, 2)
    val doubleY = pyArray2.`with`(Array(1.0, 2.0)).reshape(2, 1)
    val result2 = TestUtils.doTest2[Long](
      bitcode = s"$basePath/numpy_dot_test-cfunc-mv-float64.bc",
      source = s"$basePath/numpy_dot_test.py",
      argTypes = Seq(jLong.TYPE, jLong.TYPE),
      arguments = Seq(new jLong(doubleX.addr()), new jLong(doubleY.addr()))
    )
    val resultArray2 = new PyArrayHolder(result2)
    assert(resultArray2.doubleArray() === Seq(5.0, 11.0))
  }

  test("numpy dot - mm") {
    // Matrix * Matrix case
    val floatX = pyArray1.`with`(Array(1.0f, 2.0f, 3.0f, 4.0f)).reshape(2, 2)
    val floatY = pyArray2.`with`(Array(1.0f, 2.0f, 3.0f, 4.0f)).reshape(2, 2)
    val result1 = TestUtils.doTest2[Long](
      bitcode = s"$basePath/numpy_dot_test-cfunc-mm-float32.bc",
      source = s"$basePath/numpy_dot_test.py",
      argTypes = Seq(jLong.TYPE, jLong.TYPE),
      arguments = Seq(new jLong(floatX.addr()), new jLong(floatY.addr()))
    )
    val resultArray1 = new PyArrayHolder(result1)
    assert(resultArray1.floatArray() === Seq(7.0f, 10.0f, 15.0f, 22.0f))

    val doubleX = pyArray1.`with`(Array(1.0, 2.0, 3.0, 4.0)).reshape(2, 2)
    val doubleY = pyArray2.`with`(Array(1.0, 2.0, 3.0, 4.0)).reshape(2, 2)
    val result2 = TestUtils.doTest2[Long](
      bitcode = s"$basePath/numpy_dot_test-cfunc-mm-float64.bc",
      source = s"$basePath/numpy_dot_test.py",
      argTypes = Seq(jLong.TYPE, jLong.TYPE),
      arguments = Seq(new jLong(doubleX.addr()), new jLong(doubleY.addr()))
    )
    val resultArray2 = new PyArrayHolder(result2)
    assert(resultArray2.doubleArray() === Seq(7.0, 10.0, 15.0, 22.0))
  }

  test("numpy dot - throws an exception when hitting incompatible shapes") {
    val floatX = pyArray1.`with`(Array(1.0f, 2.0f, 3.0f, 4.0f)).reshape(4, 1)
    val floatY = pyArray2.`with`(Array(1.0f, 2.0f, 3.0f, 4.0f)).reshape(2, 2)
    val errMsg = intercept[InvocationTargetException] {
      val method = LLJVMUtils.getMethod(
        TestUtils.loadClassFromResource(s"$basePath/numpy_dot_test-cfunc-mm-float32.bc"),
        jLong.TYPE, jLong.TYPE)
      method.invoke(null, new jLong(floatX.addr()), new jLong(floatY.addr()))
    }.getCause.getMessage
    assert(errMsg.contains("Numba runtime exception <Numba C callback 'numpy_dot_test'>"))
  }

  test("linear regression") {
    val doubleX = pyArray1.`with`(Array(1.0, 1.0)).reshape(2, 1)
    val doubleY = pyArray2.`with`(Array(1.0, 1.0, 1.0, 1.0)).reshape(2, 2)
    val doubleZ = pyArray3.`with`(Array(1.0, 1.0)).reshape(2, 1)
    val result = TestUtils.doTest2[Long](
      bitcode = s"$basePath/linear_regression-numba-cfunc-float64.bc",
      source = s"$basePath/numba_examples/linear_regression.py",
      argTypes = Seq(jLong.TYPE, jLong.TYPE, jLong.TYPE, jLong.TYPE, jDouble.TYPE),
      arguments = Seq(
        new jLong(doubleX.addr()), // Y
        new jLong(doubleY.addr()), // X
        new jLong(doubleZ.addr()), // w
        new jLong(50),             // iterations
        new jDouble(0.1))          // alphaN
    )
    val doubleArray = new PyArrayHolder(result).doubleArray()
    // Just checks the result values approach to 0.50
    assert(doubleArray(0) - 0.50 < 1e-10)
    assert(doubleArray(1) - 0.50 < 1e-10)
  }

  test("logistic regression") {
    val doubleX = pyArray1.`with`(Array(1.0, 1.0)).reshape(2, 1)
    val doubleY = pyArray2.`with`(Array(1.0, 1.0, 1.0, 1.0)).reshape(2, 2)
    val doubleZ = pyArray3.`with`(Array(1.0, 1.0)).reshape(2, 1)
    val result = TestUtils.doTest2[Long](
      bitcode = s"$basePath/logistic_regression-numba-cfunc-float64.bc",
      source = s"$basePath/numba_examples/logistic_regression/logistic_regression.py",
      argTypes = Seq(jLong.TYPE, jLong.TYPE, jLong.TYPE, jLong.TYPE),
      arguments = Seq(
        new jLong(doubleX.addr()), // Y
        new jLong(doubleY.addr()), // X
        new jLong(doubleZ.addr()), // w
        new jLong(100))            // iterations
    )
    val doubleArray = new PyArrayHolder(result).doubleArray()
    // Just checks the result values approach to zero
    assert(doubleArray(0) < 1.0)
    assert(doubleArray(1) < 1.0)
  }
}
