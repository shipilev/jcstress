/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jcstress.samples.primitives.singletons;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.LL_Result;
import org.openjdk.jcstress.samples.primitives.singletons.shared.*;

import java.util.function.Supplier;

public class Singleton_05_DCL {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Singleton_05
    */

    /*
        ----------------------------------------------------------------------------------------------------------

        All the observations and samples so far provide us with the building blocks for so called
        "Double-Checked Locking" pattern. See how two features work in tandem:
           1. Volatile `instance` provides us with publication guarantees, while not solving interleaving
              or single object creation problem. (See Singleton_02_BrokenVolatile)
           2. Synchronized provides us with mutual exclusion for creating the object once.
              (See Singleton_04_InefficientSynchronized)
           3. The check before and after taking the lock solves the interleaving problem.
     */

    public static class DCL<T> implements Factory<T> {
        private volatile T instance;

        @Override
        public T get(Supplier<T> supplier) {
            if (instance == null) {
                synchronized (this) {
                    if (instance == null) {
                        instance = supplier.get();
                    }
                }
            }
            return instance;
        }
    }

    /*
        Indeed, this works well on all architectures.

        x86_64, AArch64:
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  1,259,722,777   57.88%  Acceptable  Trivial.
          data2, data2    916,873,647   42.12%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class Final {
        DCL<Singleton> factory = new DCL<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new FinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new FinalSingleton("data2")); }
    }

    /*
        Since volatile provides us with publication guarantees, non-final singletons also work well.

        x86_64, AArch64:
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  1,057,019,574   56.88%  Acceptable  Trivial.
          data2, data2    801,420,050   43.12%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class NonFinal {
        DCL<Singleton> factory = new DCL<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new NonFinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new NonFinalSingleton("data2")); }
    }

}
