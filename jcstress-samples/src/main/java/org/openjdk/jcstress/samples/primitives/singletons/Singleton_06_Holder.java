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

public class Singleton_06_Holder {

    public static class FinalHolderHolder {
        public Holder get() {
            return H.INSTANCE;
        }

        public static class H {
            public static final Holder INSTANCE = new FinalHolder("data");
        }
    }

    public static class NonFinalHolderHolder {
        public Holder get() {
            return H.INSTANCE;
        }

        public static class H {
            public static final Holder INSTANCE = new NonFinalHolder("data");
        }
    }

    @JCStressTest
    @State
    @Outcome(id = "data, data", expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class Final {
        final FinalHolderHolder singleton = new FinalHolderHolder();
        @Actor public void actor1(LL_Result r) { r.r1 = Holder.map(singleton.get()); }
        @Actor public void actor2(LL_Result r) { r.r2 = Holder.map(singleton.get()); }
    }

    @JCStressTest
    @State
    @Outcome(id = "data, data", expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class NonFinal {
        final NonFinalHolderHolder singleton = new NonFinalHolderHolder();
        @Actor public void actor1(LL_Result r) { r.r1 = Holder.map(singleton.get()); }
        @Actor public void actor2(LL_Result r) { r.r2 = Holder.map(singleton.get()); }
    }

}
