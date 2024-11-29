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

public class Singleton_09_FinalWrapper {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Singleton_09
     */

    /*
        ----------------------------------------------------------------------------------------------------------

        This example is here for completeness.

        Another way to avoid
        If one studies Singleton_05_DCL example more deeply, then one can ask whether the full-blown volatile
        is even needed. The short answer is: it is not needed.

        We only need two things here:
          1. Causality between seeing the instance and its contents. A release/acquire chain would give us
             the required semantics. (3) -> (4) provides that chain. See BasicJMM_06_Causality example for
             more discussion.
          2. Coherence between unsynchronized loads. Plain field reads are not coherent, but opaque reads are.
             (1) -> (4), (2) -> (4) chains provides the coherence. See BasicJMM_05_Coherence example for
             more discussion.

         This might improve performance on weakly-ordered platforms, where the sequentially-consistent loads
         are more heavy-weight than acquire loads.
     */

    public static class FinalWrapper<T> implements Factory<T> {
        private Wrapper<T> wrapper;

        @Override
        public T get(Supplier<T> s) {
            Wrapper<T> w = wrapper;
            if (w == null) {
                synchronized(this) {
                    w = wrapper;
                    if (w == null) {
                        wrapper = w = new Wrapper<>(s.get());
                    }
                }
            }
            return w.t;
        }

        private static class Wrapper<T> {
            public final T t;
            public Wrapper(T t) {
                this.t = t;
            }
        }
    }

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class Final {
        FinalWrapper<Singleton> factory = new FinalWrapper<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new FinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new FinalSingleton("data2")); }
    }

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class NonFinal {
        FinalWrapper<Singleton> factory = new FinalWrapper<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new NonFinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new NonFinalSingleton("data2")); }
    }

}
