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
import org.openjdk.jcstress.samples.primitives.lazy.shared.HolderFactory;
import org.openjdk.jcstress.samples.primitives.lazy.shared.Lazy;
import org.openjdk.jcstress.samples.primitives.lazy.shared.NullHolderFactory;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

public class Lazy_05_AtomicRefOneShot {

   /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Lazy_04
    */

    /*
        ----------------------------------------------------------------------------------------------------------

        Lazy_04_BrokenOneShot shows us we cannot have `volatile` field have the `final` semantics we need
        to protect from constructor races. If only we had a way to get both at the same time. And we have one:
        we can have a volatile wrapper that we put into final field.
     */

    static class AtomicRefLazy<T> implements Lazy<T> {
        private final AtomicReference<Supplier<T>> factoryRef;
        private T instance;

        public AtomicRefLazy(final Supplier<T> factory) {
            this.factoryRef = new AtomicReference<>(factory);
        }

        @Override
        public T get() {
            if (factoryRef.get() == null) {
                return instance;
            }

            synchronized (this) {
                if (factoryRef.get() != null) {
                   instance = factoryRef.get().get();
                   factoryRef.set(null);
                }
                return instance;
            }
        }
    }

    /*
      RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
  data, data  714,969,544  100.00%  Acceptable  Trivial.

     */

    @JCStressTest
    @State
    @Outcome(id = "data, data", expect = ACCEPTABLE, desc = "Trivial.")
    public static class Basic {
        Lazy<Holder> lazy = new AtomicRefLazy<>(new HolderFactory());
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

/*
                    RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
  null-holder, null-holder  838,586,824  100.00%  Acceptable  Seeing a null holder.

 */

    @JCStressTest
    @State
    @Outcome(id = "null-holder, null-holder", expect = ACCEPTABLE, desc = "Seeing a null holder.")
    public static class NullHolder {
        Lazy<Holder> lazy = new AtomicRefLazy<>(new NullHolderFactory());
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

    /*
     RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
       data    632,710,141   32.62%  Acceptable  Trivial.
  null-lazy  1,307,055,563   67.38%  Acceptable  Lazy instance not seen yet.
     */

    @JCStressTest
    @State
    @Outcome(id = "data",      expect = ACCEPTABLE, desc = "Trivial.")
    @Outcome(id = "null-lazy", expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    public static class RacyOneWay {
        Lazy<Holder> lazy;
        @Actor public void actor1()           { lazy = new AtomicRefLazy<>(new HolderFactory()); }
        @Actor public void actor2(L_Result r) { r.r1 = Lazy.map(lazy); }
    }

    /*
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
            data, data    482,246,211   12.03%  Acceptable  Trivial.
       data, null-lazy    644,270,368   16.07%  Acceptable  Lazy instance not seen yet.
       null-lazy, data    693,570,498   17.30%  Acceptable  Lazy instance not seen yet.
  null-lazy, null-lazy  2,189,716,247   54.61%  Acceptable  Lazy instance not seen yet.

     */

    @JCStressTest
    @State
    @Outcome(id = "data, data", expect = ACCEPTABLE, desc = "Trivial.")
    @Outcome(id = {"null-lazy, data", "data, null-lazy", "null-lazy, null-lazy"}, expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    public static class RacyTwoWay {
        Lazy<Holder> lazy;
        @Actor public void actor1() { lazy = new AtomicRefLazy<>(new HolderFactory()); }
        @Actor public void actor2(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor3(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

}