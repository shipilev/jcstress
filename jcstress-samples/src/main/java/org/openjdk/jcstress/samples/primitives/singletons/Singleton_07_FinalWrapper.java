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
import org.openjdk.jcstress.samples.primitives.singletons.shared.Holder;
import org.openjdk.jcstress.samples.primitives.singletons.shared.FinalHolder;
import org.openjdk.jcstress.samples.primitives.singletons.shared.NonFinalHolder;

import java.util.function.Supplier;

public class Singleton_07_FinalWrapper {

    public static class FinalWrapper {
        private FW wrapper;

        public Holder getInstance(Supplier<Holder> s) {
            // FIXME: Formally a data race here.
            if (wrapper == null) {
                synchronized(this) {
                    if (wrapper == null) {
                        wrapper = new FW(s.get());
                    }
                }
            }
            return wrapper.instance;
        }

        private static class FW {
            public final Holder instance;
            public FW(Holder instance) {
                this.instance = instance;
            }
        }
    }

    @JCStressTest
    @State
    @Outcome(id = "data, data", expect = Expect.ACCEPTABLE, desc = "Seeing the proper data.")
    public static class Final {
        final FinalWrapper singleton = new FinalWrapper();
        @Actor public void actor1(LL_Result r) { r.r1 = Holder.map(singleton.getInstance(FinalHolder::new)); }
        @Actor public void actor2(LL_Result r) { r.r2 = Holder.map(singleton.getInstance(FinalHolder::new)); }
    }

    @JCStressTest
    @State
    @Outcome(id = "data, data", expect = Expect.ACCEPTABLE, desc = "Seeing the proper data.")
    public static class NonFinal {
        final FinalWrapper singleton = new FinalWrapper();
        @Actor public void actor1(LL_Result r) { r.r1 = Holder.map(singleton.getInstance(NonFinalHolder::new)); }
        @Actor public void actor2(LL_Result r) { r.r2 = Holder.map(singleton.getInstance(NonFinalHolder::new)); }
    }

}
