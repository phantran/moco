/*
 * Copyright (c) 2021. Tran Phan
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

package io.moco.dev;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TestTest1 {

    @Test
    public void test_hihi() {
        DummyForTesting t = new DummyForTesting();
        int x = t.dummy();
        assertEquals(x, 8);

    }

    @Test
    public void test_hihi1() {
        int a = 1;
        int b = 2;
        assertEquals(b, a + 1);
    }

    @Test
    public void test_hihi3() {
        int a = 1;
        int b = 2;
        assertEquals(b, a + 1);
    }
}
