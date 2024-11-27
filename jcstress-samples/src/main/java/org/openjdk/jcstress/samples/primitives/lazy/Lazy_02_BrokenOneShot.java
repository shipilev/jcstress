/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
package org.openjdk.jcstress.samples.primitives.lazy;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.LLZ_Result;
import org.openjdk.jcstress.infra.results.LL_Result;
import org.openjdk.jcstress.infra.results.L_Result;
import org.openjdk.jcstress.samples.primitives.lazy.shared.Holder;
import org.openjdk.jcstress.samples.primitives.lazy.shared.HolderSupplier;
import org.openjdk.jcstress.samples.primitives.lazy.shared.Lazy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

/*
    How to run this test:
    $ java -jar jcstress-samples/target/jcstress.jar -t LazyTest
*/

public class Lazy_02_BrokenOneShot {

    static class BrokenOneShotLazy<T> implements Lazy<T> {
        private volatile Supplier<T> factory;
        private T value;

        public BrokenOneShotLazy(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        public T get() {
            if (factory != null) {
                synchronized (this) {
                    if (factory != null) {
                        value = factory.get();
                        factory = null;
                    }
                }
            }
            return value;
        }
    }

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2"}, expect = ACCEPTABLE, desc = "Seeing the proper data.")
    public static class Basic {
        Lazy<Holder> lazy = new BrokenOneShotLazy<>(new HolderSupplier());
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

    @JCStressTest
    @State
    @Outcome(id = "null-holder, null-holder, true", expect = ACCEPTABLE, desc = "Seeing a null holder, called factory exactly once.")
    public static class NullHolder {
        AtomicInteger counter = new AtomicInteger();
        Lazy<Holder> lazy = new BrokenOneShotLazy<>(() -> { counter.incrementAndGet(); return null; });

        @Actor   public void actor1(LLZ_Result r)  { r.r1 = Lazy.map(lazy); }
        @Actor   public void actor2(LLZ_Result r)  { r.r2 = Lazy.map(lazy); }
        @Arbiter public void arbiter(LLZ_Result r) { r.r3 = (counter.get() == 1); }
    }

    @JCStressTest
    @State
    @Outcome(id = {"data1", "data2"}, expect = ACCEPTABLE, desc = "Trivial.")
    @Outcome(id = {"null-lazy"},      expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    @Outcome(id = {"null-holder"},    expect = ACCEPTABLE_INTERESTING, desc = "Seeing uninitialized holder!")
    public static class RacyOneWay {
        Lazy<Holder> lazy;
        @Actor public void actor1() { lazy = new BrokenOneShotLazy<>(new HolderSupplier()); }
        @Actor public void actor2(L_Result r) { r.r1 = Lazy.map(lazy); }
    }

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2"}, expect = ACCEPTABLE, desc = "Trivial.")
    @Outcome(id = {"null-lazy, data.", "data., null-lazy", "null-lazy, null-lazy"}, expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    @Outcome(id = {"null-holder, .*", ".*, null-holder"}, expect = ACCEPTABLE_INTERESTING, desc = "Seeing uninitialized holder!")
    public static class RacyTwoWay {
        Lazy<Holder> lazy;
        @Actor public void actor1() { lazy = new BrokenOneShotLazy<>(new HolderSupplier()); }
        @Actor public void actor2(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor3(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

}