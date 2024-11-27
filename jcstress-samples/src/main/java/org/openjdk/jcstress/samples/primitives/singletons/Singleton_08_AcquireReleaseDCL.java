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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Supplier;

public class Singleton_08_AcquireReleaseDCL {

    public static class AcquireReleaseDCL<T> implements Factory<T> {
        static final VarHandle VH;
        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(AcquireReleaseDCL.class, "instance", Object.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private T instance;

        @Override
        public T get(Supplier<T> supplier) {
            if (VH.getOpaque(this) == null) {
                synchronized (this) {
                    if (VH.getOpaque(this) == null) {
                        VH.setRelease(this, supplier.get());
                    }
                }
            }
            return (T) VH.getAcquire(this);
        }
    }

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class Final {
        AcquireReleaseDCL<Singleton> factory = new AcquireReleaseDCL<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new FinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new FinalSingleton("data2")); }
    }

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class NonFinal {
        AcquireReleaseDCL<Singleton> factory = new AcquireReleaseDCL<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new NonFinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new NonFinalSingleton("data2")); }
    }

}
