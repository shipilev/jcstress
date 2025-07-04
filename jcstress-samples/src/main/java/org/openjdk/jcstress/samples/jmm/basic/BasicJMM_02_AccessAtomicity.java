/*
 * Copyright (c) 2016, 2021, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jcstress.samples.jmm.basic;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;
import org.openjdk.jcstress.infra.results.J_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;

import static org.openjdk.jcstress.annotations.Expect.*;

public class BasicJMM_02_AccessAtomicity {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t BasicJMM_02_AccessAtomicity[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        This is our second case: access atomicity. Most basic types come with an
        intuitive property: the reads and the writes of these basic types happen
        in full, even under races:

          RESULT         SAMPLES     FREQ      EXPECT  DESCRIPTION
              -1  11,916,557,823   81.45%  Acceptable  Seeing the full value.
               0   2,714,388,481   18.55%  Acceptable  Seeing the default value: writer had not acted yet.
     */

    @JCStressTest
    @Outcome(id = "0",  expect = ACCEPTABLE, desc = "Seeing the default value: writer had not acted yet.")
    @Outcome(id = "-1", expect = ACCEPTABLE, desc = "Seeing the full value.")
    @Outcome(           expect = FORBIDDEN,  desc = "Other cases are forbidden.")
    @State
    public static class Integers {
        int v;

        @Actor
        public void writer() {
            v = 0xFFFFFFFF;
        }

        @Actor
        public void reader(I_Result r) {
            r.r1 = v;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        There are a few interesting exceptions codified in Java Language Specification,
        under 17.7 "Non-Atomic Treatment of double and long". It says that longs and
        doubles could be treated non-atomically.

        This test would yield interesting results on some 32-bit VMs, for example x86_32:

               RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
                   -1  8,818,463,884   70.12%   Acceptable  Seeing the full value.
          -4294967296      9,586,556    0.08%  Interesting  Other cases are violating access atomicity, but allowed u...
                    0  3,747,652,022   29.80%   Acceptable  Seeing the default value: writer had not acted yet.
           4294967295         86,082   <0.01%  Interesting  Other cases are violating access atomicity, but allowed u...

        Other 32-bit VMs may still choose to use the advanced instructions to regain atomicity,
        for example on ARMv7 (32-bit):

              RESULT     SAMPLES     FREQ       EXPECT  DESCRIPTION
                  -1  96,332,256   79.50%   Acceptable  Seeing the full value.
                   0  24,839,456   20.50%   Acceptable  Seeing the default value: writer had not acted yet.

     */

    @JCStressTest
    @Outcome(id = "0",  expect = ACCEPTABLE,             desc = "Seeing the default value: writer had not acted yet.")
    @Outcome(id = "-1", expect = ACCEPTABLE,             desc = "Seeing the full value.")
    @Outcome(           expect = ACCEPTABLE_INTERESTING, desc = "Other cases are violating access atomicity, but allowed under JLS.")
    @Ref("https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#jls-17.7")
    @State
    public static class Longs {
        long v;

        @Actor
        public void writer() {
            v = 0xFFFFFFFF_FFFFFFFFL;
        }

        @Actor
        public void reader(J_Result r) {
            r.r1 = v;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Recovering the access atomicity is possible by making the field "volatile":

        x86_32:
          RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
              -1  1,306,213,861   17.27%  Acceptable  Seeing the full value.
               0  6,257,145,883   82.73%  Acceptable  Seeing the default value: writer had not acted yet.
     */

    @JCStressTest
    @Outcome(id = "0",  expect = ACCEPTABLE, desc = "Seeing the default value: writer had not acted yet.")
    @Outcome(id = "-1", expect = ACCEPTABLE, desc = "Seeing the full value.")
    @Outcome(           expect = FORBIDDEN,  desc = "Other cases are forbidden.")
    @State
    public static class VolatileLongs {
        volatile long v;

        @Actor
        public void writer() {
            v = 0xFFFFFFFF_FFFFFFFFL;
        }

        @Actor
        public void reader(J_Result r) {
            r.r1 = v;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Since Java 9, VarHandles in "opaque" access mode also require access atomicity. The upside for using
        opaque instead of volatile is avoiding carrying the additional memory semantics, which makes volatiles
        slower.

        x86_32:
          RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
              -1  7,306,206,964   64.70%  Acceptable  Seeing the full value.
               0  3,985,362,700   35.30%  Acceptable  Seeing the default value: writer had not acted yet.
     */

    @JCStressTest
    @Outcome(id = "0",  expect = ACCEPTABLE, desc = "Seeing the default value: writer had not acted yet.")
    @Outcome(id = "-1", expect = ACCEPTABLE, desc = "Seeing the full value.")
    @Outcome(           expect = FORBIDDEN,  desc = "Other cases are forbidden.")
    @State
    public static class OpaqueLongs {

        static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(OpaqueLongs.class, "v", long.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        long v;

        @Actor
        public void writer() {
            VH.setOpaque(this, 0xFFFFFFFF_FFFFFFFFL);
        }

        @Actor
        public void reader(J_Result r) {
            r.r1 = (long) VH.getOpaque(this);
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        While the spec requirements for field and array element accesses are strict, the implementations of
        concrete classes may have a relaxed semantics. Take ByteBuffer where we can read the 4-byte integer
        from an arbitrary offset.

        Older ByteBuffer implementations accessed one byte at a time, and that required merging/splitting
        anything larger than a byte into the individual operations. Of course, there is no access atomicity
        there by construction. In newer ByteBuffer implementations, the _aligned_ accesses are done with
        larger instructions that gives back atomicity. Misaligned accesses would still have to do several
        narrower accesses on machines that don't support misalignments.

        x86_64:
             RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
                 -1  142,718,041   61.57%   Acceptable  Seeing the full value.
          -16711936            4   <0.01%  Interesting  Other cases are allowed, because reads/writes are not ato...
          -16777216      111,579    0.05%  Interesting  Other cases are allowed, because reads/writes are not ato...
               -256      110,267    0.05%  Interesting  Other cases are allowed, because reads/writes are not ato...
             -65281            3   <0.01%  Interesting  Other cases are allowed, because reads/writes are not ato...
             -65536      111,618    0.05%  Interesting  Other cases are allowed, because reads/writes are not ato...
                  0   88,765,143   38.29%   Acceptable  Seeing the default value: writer had not acted yet.
           16711680           36   <0.01%  Interesting  Other cases are allowed, because reads/writes are not ato...
           16777215            5   <0.01%  Interesting  Other cases are allowed, because reads/writes are not ato...
                255            1   <0.01%  Interesting  Other cases are allowed, because reads/writes are not ato...
              65535            7   <0.01%  Interesting  Other cases are allowed, because reads/writes are not ato...
    */

    @JCStressTest
    @Outcome(id = "0",  expect = ACCEPTABLE,             desc = "Seeing the default value: writer had not acted yet.")
    @Outcome(id = "-1", expect = ACCEPTABLE,             desc = "Seeing the full value.")
    @Outcome(           expect = ACCEPTABLE_INTERESTING, desc = "Other cases are allowed, because reads/writes are not atomic.")
    @State
    public static class ByteBuffers {
        public static final int SIZE = 256;

        ByteBuffer bb = ByteBuffer.allocate(SIZE);
        int idx = ThreadLocalRandom.current().nextInt(SIZE - 4);

        @Actor
        public void writer() {
            bb.putInt(idx, 0xFFFFFFFF);
        }

        @Actor
        public void reader(I_Result r) {
            r.r1 = bb.getInt(idx);
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        However, even if the misaligned accesses is supported by hardware, it would never be guaranteed atomic.
        For example, reading the value that spans two cache-lines would not be atomic, even if we manage to issue
        a single instruction for access.

        x86_64:
             RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
                 -1  127,819,822   48.55%   Acceptable  Seeing the full value.
          -16777216           17   <0.01%  Interesting  Other cases are allowed, because reads/writes are not ato...
               -256           17   <0.01%  Interesting  Other cases are allowed, because reads/writes are not ato...
             -65536           11   <0.01%  Interesting  Other cases are allowed, because reads/writes are not ato...
                  0  134,990,763   51.27%   Acceptable  Seeing the default value: writer had not acted yet.
           16777215      154,265    0.06%  Interesting  Other cases are allowed, because reads/writes are not ato...
                255      154,643    0.06%  Interesting  Other cases are allowed, because reads/writes are not ato...
              65535      154,446    0.06%  Interesting  Other cases are allowed, because reads/writes are not ato...
     */

    @JCStressTest
    @Outcome(id = "0",  expect = ACCEPTABLE, desc = "Seeing the default value: writer had not acted yet.")
    @Outcome(id = "-1", expect = ACCEPTABLE, desc = "Seeing the full value.")
    @Outcome(expect = ACCEPTABLE_INTERESTING, desc = "Other cases are allowed, because reads/writes are not atomic.")
    @State
    public static class CrossCacheLine {
        private static final VarHandle VH = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.nativeOrder());

        private static final int BYTE_SIZE = 256;

        int off = ThreadLocalRandom.current().nextInt(BYTE_SIZE - Integer.BYTES);

        byte[] ss = new byte[BYTE_SIZE];

        @Actor
        public void writer() {
            VH.set(ss, off, 0xFFFFFFFF);
        }

        @Actor
        public void reader(I_Result r) {
            r.r1 = (int)VH.get(ss, off);
        }
    }

}
