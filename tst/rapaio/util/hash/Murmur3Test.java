/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 * Copyright 2013 - 2022 Aurelian Tutuianu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rapaio.util.hash;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class Murmur3Test {

    @Test
    public void testCorrectValues() {
        byte[] bytes = "Now is the time for all good men to come to the aid of their country".getBytes(StandardCharsets.UTF_8);
        int hash = 0;
        for (int i = 0; i < bytes.length; i++) {
            hash = hash * 31 + (bytes[i] & 0xff);
            bytes[i] = (byte) hash;
        }

        // test different offsets.
        for (int offset = 0; offset < 20; offset++) {
            // put the original bytes at the offset so the same hash will be generated
            byte[] arr = new byte[bytes.length + offset];
            System.arraycopy(bytes, 0, arr, offset, bytes.length);
            int seed = 1;

            for (int len = 0; len < bytes.length; len++) {
                seed *= 0x9e3779b1;
                int h = Murmur3.murmur3A(arr, offset, len, seed);
                assertEquals(answers32[len], h);
                long[] result = Murmur3.murmur3F(arr, offset, len, seed);
                assertEquals(answers128[len * 2], result[0]);
                assertEquals(answers128[len * 2 + 1], result[1]);
            }
        }
    }

    static int[] answers32 = new int[] {
            0x11fd02eb, 0x8dd65a73, 0x29b074ba, 0xcbcd43ce, 0xb6463881, 0xf6228557, 0x3d55c634, 0xa1bb9072,
            0x448402c6, 0xb12bf3d4, 0x18a71ccb, 0x6ae5f185, 0x9a482256, 0xc686d7f2, 0x8e8984d8, 0x68a2491d,
            0xcc29b0e6, 0x3e9130bd, 0xc90defb3, 0xf81c5978, 0x15ff7f63, 0x4ec16a7a, 0xa08aa899, 0x7317ffee,
            0x93752d34, 0x400f8781, 0x2358838c, 0x6ecb8998, 0x45a5c102, 0x46ed68fd, 0xfecb51c0, 0x7a68c7db,
            0x9e334eab, 0x21ea13b6, 0xf184e92c, 0xc016220d, 0x7f6c9713, 0x1e909123, 0xb51a21b7, 0x94c58881,
            0xe4e91bf0, 0xde80a366, 0xfd84005a, 0x3361d373, 0xe7d528cc, 0x487275a7, 0xf2290ee5, 0x869992a8,
            0x63cdd341, 0x8e94b334, 0x1fc7bf11, 0x5228b0, 0xb4292b62, 0x36ed3770, 0xfe914519, 0x7d9d1830,
            0xe1acfb60, 0xc8b4d4b7, 0xf1ec49ba, 0xedbb8cc1, 0xdc5b3ab1, 0x7c7778ae, 0x52bf68d, 0xe0bb4148,
            0xfea36521, 0xa0696ca5, 0xf28df752, 0xd82dccb6};

    static long[] answers128 = new long[] {
            0x6e54d3ad2be8e9a2L, 0xd99e452d1cfc7decL, 0x609c35d060cf37c1L, 0x4ba03e78929b6807L,
            0xf4865522a8838216L, 0xef8dc0ad3f5a0581L, 0x8513b05a329d04ecL, 0x2295dbef5a603ebcL,
            0xd0259c75fa8711b2L, 0x311f78657cb7ecb9L, 0x771d03baa6accef1L, 0x596d9c3bde77e873L,
            0xdc177610450452dbL, 0x5b85d931e890ef5eL, 0x261f88eedccbbd36L, 0xcba71c1101271139L,
            0xa3a125d270c03cL, 0xc41e9d6ae4ef9d56L, 0xf9b21d4d660517c0L, 0x409d87f99aeb3ea9L,
            0x92d8e70ae59a864L, 0xf4e12d297744d05aL, 0xd894caa03d461dbeL, 0x99d6ff317880f305L,
            0x145d42da3710d23aL, 0x2812adb381c1d64aL, 0xd90254532b45e323L, 0xacbb43b768a7b276L,
            0x74573f58c60c3ddfL, 0xc72b9b42a7cbbd69L, 0xd1129837bea190a7L, 0xa7b20418ce5d46f9L,
            0xa6d094d2a166f659L, 0x10f66ed93811576eL, 0x28d3553af07b8cfaL, 0xdd3b57dcd4d98ec2L,
            0xcd57b4faccaf9764L, 0x1e4001ee8b46813aL, 0xc79f57499389029eL, 0xf4f84142db2d7673L,
            0xfafc9890edaf9086L, 0xc54472528c0fcd98L, 0xd3ff4eff416c02b7L, 0x47c8414e9fa28367L,
            0x78f0171da51288e6L, 0x7f5046c28cd1b43aL, 0xc38dacef191ad1f0L, 0x6210c0aba8230563L,
            0x15e3cd836648fe66L, 0x56a1797408568c1eL, 0x9162e9b79d4f6689L, 0x6fc7ba8e6135592dL,
            0x569e7feab218d54aL, 0x93d21aac30f6029fL, 0x4e7a938ca19a5fe5L, 0x3c7dd68323efe355L,
            0x651993620ca49e3fL, 0x9f0cc9127f8eca7L, 0x3963f278753c4f44L, 0x3f2ab0d0e62bb19fL,
            0x4d72a64283465629L, 0xd9d958282564a987L, 0xde019492e4164d94L, 0xc319fb27d1d42455L,
            0xe788f28b58a0c025L, 0xabb3f2ca571338b4L, 0xaac4a40f227db268L, 0x8f86a5605449d75aL,
            0xcc3999bd3c872160L, 0x3010e16e331a57e2L, 0xd43cfd0741d4ed2L, 0x7954298caa472790L,
            0xfe5b6444abb41ceL, 0xaff3b10d222afeafL, 0xa5438bad24a5629eL, 0xc474fa5e2ff33329L,
            0xbda083bc5d7b382bL, 0xef31a1cda016673eL, 0xda9b98b58bb7eff5L, 0xe001283d41a1576L,
            0x6ee0f9ab35eb17ebL, 0x5de93fcf7e7e0169L, 0x3cd1756a735b7caL, 0x582ded067b6714e9L,
            0x56194735c4168e94L, 0xeeaf5a39dcf76088L, 0xf9d9a9c7d1520670L, 0xb98d7d405a177795L,
            0x3281c2365b5bc415L, 0x85e4cfb23980f8b4L, 0x484aee59fa5880bdL, 0xe000f2daa2078018L,
            0xebff3a4bff725d23L, 0x803e3c3dd2716703L, 0x413e18195eb5b4bfL, 0xce1ea41794fec551L,
            0xcbf65e356e2d69bdL, 0x654a616738582ba7L, 0x62e46d535f11c417L, 0xbd11185034218fa2L,
            0x7c715d440eaa5fb1L, 0xe68ad0d758ade8dL, 0x3242a4d88ac3ba92L, 0x10f1e6939ee06b78L,
            0x965d9c4109ab6eb4L, 0x6bc256008b6083d5L, 0xa8fb3b9666e0eb4dL, 0x2d8a83366565a273L,
            0xa5eddde29cc59fc4L, 0xfd1f7dc9866ceb19L, 0x86c13e98272a7eb9L, 0x11149397f635b42cL,
            0xcbf82258e2b85bf5L, 0x37215737b1ab86fbL, 0x44e5126c5c5f4ae5L, 0x99fe7cce58649b93L,
            0xc455e6ddc7be80f0L, 0xf93bec96644e8723L, 0x130dc4e99fb989e8L, 0xb01734fafdc5308dL,
            0x8fde545bd48cb2feL, 0x1102c89b77b4b405L, 0x2cd24ed5816eca6eL, 0xebd56473a502b63fL,
            0x357fb8e6b489be97L, 0xe163a9495e6d67daL, 0x87411ac34bd7399aL, 0xf8bc18d84f4237bfL,
            0x43702207d2269e74L, 0x37a3eec07a419e21L, 0x7fe4605c33d4ac0cL, 0x6df566b6925a898dL,
            0x89526c269d9225b0L, 0xfc24aac3b731d33eL, 0x2518f6ea6300c3caL, 0xe4e20fdb203d79f5L
    };

    private final Charset utf8Charset = StandardCharsets.UTF_8;

    private void doString(String s) {
        doString(s, 0, 0);
    }

    private void doString(String s, int pre, int post) {
        byte[] utf8 = s.getBytes(utf8Charset);
        int hash1 = Murmur3.murmur3A(utf8, pre, utf8.length - pre - post, 123456789);
        int hash2 = Murmur3.murmur3A(s, pre, s.length() - pre - post, 123456789);
        if (hash1 != hash2) {
            System.out.println(s);
            // second time for debugging...
            hash2 = Murmur3.murmur3A(s, pre, s.length() - pre - post, 123456789);
        }
        assertEquals(hash1, hash2);
    }

    @Test
    public void testStringHash() {
        doString("hello!");
        doString("ABCD");
        doString("\u0123");
        doString("\u2345");
        doString("\u2345\u1234");

        Random r = new Random();
        StringBuilder sb = new StringBuilder(40);
        for (int i = 0; i < 100000; i++) {
            sb.setLength(0);
            int pre = r.nextInt(3);
            int post = r.nextInt(3);
            int len = r.nextInt(16);

            for (int j = 0; j < pre; j++) {
                int codePoint = r.nextInt(0x80);
                sb.appendCodePoint(codePoint);
            }

            for (int j = 0; j < len; j++) {
                int codePoint;
                do {
                    int max = switch (r.nextInt() & 0x3) {
                        case 0 -> 0x80;   // 1 UTF8 bytes
                        case 1 -> 0x800;  // up to 2 bytes
                        case 2 -> 0xffff + 1; // up to 3 bytes
                        case 3 -> Character.MAX_CODE_POINT + 1;
                        default -> 0; // up to 4 bytes
                    };

                    codePoint = r.nextInt(max);
                } while (codePoint < 0xffff && (Character.isHighSurrogate((char) codePoint) || Character.isLowSurrogate((char) codePoint)));

                sb.appendCodePoint(codePoint);
            }

            for (int j = 0; j < post; j++) {
                int codePoint = r.nextInt(0x80);
                sb.appendCodePoint(codePoint);
            }

            String s = sb.toString();
            String middle = s.substring(pre, s.length() - post);

            doString(s);
            doString(middle);
            doString(s, pre, post);
        }
    }
}
