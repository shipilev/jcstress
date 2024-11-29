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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.LL_Result;
import org.openjdk.jcstress.infra.results.L_Result;
import org.openjdk.jcstress.samples.primitives.lazy.shared.Holder;
import org.openjdk.jcstress.samples.primitives.lazy.shared.HolderSupplier;
import org.openjdk.jcstress.samples.primitives.lazy.shared.Lazy;

import java.lang.invoke.VarHandle;
import java.util.function.Supplier;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

/*
    How to run this test:
    $ java -jar jcstress-samples/target/jcstress.jar -t LazyTest
*/

public class Lazy_05_Fenced {

    static class FencedLazy<T> implements Lazy<T> {
        private Supplier<T> factory;
        private T value;

        public FencedLazy(final Supplier<T> factory) {
            this.factory = factory;
            VarHandle.storeStoreFence();
        }

        @Override
        public T get() {
            if (factory == null) {
                VarHandle.acquireFence();
                return value;
            }

            synchronized (this) {
                if (factory != null) {
                    value = factory.get();
                    VarHandle.releaseFence();
                    factory = null;
                }
                return value;
            }
        }
    }

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2"}, expect = ACCEPTABLE, desc = "Trivial.")
    public static class Basic {
        Lazy<Holder> lazy = new FencedLazy<>(new HolderSupplier());
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

    @JCStressTest
    @State
    @Outcome(id = "null-holder, null-holder", expect = ACCEPTABLE, desc = "Seeing a null holder.")
    public static class NullHolder {
        Lazy<Holder> lazy = new FencedLazy<>(() -> null);
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }


    @JCStressTest
    @State
    @Outcome(id = {"data1", "data2"}, expect = ACCEPTABLE, desc = "Trivial.")
    @Outcome(id = {"null-lazy"},      expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    public static class RacyOneWay {
        Lazy<Holder> lazy;
        @Actor public void actor1() { lazy = new FencedLazy<>(new HolderSupplier()); }
        @Actor public void actor2(L_Result r) { r.r1 = Lazy.map(lazy); }
    }

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2"}, expect = ACCEPTABLE, desc = "Trivial.")
    @Outcome(id = {"null-lazy, data.", "data., null-lazy", "null-lazy, null-lazy"}, expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    public static class RacyTwoWay {
        Lazy<Holder> lazy;
        @Actor public void actor1() { lazy = new FencedLazy<>(new HolderSupplier()); }
        @Actor public void actor2(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor3(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }
}